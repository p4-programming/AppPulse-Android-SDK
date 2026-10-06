package io.apppulse.sdk.security

import io.apppulse.sdk.security.model.SecurityIssue
import io.apppulse.sdk.security.model.SecurityLevel
import io.apppulse.sdk.security.model.SecurityScore
import io.apppulse.sdk.security.model.SecuritySeverity

/**
 * Scores a scan from finding severity.
 *
 * The score starts at 100. Each critical finding removes 20, each high finding
 * removes 10, each medium finding removes 5, and each low finding removes 1.
 * The result is clamped to 0–100.
 */
class SecurityHealthCalculator {

    /**
     * Builds a [SecurityScore] from [issues].
     */
    fun calculate(issues: List<SecurityIssue>): SecurityScore {
        val critical = issues.count { it.severity == SecuritySeverity.CRITICAL }
        val high = issues.count { it.severity == SecuritySeverity.HIGH }
        val medium = issues.count { it.severity == SecuritySeverity.MEDIUM }
        val low = issues.count { it.severity == SecuritySeverity.LOW }
        val penalty = critical * CRITICAL_WEIGHT +
            high * HIGH_WEIGHT +
            medium * MEDIUM_WEIGHT +
            low * LOW_WEIGHT
        val score = (BASE_SCORE - penalty).coerceIn(0, BASE_SCORE)
        return SecurityScore(
            score = score,
            level = level(score),
            criticalCount = critical,
            highCount = high,
            mediumCount = medium,
            lowCount = low
        )
    }

    private fun level(score: Int): SecurityLevel {
        return when {
            score >= EXCELLENT_AT -> SecurityLevel.EXCELLENT
            score >= GOOD_AT -> SecurityLevel.GOOD
            score >= WARNING_AT -> SecurityLevel.WARNING
            else -> SecurityLevel.CRITICAL
        }
    }

    private companion object {
        const val BASE_SCORE = 100
        const val CRITICAL_WEIGHT = 20
        const val HIGH_WEIGHT = 10
        const val MEDIUM_WEIGHT = 5
        const val LOW_WEIGHT = 1
        const val EXCELLENT_AT = 90
        const val GOOD_AT = 75
        const val WARNING_AT = 50
    }
}
