package io.apppulse.sdk.storage.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import io.apppulse.sdk.crash.CrashEntity

/**
 * Room access to stored crash reports.
 *
 * Every call suspends and runs on Room's query executor. Callers must not
 * invoke these functions on the main thread without a coroutine.
 */
@Dao
interface CrashDao {

    /**
     * Inserts [crash] and returns the generated row id.
     *
     * @param crash Report to store. [CrashEntity.id] should be `0`.
     */
    @Insert
    suspend fun insertCrash(crash: CrashEntity): Long

    /**
     * Returns every stored crash, newest first.
     */
    @Query("SELECT * FROM crashes ORDER BY timestamp DESC, id DESC")
    suspend fun getAllCrashes(): List<CrashEntity>

    /**
     * Deletes the row identified by [crash].
     *
     * @param crash Report to remove. Matching uses the primary key.
     */
    @Delete
    suspend fun deleteCrash(crash: CrashEntity)

    /**
     * Deletes every stored crash.
     */
    @Query("DELETE FROM crashes")
    suspend fun deleteAllCrashes()

    /**
     * Returns the crash with [id], or null when that row is gone.
     *
     * @param id Primary key assigned at insert time.
     */
    @Query("SELECT * FROM crashes WHERE id = :id LIMIT 1")
    suspend fun getCrashById(id: Long): CrashEntity?

    /**
     * Returns how many crashes are stored.
     */
    @Query("SELECT COUNT(*) FROM crashes")
    suspend fun count(): Int

    /**
     * Returns how many crashes were stored at or after [since].
     */
    @Query("SELECT COUNT(*) FROM crashes WHERE timestamp >= :since")
    suspend fun countSince(since: Long): Int

    /**
     * Returns crashes that have not been uploaded, oldest first.
     */
    @Query("SELECT * FROM crashes WHERE isSynced = 0 ORDER BY timestamp ASC LIMIT :limit")
    suspend fun unsynced(limit: Int): List<CrashEntity>

    /**
     * Marks the rows in [ids] as uploaded.
     */
    @Query("UPDATE crashes SET isSynced = 1 WHERE id IN (:ids)")
    suspend fun markSynced(ids: List<Long>)
}
