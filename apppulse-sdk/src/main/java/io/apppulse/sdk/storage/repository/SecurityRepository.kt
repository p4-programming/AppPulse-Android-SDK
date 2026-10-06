package io.apppulse.sdk.storage.repository

import io.apppulse.sdk.security.model.SecurityIssue
import io.apppulse.sdk.security.model.SecuritySeverity
import io.apppulse.sdk.storage.dao.SecurityIssueDao
import io.apppulse.sdk.storage.entity.SecurityIssueEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Reads and writes security findings.
 *
 * Database work runs on [Dispatchers.IO].
 */
class SecurityRepository(
    private val securityIssueDao: SecurityIssueDao
) {

    /**
     * Replaces every stored finding with [issues].
     */
    suspend fun replaceIssues(issues: List<SecurityIssue>) {
        withContext(Dispatchers.IO) {
            securityIssueDao.deleteAll()
            if (issues.isNotEmpty()) {
                securityIssueDao.insertAll(issues.map { it.toEntity() })
            }
        }
    }

    /**
     * Replaces findings in [category] and leaves the other categories in place.
     */
    suspend fun replaceCategory(category: String, issues: List<SecurityIssue>) {
        withContext(Dispatchers.IO) {
            securityIssueDao.deleteByCategory(category)
            if (issues.isNotEmpty()) {
                securityIssueDao.insertAll(issues.map { it.toEntity() })
            }
        }
    }

    /**
     * Returns stored findings, critical first.
     */
    suspend fun getIssues(): List<SecurityIssue> {
        return withContext(Dispatchers.IO) {
            securityIssueDao.getAll().map { it.toIssue() }
        }
    }

    private fun SecurityIssue.toEntity(): SecurityIssueEntity {
        return SecurityIssueEntity(
            id = 0,
            timestamp = timestamp,
            category = category,
            code = code,
            title = title,
            description = description,
            severity = severity.name,
            target = target
        )
    }

    private fun SecurityIssueEntity.toIssue(): SecurityIssue {
        return SecurityIssue(
            id = id,
            category = category,
            code = code,
            title = title,
            description = description,
            severity = SecuritySeverity.valueOf(severity),
            target = target,
            timestamp = timestamp
        )
    }
}
