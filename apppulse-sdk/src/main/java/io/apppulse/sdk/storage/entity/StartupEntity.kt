package io.apppulse.sdk.storage.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room row for a cold-start measurement.
 *
 * @property id Surrogate key assigned by Room.
 * @property launchTimestamp Wall-clock process start time.
 * @property firstActivityTimestamp Wall-clock time of the first drawn activity frame.
 * @property startupTimeMs Elapsed cold-start duration.
 * @property slowStartup True when the start took longer than 2000 ms.
 * @property activityName First displayed activity.
 */
@Entity(tableName = "startup_metrics")
data class StartupEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,
    @ColumnInfo(name = "launchTimestamp")
    val launchTimestamp: Long,
    @ColumnInfo(name = "firstActivityTimestamp")
    val firstActivityTimestamp: Long,
    @ColumnInfo(name = "startupTimeMs")
    val startupTimeMs: Long,
    @ColumnInfo(name = "slowStartup")
    val slowStartup: Boolean,
    @ColumnInfo(name = "activityName")
    val activityName: String,
    @ColumnInfo(name = "isSynced", defaultValue = "0")
    val isSynced: Boolean = false
)
