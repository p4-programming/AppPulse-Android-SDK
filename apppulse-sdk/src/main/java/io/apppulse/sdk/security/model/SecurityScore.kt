package io.apppulse.sdk.security.model

/**
 * Numeric result of a security scan.
 *
 * @property score Value from 0 to 100 after severity penalties.
 * @property level Band for [score].
 * @property criticalCount Findings with [SecuritySeverity.CRITICAL].
 * @property highCount Findings with [SecuritySeverity.HIGH].
 * @property mediumCount Findings with [SecuritySeverity.MEDIUM].
 * @property lowCount Findings with [SecuritySeverity.LOW].
 */
data class SecurityScore(
    val score: Int,
    val level: SecurityLevel,
    val criticalCount: Int,
    val highCount: Int,
    val mediumCount: Int,
    val lowCount: Int
)

/**
 * Band for a [SecurityScore].
 *
 * 90–100 is [EXCELLENT], 75–89 is [GOOD], 50–74 is [WARNING], and 0–49 is [CRITICAL].
 */
enum class SecurityLevel {
    EXCELLENT,
    GOOD,
    WARNING,
    CRITICAL
}
