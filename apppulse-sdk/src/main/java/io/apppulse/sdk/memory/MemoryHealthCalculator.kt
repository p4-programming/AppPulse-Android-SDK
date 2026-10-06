package io.apppulse.sdk.memory

import io.apppulse.sdk.memory.model.MemoryHealthLevel
import io.apppulse.sdk.memory.model.MemoryHealthReport
import io.apppulse.sdk.memory.model.MemoryTrend

/**
 * Scores memory health from heap pressure, leak candidates, and large bitmaps.
 *
 * The score starts at 100. Each leak removes 15 points, up to 45. Heap usage
 * above 60 percent of max removes 10, above 75 percent removes 20, and above
 * 90 percent removes 35. Each bitmap larger than 5 MB removes 10 points, up
 * to 30. The result is clamped to 0–100.
 */
class MemoryHealthCalculator {

    /**
     * Builds a [MemoryHealthReport] from the current counters.
     *
     * @param usedMemoryMb Java heap currently retained.
     * @param maxMemoryMb Maximum Java heap.
     * @param leakCount Leak candidates still counted against the score.
     * @param largeBitmapCount Bitmaps currently larger than 5 MB.
     * @param recentUsedMb Oldest-to-newest used-heap samples used for the trend.
     */
    fun calculate(
        usedMemoryMb: Double,
        maxMemoryMb: Double,
        leakCount: Int,
        largeBitmapCount: Int,
        recentUsedMb: List<Double> = emptyList()
    ): MemoryHealthReport {
        val leakPenalty = (leakCount.coerceAtLeast(0) * LEAK_POINT).coerceAtMost(MAX_LEAK_PENALTY)
        val bitmapPenalty = (largeBitmapCount.coerceAtLeast(0) * BITMAP_POINT).coerceAtMost(MAX_BITMAP_PENALTY)
        val memoryPenalty = memoryPenalty(usedMemoryMb, maxMemoryMb)
        val score = (BASE_SCORE - leakPenalty - memoryPenalty - bitmapPenalty).coerceIn(0, BASE_SCORE)
        return MemoryHealthReport(
            score = score,
            level = level(score),
            leakPenalty = leakPenalty,
            memoryPenalty = memoryPenalty,
            bitmapPenalty = bitmapPenalty,
            trend = trend(recentUsedMb),
            usedMemoryMb = usedMemoryMb,
            leakCount = leakCount.coerceAtLeast(0),
            largeBitmapCount = largeBitmapCount.coerceAtLeast(0)
        )
    }

    private fun memoryPenalty(usedMemoryMb: Double, maxMemoryMb: Double): Int {
        if (maxMemoryMb <= 0.0) {
            return 0
        }
        val ratio = usedMemoryMb / maxMemoryMb
        return when {
            ratio >= 0.90 -> 35
            ratio >= 0.75 -> 20
            ratio >= 0.60 -> 10
            else -> 0
        }
    }

    private fun level(score: Int): MemoryHealthLevel {
        return when {
            score >= 90 -> MemoryHealthLevel.EXCELLENT
            score >= 75 -> MemoryHealthLevel.GOOD
            score >= 50 -> MemoryHealthLevel.WARNING
            else -> MemoryHealthLevel.CRITICAL
        }
    }

    /**
     * Compares the older half of [recentUsedMb] with the newer half.
     * A 10 percent rise is [MemoryTrend.RISING]. A 10 percent drop is [MemoryTrend.FALLING].
     */
    private fun trend(recentUsedMb: List<Double>): MemoryTrend {
        if (recentUsedMb.size < 4) {
            return MemoryTrend.UNKNOWN
        }
        val midpoint = recentUsedMb.size / 2
        val older = recentUsedMb.take(midpoint).average()
        val newer = recentUsedMb.drop(midpoint).average()
        if (older <= 0.0) {
            return MemoryTrend.UNKNOWN
        }
        val change = (newer - older) / older
        return when {
            change >= 0.10 -> MemoryTrend.RISING
            change <= -0.10 -> MemoryTrend.FALLING
            else -> MemoryTrend.STABLE
        }
    }

    private companion object {
        const val BASE_SCORE = 100
        const val LEAK_POINT = 15
        const val MAX_LEAK_PENALTY = 45
        const val BITMAP_POINT = 10
        const val MAX_BITMAP_PENALTY = 30
    }
}
