package io.apppulse.sdk.security.model

/**
 * Stored result of one completed security scan.
 *
 * @property scannedAt Wall-clock time of the newest finding, or `0` when there are none.
 * @property issues Findings ordered by severity, then target.
 * @property score Health score calculated from [issues].
 */
data class SecurityReport(
    val scannedAt: Long,
    val issues: List<SecurityIssue>,
    val score: SecurityScore
)
