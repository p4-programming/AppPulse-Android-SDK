package io.apppulse.sdk.security.model

/**
 * One finding from a security scan.
 *
 * @property id Room row id, or `0` before insert.
 * @property category Scan area. One of [CATEGORY_MANIFEST], [CATEGORY_APPLICATION],
 * [CATEGORY_PERMISSION], [CATEGORY_NETWORK], or [CATEGORY_WEBVIEW].
 * @property code Stable check identifier, such as `EXPORTED_ACTIVITY`.
 * @property title Short name shown on the dashboard.
 * @property description Why the finding matters.
 * @property severity How heavily the finding reduces the health score.
 * @property target Component, permission, or URL the finding refers to.
 * @property timestamp Wall-clock time of the scan that produced the finding.
 */
data class SecurityIssue(
    val id: Long = 0,
    val category: String,
    val code: String,
    val title: String,
    val description: String,
    val severity: SecuritySeverity,
    val target: String,
    val timestamp: Long
) {
    companion object {
        const val CATEGORY_MANIFEST = "MANIFEST"
        const val CATEGORY_APPLICATION = "APPLICATION"
        const val CATEGORY_PERMISSION = "PERMISSION"
        const val CATEGORY_NETWORK = "NETWORK"
        const val CATEGORY_WEBVIEW = "WEBVIEW"

        const val CODE_EXPORTED_ACTIVITY = "EXPORTED_ACTIVITY"
        const val CODE_EXPORTED_SERVICE = "EXPORTED_SERVICE"
        const val CODE_EXPORTED_RECEIVER = "EXPORTED_RECEIVER"
        const val CODE_EXPORTED_PROVIDER = "EXPORTED_PROVIDER"
        const val CODE_DEBUGGABLE = "DEBUGGABLE"
        const val CODE_ALLOW_BACKUP = "ALLOW_BACKUP"
        const val CODE_RISKY_PERMISSION = "RISKY_PERMISSION"
        const val CODE_CLEARTEXT = "CLEARTEXT_TRAFFIC"
        const val CODE_HTTP_URL = "HTTP_URL"
        const val CODE_WEBVIEW_JAVASCRIPT = "WEBVIEW_JAVASCRIPT"
        const val CODE_WEBVIEW_FILE_ACCESS = "WEBVIEW_FILE_ACCESS"
        const val CODE_WEBVIEW_UNIVERSAL_ACCESS = "WEBVIEW_UNIVERSAL_ACCESS"
    }
}

/**
 * Weight of a security finding.
 *
 * [CRITICAL] removes 20 points, [HIGH] removes 10, [MEDIUM] removes 5, and
 * [LOW] removes 1.
 */
enum class SecuritySeverity {
    CRITICAL,
    HIGH,
    MEDIUM,
    LOW
}
