package com.example.util

import com.example.data.model.Item
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

data class ParsedTintRow(
    val tintRecordId: String,
    val dealerCode: String,
    val dealerName: String,
    val machineType: String,
    val shadeCode: String,
    val productName: String,
    val shadeName: String,
    val baseCode: String,
    val canFactor: String,
    val litersPerCan: Double,
    val noOfCans: Int,
    val totalLiters: Double,
    val tintDateRaw: String,
    val tintTimeRaw: String,
    val tintTimestampIso: String,
    val tintDisplayDateTime: String,
    val colorantUsed: String,
    val colorantQuantity: String,
    var matchedItem: Item? = null,
    var isAlreadyProcessed: Boolean = false,
    var deductQty: Double = 1.0,
    var deductUnit: String = "can"
)

object ColorMachineParser {

    /**
     * Parses the CSV / CSC text exported from color dispensing / tinting machines (e.g., Corob / Smart Tint).
     */
    fun parse(
        content: String,
        items: List<Item>,
        processedRecordIds: Set<String>
    ): List<ParsedTintRow> {
        val rows = SpreadsheetReportParser.parseCsvOrDelimited(content)
        return parseRows(rows, items, processedRecordIds)
    }

    /**
     * Parses from any InputStream (XLS, XLSX, CSV, CSC, TXT).
     * Returns Pair(parsedRows, csvRepresentation).
     */
    fun parseFromStream(
        inputStream: java.io.InputStream,
        items: List<Item>,
        processedRecordIds: Set<String>
    ): Pair<List<ParsedTintRow>, String> {
        val report = SpreadsheetReportParser.parse(inputStream)
        val rows = parseRows(report.rows, items, processedRecordIds)
        return Pair(rows, report.csvText)
    }

    /**
     * Parses normalized 2D table rows from any spreadsheet/CSV file into ParsedTintRow objects.
     */
    fun parseRows(
        rows: List<List<String>>,
        items: List<Item>,
        processedRecordIds: Set<String>
    ): List<ParsedTintRow> {
        if (rows.size <= 1) return emptyList()

        // Find header line
        var headerIndex = -1
        var headers: List<String> = emptyList()

        for (i in rows.indices) {
            val cols = rows[i].map { it.trim().uppercase() }
            if (cols.any { it.contains("PRODUCT") || it.contains("BASE") || it.contains("DATE") || it.contains("FACTOR") }) {
                headerIndex = i
                headers = cols
                break
            }
        }

        if (headerIndex == -1 || headers.isEmpty()) {
            return emptyList()
        }

        fun findCol(vararg keywords: String): Int {
            return headers.indexOfFirst { h -> keywords.any { kw -> h == kw || h.contains(kw) } }
        }

        // Map column indices
        val colProduct = findCol("PRODUCT_NAME", "PRODUCT", "PROD_NAME", "ITEM_NAME", "DESCRIPTION")
        val colCanFactor = findCol("CAN_FACTOR", "CAN_SIZE", "PACK_SIZE", "CANFACTOR", "SIZE", "VOLUME", "LTR")
        // Strictly find CAN COUNT column - NEVER match generic QTY or QUANTITY which represents colorant ml in paint machines!
        val colNoOfCan = headers.indexOfFirst { h ->
            if (h.contains("COLORANT") || h.contains("SHOT") || h.contains("DISPENSE") || h.contains("FORMULA") || h.contains("ML") || h.contains("TINT_QTY")) {
                false
            } else {
                val canKeywords = listOf(
                    "NO_OF_CAN", "NO OF CAN", "NO_OF_CANS", "NO OF CANS", "NO_CAN", "NO_CANS",
                    "CAN_COUNT", "NUM_CANS", "CAN_QTY", "CANS_COUNT", "CANS"
                )
                canKeywords.any { kw -> h == kw || h.contains(kw) }
            }
        }
        val colTintDate = findCol("TINT_DATE", "DISPENSE_DATE", "DATE")
        val colTintTime = findCol("TINT_TIME", "DISPENSE_TIME", "TIME")
        val colBaseCode = findCol("BASE_CODE", "BASE_NAME", "BASE", "BASENAME", "BASE_NO")
        val colShadeCode = findCol("SHADE_CODE", "SHADE")
        val colShadeName = findCol("SHADE_NAME")
        val colColorantUsed = findCol("COLORANT_USED", "COLORANT")
        val colColorantQty = headers.indexOfFirst { h ->
            h.contains("COLORANT_QTY") || h.contains("COLORANT_QUANTITY") || h.contains("DISPENSED") || h.contains("SHOT") || h == "QTY" || h == "QUANTITY"
        }
        val colDealerCode = findCol("DEALER_CODE", "DEALER")
        val colDealerName = findCol("DEALER_NAME")
        val colMachineType = findCol("MACHINE_TYPE", "MACHINE")

        val result = mutableListOf<ParsedTintRow>()

        for (i in (headerIndex + 1) until rows.size) {
            val cols = rows[i].map { it.trim() }
            if (cols.size <= 2 || cols.all { it.isBlank() }) continue

            val productName = (if (colProduct >= 0) cols.getOrNull(colProduct) else "")?.trim() ?: ""
            val baseCode = (if (colBaseCode >= 0) cols.getOrNull(colBaseCode) else "")?.trim() ?: ""
            if (productName.isBlank() && baseCode.isBlank()) continue

            val canFactor = (if (colCanFactor >= 0) cols.getOrNull(colCanFactor) else "1 LIT")?.trim() ?: "1 LIT"
            val noOfCansRaw = if (colNoOfCan >= 0) cols.getOrNull(colNoOfCan)?.trim() else null
            val noOfCans = if (!noOfCansRaw.isNullOrBlank()) {
                noOfCansRaw.toIntOrNull()?.coerceAtLeast(1) ?: 1
            } else {
                1
            }
            val tintDate = (if (colTintDate >= 0) cols.getOrNull(colTintDate) else "")?.trim() ?: ""
            val tintTime = (if (colTintTime >= 0) cols.getOrNull(colTintTime) else "")?.trim() ?: ""
            val shadeCode = (if (colShadeCode >= 0) cols.getOrNull(colShadeCode) else "")?.trim() ?: ""
            val shadeName = (if (colShadeName >= 0) cols.getOrNull(colShadeName) else "")?.trim() ?: ""
            val colorantUsed = (if (colColorantUsed >= 0) cols.getOrNull(colColorantUsed) else "")?.trim() ?: ""
            val colorantQty = (if (colColorantQty >= 0) cols.getOrNull(colColorantQty) else "")?.trim() ?: ""
            val dealerCode = (if (colDealerCode >= 0) cols.getOrNull(colDealerCode) else "")?.trim() ?: ""
            val dealerName = (if (colDealerName >= 0) cols.getOrNull(colDealerName) else "")?.trim() ?: ""
            val machineType = (if (colMachineType >= 0) cols.getOrNull(colMachineType) else "")?.trim() ?: ""

            val litersPerCan = parseCanFactorToLiters(canFactor)
            val totalLiters = litersPerCan * noOfCans

            val (isoTimestamp, displayDateTime) = parseTintDateTime(tintDate, tintTime)
            // Id strictly depends on Date, Time, Base, Product, CanFactor, NoOfCan (NO shade code!)
            val tintRecordId = generateTintRecordId(
                date = tintDate,
                time = tintTime,
                product = productName,
                base = baseCode,
                canFactor = canFactor,
                noOfCan = noOfCans
            )

            val isProcessed = processedRecordIds.contains(tintRecordId)
            val matched = findBestMatchingItem(productName, baseCode, canFactor, items)

            // Determine deduction quantity & unit:
            // If 1 Ltr is used from AB11 1ltr item -> deduct 1 pcs.
            // If 20 Ltr is used from AB11 20Ltr item -> deduct 1 pcs (or how many cans are specified in data).
            val (deductQty, deductUnit) = if (matched != null) {
                val unitClean = matched.unit.trim().lowercase()
                val isPureBulkLiters = unitClean in listOf("ltr", "l", "liter", "liters", "litre", "litres") &&
                    matched.size.isBlank() &&
                    !Regex("\\b(1|4|10|20)\\s*(ltr|l|lit|liter|litres)\\b", RegexOption.IGNORE_CASE).containsMatchIn(matched.name)

                if (isPureBulkLiters) {
                    Pair(totalLiters, "Ltr")
                } else {
                    Pair(noOfCans.toDouble(), if (matched.unit.isNotBlank()) matched.unit else "pcs")
                }
            } else {
                Pair(noOfCans.toDouble(), if (noOfCans == 1) "can" else "cans")
            }

            result.add(
                ParsedTintRow(
                    tintRecordId = tintRecordId,
                    dealerCode = dealerCode,
                    dealerName = dealerName,
                    machineType = machineType,
                    shadeCode = shadeCode,
                    productName = productName,
                    shadeName = shadeName,
                    baseCode = baseCode,
                    canFactor = canFactor,
                    litersPerCan = litersPerCan,
                    noOfCans = noOfCans,
                    totalLiters = totalLiters,
                    tintDateRaw = tintDate,
                    tintTimeRaw = tintTime,
                    tintTimestampIso = isoTimestamp,
                    tintDisplayDateTime = displayDateTime,
                    colorantUsed = colorantUsed,
                    colorantQuantity = colorantQty,
                    matchedItem = matched,
                    isAlreadyProcessed = isProcessed,
                    deductQty = deductQty,
                    deductUnit = deductUnit
                )
            )
        }

        return result
    }

    /**
     * Splits a CSV line respecting quoted commas.
     */
    private fun splitCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false

        for (i in line.indices) {
            val c = line[i]
            when {
                c == '\"' -> inQuotes = !inQuotes
                (c == ',' || (c == ';' && !inQuotes)) && !inQuotes -> {
                    result.add(current.toString().trim().removeSurrounding("\""))
                    current.clear()
                }
                else -> current.append(c)
            }
        }
        result.add(current.toString().trim().removeSurrounding("\""))
        return result
    }

    /**
     * Converts machine CAN_FACTOR string (e.g. "1 LIT", "4 LIT", "500 ML", "20 L") to Liters.
     */
    fun parseCanFactorToLiters(canFactor: String): Double {
        val clean = canFactor.trim().uppercase()
        val numStr = clean.replace(Regex("[^0-9.]"), "")
        val num = numStr.toDoubleOrNull() ?: 1.0

        return when {
            clean.contains("ML") -> num / 1000.0
            else -> num
        }
    }

    /**
     * Formats can factor to clean display string (e.g., "1 Ltr", "500 ml", "4 Ltr").
     */
    fun formatCanFactorDisplay(canFactor: String): String {
        val liters = parseCanFactorToLiters(canFactor)
        return if (liters < 1.0) {
            "${(liters * 1000).toInt()} ml"
        } else if (liters == liters.toInt().toDouble()) {
            "${liters.toInt()} Ltr"
        } else {
            "$liters Ltr"
        }
    }

    /**
     * Generates a deterministic, unique record ID fingerprint to guarantee idempotency.
     * Note: Only uses date, time, product, base, canFactor, noOfCan. Does NOT read shade code!
     */
    fun generateTintRecordId(
        date: String,
        time: String,
        product: String,
        base: String,
        canFactor: String,
        noOfCan: Int
    ): String {
        val raw = "${date.trim()}|${time.trim()}|${product.trim()}|${base.trim()}|${canFactor.trim()}|$noOfCan"
            .lowercase()
        return raw.replace(Regex("[^a-z0-9|_-]"), "_")
    }

    /**
     * Robust parser for machine date & time (e.g., "06-Oct-26" and "18:45:45").
     * Returns Pair(ISO-8601 string, Formatted Display string).
     */
    fun parseTintDateTime(dateStr: String, timeStr: String): Pair<String, String> {
        val now = Date()
        val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val displayFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US)

        if (dateStr.isBlank()) {
            return Pair(isoFormat.format(now), displayFormat.format(now))
        }

        val datePatterns = listOf(
            "dd-MMM-yy",
            "dd-MMM-yyyy",
            "dd/MM/yyyy",
            "dd-MM-yyyy",
            "yyyy-MM-dd",
            "dd.MM.yyyy",
            "dd-MM-yy",
            "dd/MM/yy"
        )

        val cleanTime = if (timeStr.isBlank()) "12:00:00" else timeStr.trim()
        val timePatterns = listOf("HH:mm:ss", "HH:mm", "hh:mm:ss a", "hh:mm a")

        var parsedDate: Date? = null

        for (dp in datePatterns) {
            for (tp in timePatterns) {
                try {
                    val sdf = SimpleDateFormat("$dp $tp", Locale.US)
                    sdf.isLenient = false
                    val d = sdf.parse("$dateStr $cleanTime")
                    if (d != null) {
                        parsedDate = d
                        break
                    }
                } catch (_: Exception) {}
            }
            if (parsedDate != null) break
        }

        // If time pattern failed, try date only
        if (parsedDate == null) {
            for (dp in datePatterns) {
                try {
                    val sdf = SimpleDateFormat(dp, Locale.US)
                    val d = sdf.parse(dateStr)
                    if (d != null) {
                        parsedDate = d
                        break
                    }
                } catch (_: Exception) {}
            }
        }

        val finalDate = parsedDate ?: now
        return Pair(isoFormat.format(finalDate), displayFormat.format(finalDate))
    }

    /**
     * Matches machine product & base & can factor (1, 4, 10, 20 Ltr) to best matching inventory item.
     * Explicitly prioritizes matching the EXACT CAN FACTOR / PACK SIZE.
     */
    fun findBestMatchingItem(
        productName: String,
        baseCode: String,
        canFactor: String,
        items: List<Item>
    ): Item? {
        if (items.isEmpty()) return null

        val pClean = productName.trim().uppercase()
        val bClean = baseCode.trim().uppercase()
        val targetLiters = parseCanFactorToLiters(canFactor)

        // Clean alphanumeric version of base: e.g. "AB11S" -> "AB11S"
        val cleanBase = bClean.replace(Regex("[^A-Z0-9]"), "")
        // Extract core base by stripping standard regional/machine tint suffix:
        // e.g. "AB11S" -> "AB11", "TE15S" -> "TE15", "W11S" -> "W11", "AB11-S" -> "AB11"
        val cleanBaseCore = cleanBase.replace(Regex("([0-9]+)[A-Z]+$"), "$1")

        // Product words/tokens: e.g. "APEX", "EMULSION", "TRACTOR", "ROYALE"
        val productTokens = pClean.split(Regex("[\\s-_/]+"))
            .filter { it.length > 1 && it !in listOf("WP", "EXT", "INT", "LIT", "LTR", "L", "ML") }

        var bestScore = 0
        var bestItem: Item? = null

        for (item in items) {
            var score = 0
            val itemNameUpper = item.name.uppercase().trim()
            val itemSizeUpper = item.size.uppercase().trim()
            val itemCodeUpper = item.code.uppercase().trim()
            val itemAliasesUpper = item.aliases.uppercase().trim()
            val itemTypeUpper = item.type.uppercase().trim()
            val itemBrandUpper = item.brand.uppercase().trim()

            val cleanItemName = itemNameUpper.replace(Regex("[^A-Z0-9]"), "")
            val cleanItemCode = itemCodeUpper.replace(Regex("[^A-Z0-9]"), "")
            val cleanItemAliases = itemAliasesUpper.replace(Regex("[^A-Z0-9]"), "")

            // 1. Pack size matching
            // First determine this item's pack size in liters (from item.size or item.name)
            val itemLiters = parseCanFactorToLiters(if (item.size.isNotBlank()) item.size else item.name)
            val isSizeMatching = Math.abs(itemLiters - targetLiters) < 0.05

            if (isSizeMatching) {
                score += 100 // Big bonus for matching exact pack size (1L, 4L, 10L, 20L)
            } else if (item.size.isNotBlank() || Regex("(?i)\\b\\d+\\s*(l|ltr|lit|liter)\\b").containsMatchIn(item.name)) {
                // Penalize mismatched sizes heavily so a 20L item never takes precedence over a 1L item
                score -= 120
            }

            // 2. Base code matching
            if (cleanBase.isNotBlank()) {
                if (cleanItemName == cleanBase || cleanItemCode == cleanBase) {
                    score += 120 // Exact base code match: e.g. AB11S == AB11S or TE15 == TE15
                } else if (cleanBaseCore.isNotBlank() && (cleanItemName == cleanBaseCore || cleanItemCode == cleanBaseCore)) {
                    score += 115 // Core base match: e.g. AB11S matches AB11, TE15S matches TE15
                } else if (cleanItemName.contains(cleanBase) || cleanItemCode.contains(cleanBase)) {
                    score += 90
                } else if (cleanBaseCore.isNotBlank() && (cleanItemName.contains(cleanBaseCore) || cleanItemCode.contains(cleanBaseCore))) {
                    score += 85
                } else if (cleanItemName.length >= 2 && cleanBase.startsWith(cleanItemName)) {
                    score += 80 // e.g. AB11S starts with AB11
                } else if (cleanItemAliases.contains(cleanBase) || (cleanBaseCore.isNotBlank() && cleanItemAliases.contains(cleanBaseCore))) {
                    score += 75
                }
            }

            // 3. Product Name / Type / Brand matching
            // Check if product tokens ("APEX", "EMULSION", "TRACTOR") match item name or type
            for (token in productTokens) {
                if (itemNameUpper.contains(token) || itemTypeUpper.contains(token)) {
                    score += 25
                } else if (itemAliasesUpper.contains(token)) {
                    score += 15
                }
            }

            // High-confidence line matching (e.g. Apex, Tractor, Royale)
            if (pClean.contains("APEX") && (itemTypeUpper.contains("APEX") || itemNameUpper.contains("APEX"))) {
                score += 35
            }
            if (pClean.contains("TRACTOR") && (itemTypeUpper.contains("TRACTOR") || itemNameUpper.contains("TRACTOR"))) {
                score += 35
            }
            if (pClean.contains("ROYALE") && (itemTypeUpper.contains("ROYALE") || itemNameUpper.contains("ROYALE"))) {
                score += 35
            }

            // Brand matching (e.g. Asian Paints)
            if (itemBrandUpper.isNotBlank() && (pClean.contains(itemBrandUpper) || pClean.contains("ASIAN"))) {
                score += 15
            }

            // Type category matching ("Paint" / "Emulsion")
            if (itemTypeUpper.contains("PAINT") || itemTypeUpper.contains("EMULSION")) {
                score += 10
            }

            if (score > bestScore && score >= 70) {
                bestScore = score
                bestItem = item
            }
        }

        return bestItem
    }
}
