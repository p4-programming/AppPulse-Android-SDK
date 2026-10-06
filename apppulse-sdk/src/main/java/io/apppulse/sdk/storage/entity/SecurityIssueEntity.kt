package io.apppulse.sdk.storage.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room row for one security finding.
 *
 * @property id Surrogate key assigned by Room.
 * @property timestamp Wall-clock time of the scan.
 * @property category Scan area, such as `MANIFEST` or `WEBVIEW`.
 * @property code Stable check identifier.
 * @property title Short name shown on the dashboard.
 * @property description Why the finding matters.
 * @property severity [io.apppulse.sdk.security.model.SecuritySeverity] name.
 * @property target Component, permission, or URL the finding refers to.
 */
@Entity(tableName = "security_issues")
data class SecurityIssueEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,
    @ColumnInfo(name = "timestamp")
    val timestamp: Long,
    @ColumnInfo(name = "category")
    val category: String,
    @ColumnInfo(name = "code")
    val code: String,
    @ColumnInfo(name = "title")
    val title: String,
    @ColumnInfo(name = "description")
    val description: String,
    @ColumnInfo(name = "severity")
    val severity: String,
    @ColumnInfo(name = "target")
    val target: String,
    @ColumnInfo(name = "isSynced", defaultValue = "0")
    val isSynced: Boolean = false
)
