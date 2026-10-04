package com.example.data.remote

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.content.FileProvider
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

data class ReleaseInfo(
    val tagName: String,
    val title: String,
    val releaseNotes: String,
    val apkDownloadUrl: String,
    val apkSizeBytes: Long,
    val publishedAt: String,
    val isNewerThanCurrent: Boolean
)

sealed class UpdateStatus {
    object Idle : UpdateStatus()
    object Checking : UpdateStatus()
    data class Available(val info: ReleaseInfo) : UpdateStatus()
    object UpToDate : UpdateStatus()
    data class Downloading(val progress: Float, val downloadedBytes: Long, val totalBytes: Long) : UpdateStatus()
    data class ReadyToInstall(val apkFile: File) : UpdateStatus()
    data class Error(val message: String) : UpdateStatus()
}

class AppUpdateManager(private val context: Context) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val _status = MutableStateFlow<UpdateStatus>(UpdateStatus.Idle)
    val status: StateFlow<UpdateStatus> = _status.asStateFlow()

    companion object {
        private const val TAG = "AppUpdateManager"
        private const val GITHUB_RELEASE_API = "https://api.github.com/repos/ARIF683/Hardware/releases/latest"
        const val CURRENT_VERSION_TAG = "v1.0.0"
    }

    suspend fun checkForUpdates(): ReleaseInfo? = withContext(Dispatchers.IO) {
        _status.value = UpdateStatus.Checking
        try {
            val request = Request.Builder()
                .url(GITHUB_RELEASE_API)
                .header("Accept", "application/vnd.github.v3+json")
                .header("User-Agent", "Hardware-StockManager-App")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val code = response.code
                    _status.value = UpdateStatus.Error("GitHub API returned code $code")
                    return@withContext null
                }

                val body = response.body?.string() ?: return@withContext null
                val json = JSONObject(body)

                val tagName = json.optString("tag_name", "")
                val name = json.optString("name", "New Update")
                val notes = json.optString("body", "Bug fixes and performance improvements.")
                val publishedAt = json.optString("published_at", "")

                // Find APK asset
                var downloadUrl = ""
                var apkSize = 0L
                val assets = json.optJSONArray("assets")
                if (assets != null) {
                    for (i in 0 until assets.length()) {
                        val asset = assets.getJSONObject(i)
                        val assetName = asset.optString("name", "")
                        if (assetName.endsWith(".apk", ignoreCase = true)) {
                            downloadUrl = asset.optString("browser_download_url", "")
                            apkSize = asset.optLong("size", 0L)
                            break
                        }
                    }
                }

                if (downloadUrl.isNotEmpty()) {
                    // Check if newer: if tag is "latest" or different from current version tag
                    val isNewer = tagName.isNotEmpty() && tagName != CURRENT_VERSION_TAG
                    val info = ReleaseInfo(
                        tagName = tagName,
                        title = name,
                        releaseNotes = notes,
                        apkDownloadUrl = downloadUrl,
                        apkSizeBytes = apkSize,
                        publishedAt = publishedAt,
                        isNewerThanCurrent = isNewer
                    )

                    if (isNewer) {
                        _status.value = UpdateStatus.Available(info)
                    } else {
                        _status.value = UpdateStatus.UpToDate
                    }
                    return@withContext info
                } else {
                    _status.value = UpdateStatus.UpToDate
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking for updates", e)
            _status.value = UpdateStatus.Error(e.message ?: "Failed to check for updates")
        }
        null
    }

    suspend fun downloadAndInstall(downloadUrl: String) = withContext(Dispatchers.IO) {
        try {
            val apkFile = File(context.cacheDir, "StockManager_update.apk")
            if (apkFile.exists()) {
                apkFile.delete()
            }

            val request = Request.Builder()
                .url(downloadUrl)
                .header("User-Agent", "Hardware-StockManager-App")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    _status.value = UpdateStatus.Error("Failed to download APK: HTTP ${response.code}")
                    return@withContext
                }

                val body = response.body ?: throw Exception("Empty download response")
                val totalBytes = body.contentLength()
                var downloadedBytes = 0L

                val input = body.byteStream()
                val output = FileOutputStream(apkFile)

                val buffer = ByteArray(8192)
                var bytesRead: Int

                while (input.read(buffer).also { bytesRead = it } != -1) {
                    output.write(buffer, 0, bytesRead)
                    downloadedBytes += bytesRead

                    val progress = if (totalBytes > 0) downloadedBytes.toFloat() / totalBytes else 0.5f
                    _status.value = UpdateStatus.Downloading(progress, downloadedBytes, totalBytes)
                }

                output.flush()
                output.close()
                input.close()

                _status.value = UpdateStatus.ReadyToInstall(apkFile)
                installApk(apkFile)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Download failed", e)
            _status.value = UpdateStatus.Error("Download failed: ${e.message}")
        }
    }

    fun installApk(apkFile: File) {
        try {
            val authority = "${context.packageName}.provider"
            val uri: Uri = FileProvider.getUriForFile(context, authority, apkFile)

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Install intent failed", e)
            _status.value = UpdateStatus.Error("Cannot launch installer: ${e.message}")
        }
    }
}
