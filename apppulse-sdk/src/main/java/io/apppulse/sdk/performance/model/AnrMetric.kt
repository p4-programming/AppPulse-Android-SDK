package io.apppulse.sdk.performance.model

/**
 * A period where the main thread did not run posted work for at least five seconds.
 *
 * @property id Stored row id. `0` before insert.
 * @property timestamp Wall-clock time when the block was detected.
 * @property blockedDurationMs How long the main thread had failed to run the probe.
 * @property stackTrace Main-thread stack at detection time.
 * @property threadName Name of the blocked thread. This is `main`.
 */
data class AnrMetric(
    val id: Long = 0,
    val timestamp: Long,
    val blockedDurationMs: Long,
    val stackTrace: String,
    val threadName: String
)
