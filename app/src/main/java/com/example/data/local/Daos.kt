package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Item
import com.example.data.model.SyncQueueItem
import com.example.data.model.TransactionRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface ItemDao {
    @Query("SELECT * FROM items ORDER BY o ASC")
    fun getAllItems(): Flow<List<Item>>

    @Query("SELECT * FROM items ORDER BY o ASC")
    suspend fun getAllItemsList(): List<Item>

    @Query("SELECT * FROM items WHERE id = :id LIMIT 1")
    fun getItemById(id: String): Flow<Item?>

    @Query("SELECT * FROM items WHERE id = :id LIMIT 1")
    suspend fun getItemByIdOnce(id: String): Item?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: Item)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<Item>)

    @Update
    suspend fun update(item: Item)

    @Query("DELETE FROM items WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM items WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<String>)

    @Query("DELETE FROM items")
    suspend fun deleteAll()

    @Query("SELECT MAX(o) FROM items")
    suspend fun getMaxOrder(): Int?
}

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY created_at DESC")
    fun getAllTransactions(): Flow<List<TransactionRecord>>

    @Query("SELECT * FROM transactions ORDER BY created_at DESC LIMIT :limit")
    fun getRecentTransactions(limit: Int = 5): Flow<List<TransactionRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: TransactionRecord)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(records: List<TransactionRecord>)

    @Query("DELETE FROM transactions")
    suspend fun deleteAll()
}

@Dao
interface SyncQueueDao {
    @Query("SELECT * FROM sync_queue ORDER BY timestamp ASC")
    suspend fun getAll(): List<SyncQueueItem>

    @Query("SELECT * FROM sync_queue ORDER BY timestamp ASC")
    fun getFlow(): Flow<List<SyncQueueItem>>

    @Insert
    suspend fun insert(item: SyncQueueItem): Long

    @Query("DELETE FROM sync_queue WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM sync_queue")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM sync_queue")
    fun getCountFlow(): Flow<Int>
}
