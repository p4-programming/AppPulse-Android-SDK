package io.apppulse.sdk.performance.model

/**
 * Aggregated performance figures shown on the dashboard summary card.
 *
 * @property averageStartupMs Mean cold-start duration, or null when none are stored.
 * @property averageFps Mean sampled FPS, or null when none are stored.
 * @property slowestScreenName Activity with the largest render time.
 * @property slowestScreenMs That render time, in milliseconds.
 * @property slowestApiLabel Short label of the slowest stored request.
 * @property slowestApiMs That request duration, in milliseconds.
 * @property anrCount Number of stored ANR records.
 */
data class PerformanceReport(
    val averageStartupMs: Long?,
    val averageFps: Double?,
    val slowestScreenName: String?,
    val slowestScreenMs: Long?,
    val slowestApiLabel: String?,
    val slowestApiMs: Long?,
    val anrCount: Int
)
