package io.apppulse.sdk.network.dto

/**
 * Performance batch sent to `POST /api/v1/performance`.
 */
data class PerformanceDto(
    val startups: List<StartupDto>,
    val screens: List<ScreenDto>,
    val calls: List<NetworkCallDto>,
    val fps: List<FpsDto>,
    val anrs: List<AnrDto>
) {
    /**
     * True when every list is empty.
     */
    fun isEmpty(): Boolean {
        return startups.isEmpty() && screens.isEmpty() && calls.isEmpty() && fps.isEmpty() && anrs.isEmpty()
    }
}

/**
 * One cold-start measurement.
 */
data class StartupDto(
    val id: Long,
    val launchTimestamp: Long,
    val firstActivityTimestamp: Long,
    val startupTimeMs: Long,
    val slowStartup: Boolean,
    val activityName: String
)

/**
 * One screen render measurement.
 */
data class ScreenDto(
    val id: Long,
    val activityName: String,
    val openTimestamp: Long,
    val fullyDrawnTimestamp: Long,
    val renderTimeMs: Long,
    val slowScreen: Boolean
)

/**
 * One recorded HTTP call.
 */
data class NetworkCallDto(
    val id: Long,
    val url: String,
    val method: String,
    val responseCode: Int,
    val requestSize: Long,
    val responseSize: Long,
    val durationMs: Long,
    val slowApi: Boolean,
    val timestamp: Long
)

/**
 * One frame-rate sample.
 */
data class FpsDto(
    val id: Long,
    val timestamp: Long,
    val fps: Double,
    val droppedFrames: Int,
    val averageFps: Double,
    val performanceIssue: Boolean
)

/**
 * One main-thread stall.
 */
data class AnrDto(
    val id: Long,
    val timestamp: Long,
    val blockedDurationMs: Long,
    val stackTrace: String,
    val threadName: String
)
