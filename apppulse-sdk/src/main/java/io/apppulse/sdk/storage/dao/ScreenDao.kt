package io.apppulse.sdk.storage.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import io.apppulse.sdk.storage.entity.ScreenEntity

/**
 * Room access to screen render measurements.
 */
@Dao
interface ScreenDao {

    /**
     * Inserts [metric] and returns the generated row id.
     */
    @Insert
    suspend fun insert(metric: ScreenEntity): Long

    /**
     * Returns screens flagged as slow, slowest first.
     */
    @Query(
        "SELECT * FROM screen_metrics WHERE slowScreen = 1 " +
            "ORDER BY renderTimeMs DESC LIMIT :limit"
    )
    suspend fun getSlowScreens(limit: Int): List<ScreenEntity>

    /**
     * Returns the screen with the largest render time, or null when none exist.
     */
    @Query("SELECT * FROM screen_metrics ORDER BY renderTimeMs DESC LIMIT 1")
    suspend fun slowestScreen(): ScreenEntity?

    /**
     * Returns how many screens were slower than 1000 ms.
     */
    @Query("SELECT COUNT(*) FROM screen_metrics WHERE slowScreen = 1")
    suspend fun slowCount(): Int

    /**
     * Returns screen renders that have not been uploaded, oldest first.
     */
    @Query("SELECT * FROM screen_metrics WHERE isSynced = 0 ORDER BY openTimestamp ASC LIMIT :limit")
    suspend fun unsynced(limit: Int): List<ScreenEntity>

    /**
     * Marks the rows in [ids] as uploaded.
     */
    @Query("UPDATE screen_metrics SET isSynced = 1 WHERE id IN (:ids)")
    suspend fun markSynced(ids: List<Long>)
}
