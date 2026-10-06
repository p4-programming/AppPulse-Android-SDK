package io.apppulse.sdk.network.dto

/**
 * One crash report sent to `POST /api/v1/crashes`.
 *
 * @property id Local Room id. The server can use it to deduplicate retries.
 */
data class CrashDto(
    val id: Long,
    val timestamp: Long,
    val exceptionType: String,
    val message: String,
    val stackTrace: String,
    val deviceManufacturer: String,
    val deviceModel: String,
    val androidVersion: String,
    val appVersion: String
)
