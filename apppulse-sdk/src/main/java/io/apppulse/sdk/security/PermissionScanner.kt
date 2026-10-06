package io.apppulse.sdk.security

import android.Manifest
import android.content.pm.PackageInfo
import io.apppulse.sdk.security.model.SecurityIssue
import io.apppulse.sdk.security.model.SecuritySeverity

/**
 * Flags sensitive permissions declared by the host application.
 *
 * SMS permissions are [SecuritySeverity.CRITICAL]. Contacts, microphone, and
 * fine location are [SecuritySeverity.HIGH]. The check uses the manifest
 * declaration, whether or not the user has granted the permission.
 */
class PermissionScanner {

    /**
     * Returns one finding for each risky permission in [info].
     */
    fun scan(info: PackageInfo): List<SecurityIssue> {
        val requested = info.requestedPermissions.orEmpty().toSet()
        return RISKY.mapNotNull { (permission, severity) ->
            if (permission !in requested) {
                null
            } else {
                SecurityIssue(
                    category = SecurityIssue.CATEGORY_PERMISSION,
                    code = SecurityIssue.CODE_RISKY_PERMISSION,
                    title = "Risky permission",
                    description = "The manifest requests $permission.",
                    severity = severity,
                    target = permission,
                    timestamp = 0L
                )
            }
        }
    }

    private companion object {
        val RISKY = listOf(
            Manifest.permission.READ_SMS to SecuritySeverity.CRITICAL,
            Manifest.permission.RECEIVE_SMS to SecuritySeverity.CRITICAL,
            Manifest.permission.SEND_SMS to SecuritySeverity.CRITICAL,
            Manifest.permission.READ_CONTACTS to SecuritySeverity.HIGH,
            Manifest.permission.WRITE_CONTACTS to SecuritySeverity.HIGH,
            Manifest.permission.RECORD_AUDIO to SecuritySeverity.HIGH,
            Manifest.permission.ACCESS_FINE_LOCATION to SecuritySeverity.HIGH
        )
    }
}
