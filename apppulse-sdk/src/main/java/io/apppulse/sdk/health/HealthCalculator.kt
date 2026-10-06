package io.apppulse.sdk.health

import io.apppulse.sdk.health.model.HealthCategory
import io.apppulse.sdk.health.model.HealthLevel
import io.apppulse.sdk.health.model.HealthReport
import io.apppulse.sdk.health.model.HealthScore
import io.apppulse.sdk.health.model.HealthTrend
import io.apppulse.sdk.performance.PerformanceLimits
import io.apppulse.sdk.security.SecurityHealthCalculator
import io.apppulse.sdk.security.model.SecurityIssue
import io.apppulse.sdk.security.model.SecuritySeverity
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Measurements read from the crash tables.
 *
 * Every stored crash is fatal because the SDK records uncaught exceptions.
 *
 * @property totalCrashes All stored crashes.
 * @property crashesLastDay Crashes stored in the last 24 hours.
 * @property fatalCrashes Uncaught crashes. Equal to [totalCrashes].
 */
data class CrashHealthInput(
    val totalCrashes: Int,
    val crashesLastDay: Int,
    val fatalCrashes: Int
)

/**
 * Measurements read from the performance tables.
 *
 * @property averageStartupMs Mean cold-start duration, or null when none exist.
 * @property averageFps Mean frame rate, or null when none exist.
 * @property anrCount Stored main-thread stalls.
 * @property slowApiCount Calls slower than 3000 ms.
 * @property slowScreenCount Screens slower than 1000 ms.
 */
data class PerformanceHealthInput(
    val averageStartupMs: Double?,
    val averageFps: Double?,
    val anrCount: Int,
    val slowApiCount: Int,
    val slowScreenCount: Int
)

/**
 * Measurements read from the memory tables.
 *
 * @property leakCount Stored activity and fragment leak candidates.
 * @property peakMemoryMb Largest used heap, or null when none exist.
 * @property averageMemoryMb Mean used heap, or null when none exist.
 * @property maxMemoryMb Max heap from the newest sample, or null when none exist.
 * @property bitmapViolations Large bitmaps in the newest bitmap sample.
 */
data class MemoryHealthInput(
    val leakCount: Int,
    val peakMemoryMb: Double?,
    val averageMemoryMb: Double?,
    val maxMemoryMb: Double?,
    val bitmapViolations: Int
)

/**
 * Severity counts from the latest security scan.
 */
data class SecurityHealthInput(
    val criticalCount: Int,
    val highCount: Int,
    val mediumCount: Int,
    val lowCount: Int
)

/**
 * Builds category scores, the weighted overall score, and recommendations.
 *
 * Crash starts at 100. Each stored crash removes 8 points, up to 40. Each
 * crash in the last 24 hours removes 6, up to 30. Each fatal crash removes 8,
 * up to 30.
 *
 * Performance starts at 100. A cold start above 2000 ms removes 15, or 25
 * above 4000 ms. Average FPS below 45 removes 20, or 35 below 30. Each ANR
 * removes 15, up to 30. Each slow API or slow screen removes 5, up to 15.
 *
 * Memory starts at 100. Each leak removes 6, up to 36. Peak heap above 60,
 * 75, and 90 percent of max removes 8, 15, and 25. Average heap above 60 and
 * 75 percent removes 8 and 15. Each large bitmap removes 8, up to 24.
 *
 * Security uses the same weights as [SecurityHealthCalculator]: critical 20,
 * high 10, medium 5, and low 1.
 *
 * The overall score is crash × 0.30 + performance × 0.30 + memory × 0.20 +
 * security × 0.20.
 */
class HealthCalculator(
    private val securityCalculator: SecurityHealthCalculator = SecurityHealthCalculator()
) {

    /**
     * Builds a [HealthReport] from the current module inputs.
     *
     * @param previousOverall Overall score of the previous report, or null.
     */
    fun calculate(
        crash: CrashHealthInput,
        performance: PerformanceHealthInput,
        memory: MemoryHealthInput,
        security: SecurityHealthInput,
        previousOverall: Int?,
        generatedAt: Long
    ): HealthReport {
        val crashScore = category(
            category = HealthCategory.CRASH,
            score = crashScore(crash),
            weight = CRASH_WEIGHT,
            detail = "${crash.totalCrashes.coerceAtLeast(0)} crashes · " +
                "${crash.crashesLastDay.coerceAtLeast(0)} in 24h · " +
                "${crash.fatalCrashes.coerceAtLeast(0)} fatal"
        )
        val performanceScore = category(
            category = HealthCategory.PERFORMANCE,
            score = performanceScore(performance),
            weight = PERFORMANCE_WEIGHT,
            detail = performanceDetail(performance)
        )
        val memoryScore = category(
            category = HealthCategory.MEMORY,
            score = memoryScore(memory),
            weight = MEMORY_WEIGHT,
            detail = memoryDetail(memory)
        )
        val securityScore = category(
            category = HealthCategory.SECURITY,
            score = securityScore(security),
            weight = SECURITY_WEIGHT,
            detail = "${security.criticalCount.coerceAtLeast(0)} critical · " +
                "${security.highCount.coerceAtLeast(0)} high · " +
                "${security.mediumCount.coerceAtLeast(0)} medium · " +
                "${security.lowCount.coerceAtLeast(0)} low"
        )
        val overall = weighted(
            crashScore.score,
            performanceScore.score,
            memoryScore.score,
            securityScore.score
        )
        val recommendations = recommendations(crash, performance, memory, security)
        return HealthReport(
            overallScore = overall,
            overallStatus = level(overall),
            crashScore = crashScore.score,
            performanceScore = performanceScore.score,
            memoryScore = memoryScore.score,
            securityScore = securityScore.score,
            generatedAt = generatedAt,
            recommendations = recommendations,
            categories = listOf(crashScore, performanceScore, memoryScore, securityScore),
            trend = trend(overall, previousOverall)
        )
    }

    /**
     * Returns the band for [score].
     */
    fun level(score: Int): HealthLevel {
        return when {
            score >= EXCELLENT_AT -> HealthLevel.EXCELLENT
            score >= GOOD_AT -> HealthLevel.GOOD
            score >= WARNING_AT -> HealthLevel.WARNING
            else -> HealthLevel.CRITICAL
        }
    }

    private fun crashScore(input: CrashHealthInput): Int {
        val totalPenalty = (input.totalCrashes.coerceAtLeast(0) * CRASH_POINT).coerceAtMost(MAX_CRASH_PENALTY)
        val frequencyPenalty = (input.crashesLastDay.coerceAtLeast(0) * FREQUENCY_POINT)
            .coerceAtMost(MAX_FREQUENCY_PENALTY)
        val fatalPenalty = (input.fatalCrashes.coerceAtLeast(0) * FATAL_POINT).coerceAtMost(MAX_FATAL_PENALTY)
        return (BASE_SCORE - totalPenalty - frequencyPenalty - fatalPenalty).coerceIn(0, BASE_SCORE)
    }

    private fun performanceScore(input: PerformanceHealthInput): Int {
        val startupPenalty = when {
            input.averageStartupMs == null -> 0
            input.averageStartupMs > SLOW_STARTUP_MS * 2 -> 25
            input.averageStartupMs > PerformanceLimits.SLOW_STARTUP_MS -> 15
            else -> 0
        }
        val fpsPenalty = when {
            input.averageFps == null -> 0
            input.averageFps < 30.0 -> 35
            input.averageFps < PerformanceLimits.LOW_FPS -> 20
            else -> 0
        }
        val anrPenalty = (input.anrCount.coerceAtLeast(0) * ANR_POINT).coerceAtMost(MAX_ANR_PENALTY)
        val apiPenalty = (input.slowApiCount.coerceAtLeast(0) * SLOW_POINT).coerceAtMost(MAX_SLOW_PENALTY)
        val screenPenalty = (input.slowScreenCount.coerceAtLeast(0) * SLOW_POINT).coerceAtMost(MAX_SLOW_PENALTY)
        return (BASE_SCORE - startupPenalty - fpsPenalty - anrPenalty - apiPenalty - screenPenalty)
            .coerceIn(0, BASE_SCORE)
    }

    private fun memoryScore(input: MemoryHealthInput): Int {
        val leakPenalty = (input.leakCount.coerceAtLeast(0) * LEAK_POINT).coerceAtMost(MAX_LEAK_PENALTY)
        val peakPenalty = ratioPenalty(input.peakMemoryMb, input.maxMemoryMb, peak = true)
        val averagePenalty = ratioPenalty(input.averageMemoryMb, input.maxMemoryMb, peak = false)
        val bitmapPenalty = (input.bitmapViolations.coerceAtLeast(0) * BITMAP_POINT)
            .coerceAtMost(MAX_BITMAP_PENALTY)
        return (BASE_SCORE - leakPenalty - peakPenalty - averagePenalty - bitmapPenalty)
            .coerceIn(0, BASE_SCORE)
    }

    private fun ratioPenalty(usedMb: Double?, maxMb: Double?, peak: Boolean): Int {
        if (usedMb == null || maxMb == null || maxMb <= 0.0) {
            return 0
        }
        val ratio = usedMb / maxMb
        return if (peak) {
            when {
                ratio >= 0.90 -> 25
                ratio >= 0.75 -> 15
                ratio >= 0.60 -> 8
                else -> 0
            }
        } else {
            when {
                ratio >= 0.75 -> 15
                ratio >= 0.60 -> 8
                else -> 0
            }
        }
    }

    private fun securityScore(input: SecurityHealthInput): Int {
        val issues = ArrayList<SecurityIssue>()
        repeat(input.criticalCount.coerceAtLeast(0)) { index ->
            issues += securityPlaceholder(SecuritySeverity.CRITICAL, index)
        }
        repeat(input.highCount.coerceAtLeast(0)) { index ->
            issues += securityPlaceholder(SecuritySeverity.HIGH, index)
        }
        repeat(input.mediumCount.coerceAtLeast(0)) { index ->
            issues += securityPlaceholder(SecuritySeverity.MEDIUM, index)
        }
        repeat(input.lowCount.coerceAtLeast(0)) { index ->
            issues += securityPlaceholder(SecuritySeverity.LOW, index)
        }
        return securityCalculator.calculate(issues).score
    }

    private fun securityPlaceholder(severity: SecuritySeverity, index: Int): SecurityIssue {
        return SecurityIssue(
            category = SecurityIssue.CATEGORY_APPLICATION,
            code = "HEALTH",
            title = "count",
            description = "",
            severity = severity,
            target = "$severity-$index",
            timestamp = 0L
        )
    }

    private fun recommendations(
        crash: CrashHealthInput,
        performance: PerformanceHealthInput,
        memory: MemoryHealthInput,
        security: SecurityHealthInput
    ): List<String> {
        val lines = ArrayList<String>()
        if (crash.totalCrashes > 0) {
            lines += "Investigate recorded crashes"
        }
        if (performance.averageFps != null && performance.averageFps < PerformanceLimits.LOW_FPS) {
            lines += "Improve rendering performance"
        }
        if (performance.averageStartupMs != null &&
            performance.averageStartupMs > PerformanceLimits.SLOW_STARTUP_MS
        ) {
            lines += "Reduce cold-start time"
        }
        if (performance.anrCount > 0) {
            lines += "Resolve application not responding stalls"
        }
        if (performance.slowApiCount > 0) {
            lines += "Speed up slow API calls"
        }
        if (performance.slowScreenCount > 0) {
            lines += "Reduce slow screen rendering"
        }
        if (memory.leakCount > 5) {
            lines += "Investigate memory leaks"
        }
        if (isHighMemory(memory.peakMemoryMb, memory.maxMemoryMb)) {
            lines += "Reduce peak memory usage"
        }
        if (memory.bitmapViolations > 0) {
            lines += "Reduce large bitmap usage"
        }
        if (security.criticalCount > 0) {
            lines += "Resolve critical security issues"
        }
        if (security.highCount > 0) {
            lines += "Review high-severity security findings"
        }
        if (lines.isEmpty()) {
            lines += "No issues need attention"
        }
        return lines
    }

    private fun isHighMemory(usedMb: Double?, maxMb: Double?): Boolean {
        if (usedMb == null || maxMb == null || maxMb <= 0.0) {
            return false
        }
        return usedMb / maxMb >= 0.75
    }

    private fun category(
        category: HealthCategory,
        score: Int,
        weight: Double,
        detail: String
    ): HealthScore {
        return HealthScore(
            category = category,
            score = score,
            level = level(score),
            weight = weight,
            detail = detail
        )
    }

    private fun weighted(crash: Int, performance: Int, memory: Int, security: Int): Int {
        val sum = crash * CRASH_WEIGHT_POINTS +
            performance * PERFORMANCE_WEIGHT_POINTS +
            memory * MEMORY_WEIGHT_POINTS +
            security * SECURITY_WEIGHT_POINTS
        return (sum / 100.0).roundToInt().coerceIn(0, BASE_SCORE)
    }

    private fun trend(overall: Int, previous: Int?): HealthTrend {
        if (previous == null) {
            return HealthTrend.UNKNOWN
        }
        val delta = overall - previous
        return when {
            delta >= TREND_STEP -> HealthTrend.IMPROVING
            delta <= -TREND_STEP -> HealthTrend.DECLINING
            else -> HealthTrend.STABLE
        }
    }

    private fun performanceDetail(input: PerformanceHealthInput): String {
        val startup = input.averageStartupMs?.toLong()?.toString() ?: "—"
        val fps = input.averageFps?.toInt()?.toString() ?: "—"
        return "Startup $startup ms · FPS $fps · ${input.anrCount.coerceAtLeast(0)} ANR · " +
            "${input.slowApiCount.coerceAtLeast(0)} slow APIs · " +
            "${input.slowScreenCount.coerceAtLeast(0)} slow screens"
    }

    private fun memoryDetail(input: MemoryHealthInput): String {
        val peak = input.peakMemoryMb?.let { String.format(Locale.US, "%.0f", it) } ?: "—"
        val average = input.averageMemoryMb?.let { String.format(Locale.US, "%.0f", it) } ?: "—"
        return "${input.leakCount.coerceAtLeast(0)} leaks · peak $peak MB · " +
            "average $average MB · ${input.bitmapViolations.coerceAtLeast(0)} large bitmaps"
    }

    private companion object {
        const val BASE_SCORE = 100
        const val CRASH_WEIGHT = 0.30
        const val PERFORMANCE_WEIGHT = 0.30
        const val MEMORY_WEIGHT = 0.20
        const val SECURITY_WEIGHT = 0.20
        const val CRASH_WEIGHT_POINTS = 30
        const val PERFORMANCE_WEIGHT_POINTS = 30
        const val MEMORY_WEIGHT_POINTS = 20
        const val SECURITY_WEIGHT_POINTS = 20
        const val CRASH_POINT = 8
        const val MAX_CRASH_PENALTY = 40
        const val FREQUENCY_POINT = 6
        const val MAX_FREQUENCY_PENALTY = 30
        const val FATAL_POINT = 8
        const val MAX_FATAL_PENALTY = 30
        const val ANR_POINT = 15
        const val MAX_ANR_PENALTY = 30
        const val SLOW_POINT = 5
        const val MAX_SLOW_PENALTY = 15
        const val LEAK_POINT = 6
        const val MAX_LEAK_PENALTY = 36
        const val BITMAP_POINT = 8
        const val MAX_BITMAP_PENALTY = 24
        const val SLOW_STARTUP_MS = 2_000.0
        const val EXCELLENT_AT = 90
        const val GOOD_AT = 75
        const val WARNING_AT = 50
        const val TREND_STEP = 5
    }
}
