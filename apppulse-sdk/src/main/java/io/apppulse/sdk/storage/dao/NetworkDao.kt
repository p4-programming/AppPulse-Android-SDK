package io.apppulse.sdk.storage.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import io.apppulse.sdk.storage.entity.NetworkEntity

/**
 * Room access to HTTP call measurements.
 */
@Dao
interface NetworkDao {

    /**
     * Inserts [metric] and returns the generated row id.
     */
    @Insert
    suspend fun insert(metric: NetworkEntity): Long

    /**
     * Returns recent calls, newest first.
     */
    @Query("SELECT * FROM network_metrics ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getAll(limit: Int): List<NetworkEntity>

    /**
     * Returns the slowest stored call, or null when none exist.
     */
    @Query("SELECT * FROM network_metrics ORDER BY durationMs DESC LIMIT 1")
    suspend fun slowestRequest(): NetworkEntity?

    /**
     * Returns recorded URLs that use cleartext HTTP, newest first.
     */
    @Query(
        "SELECT url FROM network_metrics WHERE LOWER(url) LIKE 'http://%' " +
            "ORDER BY timestamp DESC LIMIT :limit"
    )
    suspend fun httpUrls(limit: Int): List<String>

    /**
     * Returns how many calls were slower than 3000 ms.
     */
    @Query("SELECT COUNT(*) FROM network_metrics WHERE slowApi = 1")
    suspend fun slowCount(): Int

    /**
     * Returns calls that have not been uploaded, oldest first.
     */
    @Query("SELECT * FROM network_metrics WHERE isSynced = 0 ORDER BY timestamp ASC LIMIT :limit")
    suspend fun unsynced(limit: Int): List<NetworkEntity>

    /**
     * Marks the rows in [ids] as uploaded.
     */
    @Query("UPDATE network_metrics SET isSynced = 1 WHERE id IN (:ids)")
    suspend fun markSynced(ids: List<Long>)
}
