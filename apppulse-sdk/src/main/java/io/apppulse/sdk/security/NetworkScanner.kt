package io.apppulse.sdk.security

import android.security.NetworkSecurityPolicy
import io.apppulse.sdk.security.model.SecurityIssue
import io.apppulse.sdk.security.model.SecuritySeverity
import io.apppulse.sdk.storage.dao.NetworkDao

/**
 * Checks cleartext policy and HTTP URLs already stored by performance monitoring.
 *
 * @param networkDao Source of recorded request URLs.
 */
class NetworkScanner(
    private val networkDao: NetworkDao
) {

    /**
     * Returns cleartext and HTTP findings.
     *
     * Cleartext is read from [NetworkSecurityPolicy], which honors
     * `android:usesCleartextTraffic` and the network security config.
     */
    suspend fun scan(): List<SecurityIssue> {
        val findings = ArrayList<SecurityIssue>(2)
        if (NetworkSecurityPolicy.getInstance().isCleartextTrafficPermitted) {
            findings += SecurityIssue(
                category = SecurityIssue.CATEGORY_NETWORK,
                code = SecurityIssue.CODE_CLEARTEXT,
                title = "Cleartext traffic enabled",
                description = "The network security policy permits cleartext HTTP.",
                severity = SecuritySeverity.HIGH,
                target = "usesCleartextTraffic",
                timestamp = 0L
            )
        }
        val urls = networkDao.httpUrls(URL_LIMIT).distinct()
        if (urls.isNotEmpty()) {
            val sample = urls.take(SAMPLE_LIMIT).joinToString(separator = "\n")
            findings += SecurityIssue(
                category = SecurityIssue.CATEGORY_NETWORK,
                code = SecurityIssue.CODE_HTTP_URL,
                title = "HTTP URL found",
                description = "Cleartext HTTP URLs were recorded:\n$sample",
                severity = SecuritySeverity.HIGH,
                target = "${urls.size} url(s)",
                timestamp = 0L
            )
        }
        return findings
    }

    private companion object {
        const val URL_LIMIT = 50
        const val SAMPLE_LIMIT = 8
    }
}
