package io.apppulse.sdk.storage.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import io.apppulse.sdk.storage.entity.LeakEntity

/**
 * Room access to leak candidates.
 */
@Dao
interface LeakDao {

    /**
     * Inserts [leak] and returns the generated row id.
     */
    @Insert
    suspend fun insert(leak: LeakEntity): Long

    /**
     * Returns stored candidates, newest detection first.
     */
    @Query("SELECT * FROM leak_candidates ORDER BY detectedTimestamp DESC LIMIT :limit")
    suspend fun getAll(limit: Int): List<LeakEntity>

    /**
     * Returns how many candidates are stored.
     */
    @Query("SELECT COUNT(*) FROM leak_candidates")
    suspend fun count(): Int

    /**
     * Returns candidates that have not been uploaded, oldest first.
     */
    @Query("SELECT * FROM leak_candidates WHERE isSynced = 0 ORDER BY detectedTimestamp ASC LIMIT :limit")
    suspend fun unsynced(limit: Int): List<LeakEntity>

    /**
     * Marks the rows in [ids] as uploaded.
     */
    @Query("UPDATE leak_candidates SET isSynced = 1 WHERE id IN (:ids)")
    suspend fun markSynced(ids: List<Long>)
}
