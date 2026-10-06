package io.apppulse.sdk.performance.model

/**
 * Time from an activity becoming visible to its first drawn frame.
 *
 * @property id Stored row id. `0` before insert.
 * @property activityName Simple class name of the activity.
 * @property openTimestamp Wall-clock time when the activity started.
 * @property fullyDrawnTimestamp Wall-clock time of the first drawn frame.
 * @property renderTimeMs Elapsed time between those two points.
 * @property slowScreen True when [renderTimeMs] is greater than 1000.
 */
data class ScreenMetric(
    val id: Long = 0,
    val activityName: String,
    val openTimestamp: Long,
    val fullyDrawnTimestamp: Long,
    val renderTimeMs: Long,
    val slowScreen: Boolean
)
