package io.apppulse.sdk.utils

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import io.apppulse.sdk.core.AppPulseLogger

/**
 * Reads Android and host-application version strings for diagnostic events.
 */
object VersionUtils {

    /**
     * Returns the Android release name and API level, for example `14 (API 34)`.
     */
    fun androidVersion(): String {
        val release = Build.VERSION.RELEASE?.trim().orEmpty().ifEmpty { "unknown" }
        return "$release (API ${Build.VERSION.SDK_INT})"
    }

    /**
     * Returns the host application's `versionName`.
     *
     * @param context Context whose package is the host application.
     * @return The version name, or `unknown` when the package manager cannot read it.
     */
    fun appVersion(context: Context): String {
        return try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.PackageInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            packageInfo.versionName?.trim()?.ifEmpty { UNKNOWN } ?: UNKNOWN
        } catch (exception: PackageManager.NameNotFoundException) {
            AppPulseLogger.w(TAG, "Unable to read application version name", exception)
            UNKNOWN
        }
    }

    private const val TAG = "AppPulseVersion"
    private const val UNKNOWN = "unknown"
}
