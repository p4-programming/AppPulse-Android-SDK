package io.apppulse.sdk.health.model

/**
 * Combined health result for one generation.
 *
 * @property overallScore Weighted score from 0 to 100.
 * @property overallStatus Band for [overallScore].
 * @property crashScore Crash module score from 0 to 100.
 * @property performanceScore Performance module score from 0 to 100.
 * @property memoryScore Memory module score from 0 to 100.
 * @property securityScore Security module score from 0 to 100.
 * @property generatedAt Wall-clock time the report was built.
 * @property recommendations Actions suggested by the current inputs.
 * @property categories Per-module scores, including weight and detail text.
 * @property trend Change versus the previous stored report.
 */
data class HealthReport(
    val overallScore: Int,
    val overallStatus: HealthLevel,
    val crashScore: Int,
    val performanceScore: Int,
    val memoryScore: Int,
    val securityScore: Int,
    val generatedAt: Long,
    val recommendations: List<String>,
    val categories: List<HealthScore>,
    val trend: HealthTrend
)

/**
 * One stored overall score used for trend analysis.
 *
 * @property generatedAt Wall-clock time the report was built.
 * @property overallScore Weighted score from 0 to 100.
 * @property overallStatus Band for [overallScore].
 * @property crashScore Crash module score.
 * @property performanceScore Performance module score.
 * @property memoryScore Memory module score.
 * @property securityScore Security module score.
 */
data class HealthSnapshot(
    val generatedAt: Long,
    val overallScore: Int,
    val overallStatus: HealthLevel,
    val crashScore: Int,
    val performanceScore: Int,
    val memoryScore: Int,
    val securityScore: Int
)
