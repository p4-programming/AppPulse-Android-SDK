package io.apppulse.sdk.storage.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room row for one FPS sample.
 *
 * @property id Surrogate key assigned by Room.
 * @property timestamp Wall-clock time when the sample window closed.
 * @property fps Frames per second in the window.
 * @property droppedFrames Frames missed against the 16.67 ms budget.
 * @property averageFps Running session average at sample time.
 * @property performanceIssue True when [fps] is below 45.
 */
@Entity(tableName = "fps_metrics")
data class FpsEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,
    @ColumnInfo(name = "timestamp")
    val timestamp: Long,
    @ColumnInfo(name = "fps")
    val fps: Double,
    @ColumnInfo(name = "droppedFrames")
    val droppedFrames: Int,
    @ColumnInfo(name = "averageFps")
    val averageFps: Double,
    @ColumnInfo(name = "performanceIssue")
    val performanceIssue: Boolean,
    @ColumnInfo(name = "isSynced", defaultValue = "0")
    val isSynced: Boolean = false
)
