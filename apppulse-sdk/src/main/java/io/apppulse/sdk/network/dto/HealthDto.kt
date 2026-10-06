package io.apppulse.sdk.network.dto

/**
 * Health batch sent to `POST /api/v1/health`.
 */
data class HealthDto(
    val reports: List<HealthReportDto>
) {
    /**
     * True when [reports] is empty.
     */
    fun isEmpty(): Boolean = reports.isEmpty()
}

/**
 * One generated health report.
 */
data class HealthReportDto(
    val id: Long,
    val generatedAt: Long,
    val overallScore: Int,
    val overallStatus: String,
    val crashScore: Int,
    val performanceScore: Int,
    val memoryScore: Int,
    val securityScore: Int
)
