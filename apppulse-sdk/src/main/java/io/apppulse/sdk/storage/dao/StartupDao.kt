package io.apppulse.sdk.storage.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import io.apppulse.sdk.storage.entity.StartupEntity

/**
 * Room access to cold-start measurements.
 */
@Dao
interface StartupDao {

    /**
     * Inserts [metric] and returns the generated row id.
     */
    @Insert
    suspend fun insert(metric: StartupEntity): Long

    /**
     * Returns stored cold starts, newest first.
     */
    @Query("SELECT * FROM startup_metrics ORDER BY firstActivityTimestamp DESC LIMIT :limit")
    suspend fun getAll(limit: Int): List<StartupEntity>

    /**
     * Returns the mean cold-start duration, or null when the table is empty.
     */
    @Query("SELECT AVG(startupTimeMs) FROM startup_metrics")
    suspend fun averageStartupTime(): Double?

    /**
     * Returns cold starts that have not been uploaded, oldest first.
     */
    @Query("SELECT * FROM startup_metrics WHERE isSynced = 0 ORDER BY launchTimestamp ASC LIMIT :limit")
    suspend fun unsynced(limit: Int): List<StartupEntity>

    /**
     * Marks the rows in [ids] as uploaded.
     */
    @Query("UPDATE startup_metrics SET isSynced = 1 WHERE id IN (:ids)")
    suspend fun markSynced(ids: List<Long>)
}
