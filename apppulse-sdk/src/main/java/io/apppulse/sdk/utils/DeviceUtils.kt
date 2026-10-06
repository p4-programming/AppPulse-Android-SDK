package io.apppulse.sdk.utils

import android.content.Context
import android.os.Build
import io.apppulse.sdk.model.DeviceInfo

/**
 * Reads stable device identity fields used by AppPulse diagnostics.
 *
 * Methods return a non-blank value. When the platform reports an empty field,
 * the result is [UNKNOWN].
 */
object DeviceUtils {

    /**
     * Returns the device manufacturer, such as the value of [Build.MANUFACTURER].
     */
    fun manufacturer(): String = normalize(Build.MANUFACTURER)

    /**
     * Returns the device model, such as the value of [Build.MODEL].
     */
    fun model(): String = normalize(Build.MODEL)

    /**
     * Collects manufacturer, model, Android version, and the host app version.
     *
     * @param context Used to resolve the host application version.
     */
    fun deviceInfo(context: Context): DeviceInfo {
        return DeviceInfo(
            manufacturer = manufacturer(),
            model = model(),
            androidVersion = VersionUtils.androidVersion(),
            appVersion = VersionUtils.appVersion(context)
        )
    }

    private fun normalize(value: String?): String {
        return value?.trim().orEmpty().ifEmpty { UNKNOWN }
    }

    private const val UNKNOWN = "unknown"
}
