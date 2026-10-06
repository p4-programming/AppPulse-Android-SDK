package io.apppulse.sdk.performance.model

/**
 * Cold-start measurement from process launch to the first drawn activity frame.
 *
 * @property id Stored row id. `0` before insert.
 * @property launchTimestamp Wall-clock time when the process started.
 * @property firstActivityTimestamp Wall-clock time when the first activity frame was drawn.
 * @property startupTimeMs Elapsed time from process start to that frame.
 * @property slowStartup True when [startupTimeMs] is greater than 2000.
 * @property activityName Simple name of the first displayed activity.
 */
data class StartupMetric(
    val id: Long = 0,
    val launchTimestamp: Long,
    val firstActivityTimestamp: Long,
    val startupTimeMs: Long,
    val slowStartup: Boolean,
    val activityName: String
)
