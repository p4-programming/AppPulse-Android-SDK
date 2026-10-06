package io.apppulse.sdk.crash

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import io.apppulse.sdk.model.CrashEvent
import java.text.DateFormat
import java.util.Date

/**
 * Stored record of one uncaught exception.
 *
 * Rows live in the `crashes` table of `apppulse.db`. [id] is assigned by Room
 * when the row is inserted.
 *
 * @property id Surrogate key. `0` means the row has not been inserted yet.
 * @property timestamp Epoch milliseconds when the exception was captured.
 * @property exceptionType Fully qualified exception class name.
 * @property message Exception message. Empty when the throwable had none.
 * @property stackTrace Complete stack trace, including causes.
 * @property deviceManufacturer Device manufacturer at capture time.
 * @property deviceModel Device model at capture time.
 * @property androidVersion Android release and API level at capture time.
 * @property appVersion Host application version name at capture time.
 * @property isSynced True after the row has been accepted by the remote API.
 */
@Entity(
    tableName = "crashes",
    indices = [Index(value = ["timestamp"])]
)
data class CrashEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,
    @ColumnInfo(name = "timestamp")
    val timestamp: Long,
    @ColumnInfo(name = "exceptionType")
    val exceptionType: String,
    @ColumnInfo(name = "message")
    val message: String,
    @ColumnInfo(name = "stackTrace")
    val stackTrace: String,
    @ColumnInfo(name = "deviceManufacturer")
    val deviceManufacturer: String,
    @ColumnInfo(name = "deviceModel")
    val deviceModel: String,
    @ColumnInfo(name = "androidVersion")
    val androidVersion: String,
    @ColumnInfo(name = "appVersion")
    val appVersion: String,
    @ColumnInfo(name = "isSynced", defaultValue = "0")
    val isSynced: Boolean = false
) {

    /**
     * Returns the simple exception class name for compact list rows.
     */
    fun simpleExceptionType(): String {
        return exceptionType.substringAfterLast('.').ifBlank { exceptionType }
    }

    /**
     * Returns the manufacturer and model as a single device label.
     */
    fun deviceLabel(): String {
        return "$deviceManufacturer $deviceModel".trim().ifBlank { "unknown" }
    }

    /**
     * Returns a localized date and time for [timestamp].
     */
    fun formattedTimestamp(): String {
        val formatter = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM)
        return formatter.format(Date(timestamp))
    }

    /**
     * Returns [message], or a readable placeholder when the throwable had none.
     */
    fun displayMessage(): String {
        return message.ifBlank { "No message" }
    }

    companion object {
        /**
         * Maps a captured [CrashEvent] into a row that Room can insert.
         *
         * The stack trace is copied in full. The generated [id] stays `0` so
         * SQLite assigns the next key.
         */
        fun from(event: CrashEvent): CrashEntity {
            return CrashEntity(
                timestamp = event.timestampEpochMillis,
                exceptionType = event.exceptionName.ifBlank { "unknown" },
                message = event.message.orEmpty(),
                stackTrace = event.stackTrace,
                deviceManufacturer = event.deviceInfo.manufacturer,
                deviceModel = event.deviceInfo.model,
                androidVersion = event.deviceInfo.androidVersion,
                appVersion = event.deviceInfo.appVersion
            )
        }
    }
}
