package io.apppulse.sdk.security

import android.content.ComponentName
import android.content.pm.ApplicationInfo
import android.content.pm.ComponentInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.PermissionInfo
import android.content.pm.ProviderInfo
import android.os.Build
import io.apppulse.sdk.security.model.SecurityIssue
import io.apppulse.sdk.security.model.SecuritySeverity

/**
 * Reads exported components and application flags from the merged manifest.
 *
 * A component protected by a signature permission is recorded as
 * [SecuritySeverity.LOW]. An exported provider that does not protect writes
 * is [SecuritySeverity.CRITICAL].
 *
 * @param packageManager Host package manager.
 * @param packageName Application id being scanned.
 */
class ManifestScanner(
    private val packageManager: PackageManager,
    private val packageName: String
) {

    /**
     * Returns findings for exported activities, services, receivers, and providers.
     */
    fun scanComponents(info: PackageInfo): List<SecurityIssue> {
        val findings = ArrayList<SecurityIssue>()
        info.activities.orEmpty().filter { it.isComponentExported() && it.packageName == packageName }.forEach { activity ->
            findings += componentIssue(
                info = activity,
                permission = activity.permission,
                category = SecurityIssue.CATEGORY_MANIFEST,
                code = SecurityIssue.CODE_EXPORTED_ACTIVITY,
                title = "Exported activity",
                kind = "Activity",
                base = SecuritySeverity.HIGH
            )
        }
        info.services.orEmpty().filter { it.isComponentExported() && it.packageName == packageName }.forEach { service ->
            findings += componentIssue(
                info = service,
                permission = service.permission,
                category = SecurityIssue.CATEGORY_MANIFEST,
                code = SecurityIssue.CODE_EXPORTED_SERVICE,
                title = "Exported service",
                kind = "Service",
                base = SecuritySeverity.HIGH
            )
        }
        info.receivers.orEmpty().filter { it.isComponentExported() && it.packageName == packageName }.forEach { receiver ->
            findings += componentIssue(
                info = receiver,
                permission = receiver.permission,
                category = SecurityIssue.CATEGORY_MANIFEST,
                code = SecurityIssue.CODE_EXPORTED_RECEIVER,
                title = "Exported receiver",
                kind = "Receiver",
                base = SecuritySeverity.MEDIUM
            )
        }
        info.providers.orEmpty().filter { it.isComponentExported() && it.packageName == packageName }.forEach { provider ->
            findings += providerIssue(provider)
        }
        return findings
    }

    /**
     * Returns findings for `android:debuggable` and `android:allowBackup`.
     */
    fun scanConfiguration(applicationInfo: ApplicationInfo): List<SecurityIssue> {
        val findings = ArrayList<SecurityIssue>(2)
        if (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) {
            findings += SecurityIssue(
                category = SecurityIssue.CATEGORY_APPLICATION,
                code = SecurityIssue.CODE_DEBUGGABLE,
                title = "Debuggable application",
                description = "android:debuggable is true. A debugger can attach to this process.",
                severity = SecuritySeverity.CRITICAL,
                target = packageName,
                timestamp = 0L
            )
        }
        if (applicationInfo.flags and ApplicationInfo.FLAG_ALLOW_BACKUP != 0) {
            findings += SecurityIssue(
                category = SecurityIssue.CATEGORY_APPLICATION,
                code = SecurityIssue.CODE_ALLOW_BACKUP,
                title = "Backup allowed",
                description = "android:allowBackup is true. App data can be extracted through backup.",
                severity = SecuritySeverity.MEDIUM,
                target = packageName,
                timestamp = 0L
            )
        }
        return findings
    }

    private fun componentIssue(
        info: ComponentInfo,
        permission: String?,
        category: String,
        code: String,
        title: String,
        kind: String,
        base: SecuritySeverity
    ): SecurityIssue {
        val protected = isSignatureProtected(permission)
        val enabled = if (info.enabled) "" else " It is currently disabled."
        val permissionNote = permission?.let { " Permission: $it." }.orEmpty()
        return SecurityIssue(
            category = category,
            code = code,
            title = title,
            description = "$kind ${displayName(info)} is exported.$permissionNote$enabled",
            severity = if (protected) SecuritySeverity.LOW else base,
            target = displayName(info),
            timestamp = 0L
        )
    }

    private fun providerIssue(info: ProviderInfo): SecurityIssue {
        val read = info.readPermission
        val write = info.writePermission
        val severity = when {
            read == null && write == null -> SecuritySeverity.CRITICAL
            isSignatureProtected(read) && isSignatureProtected(write) -> SecuritySeverity.LOW
            else -> SecuritySeverity.HIGH
        }
        val enabled = if (info.enabled) "" else " It is currently disabled."
        return SecurityIssue(
            category = SecurityIssue.CATEGORY_MANIFEST,
            code = SecurityIssue.CODE_EXPORTED_PROVIDER,
            title = "Exported provider",
            description = "Provider ${displayName(info)} is exported. " +
                "read=${read ?: "none"}, write=${write ?: "none"}.$enabled",
            severity = severity,
            target = displayName(info),
            timestamp = 0L
        )
    }

    private fun displayName(info: ComponentInfo): String {
        val name = info.name
        if (name.startsWith(".")) {
            return packageName + name
        }
        return name.ifBlank { ComponentName(info.packageName, info.name).className }
    }

    private fun ComponentInfo.isComponentExported(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return exported
        }
        return try {
            val field = ComponentInfo::class.java.getField("exported")
            field.getBoolean(this)
        } catch (throwable: Throwable) {
            false
        }
    }

    private fun isSignatureProtected(permission: String?): Boolean {
        if (permission.isNullOrBlank()) {
            return false
        }
        return try {
            val info = permissionInfo(permission)
            val base = info.protectionLevel and PermissionInfo.PROTECTION_MASK_BASE
            base == PermissionInfo.PROTECTION_SIGNATURE ||
                base == PermissionInfo.PROTECTION_SIGNATURE_OR_SYSTEM
        } catch (ignored: PackageManager.NameNotFoundException) {
            false
        }
    }

    @Suppress("DEPRECATION")
    private fun permissionInfo(permission: String): PermissionInfo {
        return packageManager.getPermissionInfo(permission, 0)
    }
}
