package io.apppulse.sdk.storage.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room row for a detected main-thread stall.
 *
 * @property id Surrogate key assigned by Room.
 * @property timestamp Wall-clock time when the stall was detected.
 * @property blockedDurationMs Observed stall length.
 * @property stackTrace Main-thread stack trace.
 * @property threadName Blocked thread name.
 */
@Entity(tableName = "anr_metrics")
data class AnrEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,
    @ColumnInfo(name = "timestamp")
    val timestamp: Long,
    @ColumnInfo(name = "blockedDurationMs")
    val blockedDurationMs: Long,
    @ColumnInfo(name = "stackTrace")
    val stackTrace: String,
    @ColumnInfo(name = "threadName")
    val threadName: String,
    @ColumnInfo(name = "isSynced", defaultValue = "0")
    val isSynced: Boolean = false
)
