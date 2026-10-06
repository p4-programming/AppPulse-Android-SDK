package io.apppulse.sdk.storage.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import io.apppulse.sdk.storage.entity.MemoryEntity

/**
 * Room access to heap samples.
 */
@Dao
interface MemoryDao {

    /**
     * Inserts [metric] and returns the generated row id.
     */
    @Insert
    suspend fun insert(metric: MemoryEntity): Long

    /**
     * Returns recent samples, newest first.
     */
    @Query("SELECT * FROM memory_metrics ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getAll(limit: Int): List<MemoryEntity>

    /**
     * Returns the largest used-heap sample, or null when the table is empty.
     */
    @Query("SELECT MAX(usedMemoryMb) FROM memory_metrics")
    suspend fun peakUsedMemoryMb(): Double?

    /**
     * Returns the mean used heap, or null when the table is empty.
     */
    @Query("SELECT AVG(usedMemoryMb) FROM memory_metrics")
    suspend fun averageUsedMemoryMb(): Double?

    /**
     * Returns the max-heap value from the newest sample, or null when none exist.
     */
    @Query("SELECT maxMemoryMb FROM memory_metrics ORDER BY timestamp DESC LIMIT 1")
    suspend fun latestMaxMemoryMb(): Double?

    /**
     * Returns samples that have not been uploaded, oldest first.
     */
    @Query("SELECT * FROM memory_metrics WHERE isSynced = 0 ORDER BY timestamp ASC LIMIT :limit")
    suspend fun unsynced(limit: Int): List<MemoryEntity>

    /**
     * Marks the rows in [ids] as uploaded.
     */
    @Query("UPDATE memory_metrics SET isSynced = 1 WHERE id IN (:ids)")
    suspend fun markSynced(ids: List<Long>)
}
