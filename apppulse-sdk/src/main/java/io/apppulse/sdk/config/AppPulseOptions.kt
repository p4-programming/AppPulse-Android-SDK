package io.apppulse.sdk.config

/**
 * Runtime settings for remote sync.
 *
 * [autoSyncEnabled] and [syncIntervalMinutes] can change after
 * [io.apppulse.sdk.AppPulse.init] through [io.apppulse.sdk.AppPulse.enableAutoSync]
 * and [io.apppulse.sdk.AppPulse.disableAutoSync]. The other values stay as they
 * were captured from [AppPulseConfig].
 */
class AppPulseOptions internal constructor(
    val apiKey: String,
    val baseUrl: String,
    val debugMode: Boolean,
    val enableCrashTracking: Boolean,
    val enablePerformanceTracking: Boolean,
    val enableMemoryTracking: Boolean,
    val enableSecurityTracking: Boolean,
    autoSyncEnabled: Boolean,
    syncIntervalMinutes: Long
) {

    /**
     * When true, [io.apppulse.sdk.sync.SyncWorker] is allowed to upload.
     */
    @Volatile
    var autoSyncEnabled: Boolean = autoSyncEnabled
        internal set

    /**
     * Minutes between automatic uploads. Never below [MIN_SYNC_INTERVAL_MINUTES].
     */
    @Volatile
    var syncIntervalMinutes: Long = syncIntervalMinutes
        internal set

    /**
     * True when an API key is present and uploads can be attempted.
     */
    fun canSync(): Boolean = apiKey.isNotBlank() && baseUrl.isNotBlank()

    companion object {
        /**
         * Origin used when the host does not set [AppPulseConfig.Builder.baseUrl].
         */
        const val DEFAULT_BASE_URL = "https://api.apppulse.io/"

        /**
         * Smallest periodic interval WorkManager accepts.
         */
        const val MIN_SYNC_INTERVAL_MINUTES = 15L

        /**
         * Largest interval accepted by [AppPulseConfig.Builder.syncIntervalMinutes].
         */
        const val MAX_SYNC_INTERVAL_MINUTES = 24L * 60L

        /**
         * Interval used when the host does not set one.
         */
        const val DEFAULT_SYNC_INTERVAL_MINUTES = 30L

        /**
         * Copies [config] into runtime options.
         */
        fun from(config: AppPulseConfig): AppPulseOptions {
            return AppPulseOptions(
                apiKey = config.apiKey,
                baseUrl = config.baseUrl,
                debugMode = config.debugMode,
                enableCrashTracking = config.enableCrashTracking,
                enablePerformanceTracking = config.enablePerformanceTracking,
                enableMemoryTracking = config.enableMemoryTracking,
                enableSecurityTracking = config.enableSecurityTracking,
                autoSyncEnabled = config.enableAutoSync,
                syncIntervalMinutes = config.syncIntervalMinutes
            )
        }

        /**
         * Returns [url] with a trailing slash so Retrofit can resolve relative paths.
         */
        fun normalizeBaseUrl(url: String): String {
            val trimmed = url.trim().ifBlank { DEFAULT_BASE_URL }
            return if (trimmed.endsWith("/")) trimmed else "$trimmed/"
        }
    }
}
