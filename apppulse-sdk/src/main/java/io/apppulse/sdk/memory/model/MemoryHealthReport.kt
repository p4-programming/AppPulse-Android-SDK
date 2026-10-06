package io.apppulse.sdk.memory.model

/**
 * Direction of recent Java-heap usage.
 */
enum class MemoryTrend {
    RISING,
    STABLE,
    FALLING,
    UNKNOWN
}

/**
 * Band for a [MemoryHealthReport.score].
 */
enum class MemoryHealthLevel {
    EXCELLENT,
    GOOD,
    WARNING,
    CRITICAL
}

/**
 * Score and the penalties that produced it.
 *
 * Bands are 90–100 Excellent, 75–89 Good, 50–74 Warning, and 0–49 Critical.
 *
 * @property score Value from 0 to 100.
 * @property level Band that contains [score].
 * @property leakPenalty Points removed for live leak candidates.
 * @property memoryPenalty Points removed because the heap is under pressure.
 * @property bitmapPenalty Points removed for bitmaps larger than 5 MB.
 * @property trend Recent direction of used-heap samples.
 * @property usedMemoryMb Used heap used for the memory penalty.
 * @property leakCount Leak candidates included in [leakPenalty].
 * @property largeBitmapCount Large bitmaps included in [bitmapPenalty].
 */
data class MemoryHealthReport(
    val score: Int,
    val level: MemoryHealthLevel,
    val leakPenalty: Int,
    val memoryPenalty: Int,
    val bitmapPenalty: Int,
    val trend: MemoryTrend,
    val usedMemoryMb: Double,
    val leakCount: Int,
    val largeBitmapCount: Int
)
