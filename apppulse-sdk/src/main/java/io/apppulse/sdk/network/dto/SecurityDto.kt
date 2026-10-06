package io.apppulse.sdk.network.dto

/**
 * Security batch sent to `POST /api/v1/security`.
 */
data class SecurityDto(
    val issues: List<SecurityIssueDto>
) {
    /**
     * True when [issues] is empty.
     */
    fun isEmpty(): Boolean = issues.isEmpty()
}

/**
 * One security finding.
 */
data class SecurityIssueDto(
    val id: Long,
    val timestamp: Long,
    val category: String,
    val code: String,
    val title: String,
    val description: String,
    val severity: String,
    val target: String
)
