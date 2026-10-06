package io.apppulse.sdk.performance

/**
 * Thresholds that mark a performance sample as a problem.
 */
internal object PerformanceLimits {
    const val SLOW_STARTUP_MS = 2_000L
    const val SLOW_SCREEN_MS = 1_000L
    const val SLOW_API_MS = 3_000L
    const val LOW_FPS = 45.0
    const val ANR_BLOCKED_MS = 5_000L
    const val FRAME_BUDGET_NANOS = 16_666_667L
}
