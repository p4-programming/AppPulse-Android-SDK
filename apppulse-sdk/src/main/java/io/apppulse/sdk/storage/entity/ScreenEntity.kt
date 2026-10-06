package io.apppulse.sdk.storage.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room row for one screen render.
 *
 * @property id Surrogate key assigned by Room.
 * @property activityName Activity simple name.
 * @property openTimestamp Wall-clock time the activity started.
 * @property fullyDrawnTimestamp Wall-clock time of the first drawn frame.
 * @property renderTimeMs Elapsed render duration.
 * @property slowScreen True when rendering took longer than 1000 ms.
 */
@Entity(tableName = "screen_metrics")
data class ScreenEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,
    @ColumnInfo(name = "activityName")
    val activityName: String,
    @ColumnInfo(name = "openTimestamp")
    val openTimestamp: Long,
    @ColumnInfo(name = "fullyDrawnTimestamp")
    val fullyDrawnTimestamp: Long,
    @ColumnInfo(name = "renderTimeMs")
    val renderTimeMs: Long,
    @ColumnInfo(name = "slowScreen")
    val slowScreen: Boolean,
    @ColumnInfo(name = "isSynced", defaultValue = "0")
    val isSynced: Boolean = false
)
