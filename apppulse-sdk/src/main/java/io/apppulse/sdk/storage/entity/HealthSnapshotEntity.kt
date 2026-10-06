package io.apppulse.sdk.storage.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room row for one generated health report.
 *
 * @property id Surrogate key assigned by Room.
 * @property generatedAt Wall-clock time the report was built.
 * @property overallScore Weighted score from 0 to 100.
 * @property overallStatus [io.apppulse.sdk.health.model.HealthLevel] name.
 * @property crashScore Crash module score.
 * @property performanceScore Performance module score.
 * @property memoryScore Memory module score.
 * @property securityScore Security module score.
 */
@Entity(tableName = "health_snapshots")
data class HealthSnapshotEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,
    @ColumnInfo(name = "generatedAt")
    val generatedAt: Long,
    @ColumnInfo(name = "overallScore")
    val overallScore: Int,
    @ColumnInfo(name = "overallStatus")
    val overallStatus: String,
    @ColumnInfo(name = "crashScore")
    val crashScore: Int,
    @ColumnInfo(name = "performanceScore")
    val performanceScore: Int,
    @ColumnInfo(name = "memoryScore")
    val memoryScore: Int,
    @ColumnInfo(name = "securityScore")
    val securityScore: Int,
    @ColumnInfo(name = "isSynced", defaultValue = "0")
    val isSynced: Boolean = false
)
