package io.apppulse.sdk.storage.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import io.apppulse.sdk.storage.entity.FpsEntity

/**
 * Room access to frame-rate samples.
 */
@Dao
interface FpsDao {

    /**
     * Inserts [metric] and returns the generated row id.
     */
    @Insert
    suspend fun insert(metric: FpsEntity): Long

    /**
     * Returns recent samples, newest first.
     */
    @Query("SELECT * FROM fps_metrics ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getAll(limit: Int): List<FpsEntity>

    /**
     * Returns the mean sampled FPS, or null when the table is empty.
     */
    @Query("SELECT AVG(fps) FROM fps_metrics")
    suspend fun averageFps(): Double?

    /**
     * Returns samples that have not been uploaded, oldest first.
     */
    @Query("SELECT * FROM fps_metrics WHERE isSynced = 0 ORDER BY timestamp ASC LIMIT :limit")
    suspend fun unsynced(limit: Int): List<FpsEntity>

    /**
     * Marks the rows in [ids] as uploaded.
     */
    @Query("UPDATE fps_metrics SET isSynced = 1 WHERE id IN (:ids)")
    suspend fun markSynced(ids: List<Long>)
}
