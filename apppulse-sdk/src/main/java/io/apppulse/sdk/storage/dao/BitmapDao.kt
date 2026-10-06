package io.apppulse.sdk.storage.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import io.apppulse.sdk.storage.entity.BitmapEntity

/**
 * Room access to bitmap measurements.
 */
@Dao
interface BitmapDao {

    /**
     * Inserts [metric] and returns the generated row id.
     */
    @Insert
    suspend fun insert(metric: BitmapEntity): Long

    /**
     * Returns recent bitmap rows, newest first.
     */
    @Query("SELECT * FROM bitmap_metrics ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getAll(limit: Int): List<BitmapEntity>

    /**
     * Returns the large-bitmap count from the newest sample, or null when none exist.
     */
    @Query("SELECT largeCount FROM bitmap_metrics ORDER BY timestamp DESC LIMIT 1")
    suspend fun latestLargeCount(): Int?

    /**
     * Returns samples that have not been uploaded, oldest first.
     */
    @Query("SELECT * FROM bitmap_metrics WHERE isSynced = 0 ORDER BY timestamp ASC LIMIT :limit")
    suspend fun unsynced(limit: Int): List<BitmapEntity>

    /**
     * Marks the rows in [ids] as uploaded.
     */
    @Query("UPDATE bitmap_metrics SET isSynced = 1 WHERE id IN (:ids)")
    suspend fun markSynced(ids: List<Long>)
}
