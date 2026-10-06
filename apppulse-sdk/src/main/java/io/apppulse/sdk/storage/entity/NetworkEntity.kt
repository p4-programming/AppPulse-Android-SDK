package io.apppulse.sdk.storage.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room row for one HTTP call.
 *
 * @property id Surrogate key assigned by Room.
 * @property url Request URL without the query string.
 * @property method HTTP method.
 * @property responseCode HTTP status, or `0` when the call failed.
 * @property requestSize Request body size in bytes.
 * @property responseSize Response body size in bytes.
 * @property durationMs Call duration.
 * @property slowApi True when the call took longer than 3000 ms.
 * @property timestamp Wall-clock time when the call finished.
 */
@Entity(tableName = "network_metrics")
data class NetworkEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,
    @ColumnInfo(name = "url")
    val url: String,
    @ColumnInfo(name = "method")
    val method: String,
    @ColumnInfo(name = "responseCode")
    val responseCode: Int,
    @ColumnInfo(name = "requestSize")
    val requestSize: Long,
    @ColumnInfo(name = "responseSize")
    val responseSize: Long,
    @ColumnInfo(name = "durationMs")
    val durationMs: Long,
    @ColumnInfo(name = "slowApi")
    val slowApi: Boolean,
    @ColumnInfo(name = "timestamp")
    val timestamp: Long,
    @ColumnInfo(name = "isSynced", defaultValue = "0")
    val isSynced: Boolean = false
)
