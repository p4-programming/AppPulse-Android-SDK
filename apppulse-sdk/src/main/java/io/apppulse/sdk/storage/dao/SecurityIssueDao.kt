package io.apppulse.sdk.storage.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import io.apppulse.sdk.storage.entity.SecurityIssueEntity

/**
 * Room access to security findings.
 */
@Dao
interface SecurityIssueDao {

    /**
     * Inserts every row in [issues].
     */
    @Insert
    suspend fun insertAll(issues: List<SecurityIssueEntity>)

    /**
     * Removes every stored finding.
     */
    @Query("DELETE FROM security_issues")
    suspend fun deleteAll()

    /**
     * Removes findings in [category].
     */
    @Query("DELETE FROM security_issues WHERE category = :category")
    suspend fun deleteByCategory(category: String)

    /**
     * Returns stored findings, critical first.
     */
    @Query(
        "SELECT * FROM security_issues ORDER BY " +
            "CASE severity " +
            "WHEN 'CRITICAL' THEN 0 " +
            "WHEN 'HIGH' THEN 1 " +
            "WHEN 'MEDIUM' THEN 2 " +
            "ELSE 3 END, target ASC"
    )
    suspend fun getAll(): List<SecurityIssueEntity>

    /**
     * Returns findings that have not been uploaded.
     */
    @Query(
        "SELECT * FROM security_issues WHERE isSynced = 0 ORDER BY timestamp ASC LIMIT :limit"
    )
    suspend fun unsynced(limit: Int): List<SecurityIssueEntity>

    /**
     * Marks the rows in [ids] as uploaded.
     */
    @Query("UPDATE security_issues SET isSynced = 1 WHERE id IN (:ids)")
    suspend fun markSynced(ids: List<Long>)
}
