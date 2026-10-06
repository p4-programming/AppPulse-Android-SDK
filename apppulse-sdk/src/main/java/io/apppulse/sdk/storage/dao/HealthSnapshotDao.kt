package io.apppulse.sdk.storage.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import io.apppulse.sdk.storage.entity.HealthSnapshotEntity

/**
 * Room access to generated health reports.
 */
@Dao
interface HealthSnapshotDao {

    /**
     * Inserts [snapshot] and returns the generated row id.
     */
    @Insert
    suspend fun insert(snapshot: HealthSnapshotEntity): Long

    /**
     * Returns the newest snapshot, or null when none exist.
     */
    @Query("SELECT * FROM health_snapshots ORDER BY generatedAt DESC, id DESC LIMIT 1")
    suspend fun latest(): HealthSnapshotEntity?

    /**
     * Returns recent snapshots, newest first.
     */
    @Query("SELECT * FROM health_snapshots ORDER BY generatedAt DESC, id DESC LIMIT :limit")
    suspend fun recent(limit: Int): List<HealthSnapshotEntity>

    /**
     * Returns reports that have not been uploaded, oldest first.
     */
    @Query("SELECT * FROM health_snapshots WHERE isSynced = 0 ORDER BY generatedAt ASC LIMIT :limit")
    suspend fun unsynced(limit: Int): List<HealthSnapshotEntity>

    /**
     * Marks the rows in [ids] as uploaded.
     */
    @Query("UPDATE health_snapshots SET isSynced = 1 WHERE id IN (:ids)")
    suspend fun markSynced(ids: List<Long>)
}
