package io.apppulse.sdk.health.model

/**
 * Score for one [HealthCategory].
 *
 * @property category Module this score describes.
 * @property score Value from 0 to 100.
 * @property level Band for [score].
 * @property weight Share of the overall score, from 0 to 1.
 * @property detail Short explanation of the inputs that produced [score].
 */
data class HealthScore(
    val category: HealthCategory,
    val score: Int,
    val level: HealthLevel,
    val weight: Double,
    val detail: String
)

/**
 * Band for a health score.
 *
 * 90–100 is [EXCELLENT], 75–89 is [GOOD], 50–74 is [WARNING], and 0–49 is [CRITICAL].
 */
enum class HealthLevel {
    EXCELLENT,
    GOOD,
    WARNING,
    CRITICAL
}

/**
 * Direction of the overall score compared with the previous report.
 */
enum class HealthTrend {
    IMPROVING,
    DECLINING,
    STABLE,
    UNKNOWN
}
