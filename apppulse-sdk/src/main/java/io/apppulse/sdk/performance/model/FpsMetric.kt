package io.apppulse.sdk.performance.model

/**
 * One second of frame rendering while the host app is in the foreground.
 *
 * @property id Stored row id. `0` before insert.
 * @property timestamp Wall-clock time when the sample window closed.
 * @property fps Frames per second during the window.
 * @property droppedFrames Frames missed against a 16.67 ms budget.
 * @property averageFps Running average FPS for the current monitoring session.
 * @property performanceIssue True when [fps] is below 45.
 */
data class FpsMetric(
    val id: Long = 0,
    val timestamp: Long,
    val fps: Double,
    val droppedFrames: Int,
    val averageFps: Double,
    val performanceIssue: Boolean
)
