package io.apppulse.sdk.model

/**
 * Snapshot of the device and host application identity attached to diagnostic events.
 *
 * @property manufacturer Device manufacturer reported by the platform.
 * @property model Device model reported by the platform.
 * @property androidVersion Android release name and API level.
 * @property appVersion Host application version name.
 */
data class DeviceInfo(
    val manufacturer: String,
    val model: String,
    val androidVersion: String,
    val appVersion: String
)
