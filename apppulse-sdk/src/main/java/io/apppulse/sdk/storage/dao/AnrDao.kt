package io.apppulse.sdk.storage.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import io.apppulse.sdk.storage.entity.AnrEntity

/**
 * Room access to detected main-thread stalls.
 */
@Dao
interface AnrDao {

    /**
     * Inserts [metric] and returns the generated row id.
     */
    @Insert
    suspend fun insert(metric: AnrEntity): Long

    /**
     * Returns stored stalls, newest first.
     */
    @Query("SELECT * FROM anr_metrics ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getAll(limit: Int): List<AnrEntity>

    /**
     * Returns the number of stored stalls.
     */
    @Query("SELECT COUNT(*) FROM anr_metrics")
    suspend fun count(): Int

    /**
     * Returns stalls that have not been uploaded, oldest first.
     */
    @Query("SELECT * FROM anr_metrics WHERE isSynced = 0 ORDER BY timestamp ASC LIMIT :limit")
    suspend fun unsynced(limit: Int): List<AnrEntity>

    /**
     * Marks the rows in [ids] as uploaded.
     */
    @Query("UPDATE anr_metrics SET isSynced = 1 WHERE id IN (:ids)")
    suspend fun markSynced(ids: List<Long>)
}
