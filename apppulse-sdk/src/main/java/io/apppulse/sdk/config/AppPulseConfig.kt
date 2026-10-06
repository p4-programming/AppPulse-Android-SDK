package io.apppulse.sdk.config

/**
 * Immutable configuration for an AppPulse session.
 *
 * Create instances with [Builder]. Feature flags default to enabled so a host
 * that calls [io.apppulse.sdk.AppPulse.init] without a config still gets local
 * diagnostics. Remote sync is skipped when [apiKey] is blank.
 *
 * @property apiKey Credential sent as `X-Api-Key`. Local trackers run when this is blank.
 * @property baseUrl Remote API origin. A trailing slash is added when missing.
 * @property debugMode Enables verbose and debug logs in [io.apppulse.sdk.core.AppPulseLogger].
 * @property enableCrashTracking Installs the AppPulse uncaught-exception handler.
 * @property enablePerformanceTracking Starts the performance module.
 * @property enableMemoryTracking Starts the memory module.
 * @property enableSecurityTracking Starts the security module.
 * @property enableAutoSync Schedules [io.apppulse.sdk.sync.SyncWorker] after [io.apppulse.sdk.AppPulse.init].
 * @property syncIntervalMinutes Period between automatic uploads. Values below 15 are raised to 15.
 */
class AppPulseConfig private constructor(
    val apiKey: String,
    val baseUrl: String,
    val debugMode: Boolean,
    val enableCrashTracking: Boolean,
    val enablePerformanceTracking: Boolean,
    val enableMemoryTracking: Boolean,
    val enableSecurityTracking: Boolean,
    val enableAutoSync: Boolean,
    val syncIntervalMinutes: Long
) {

    /**
     * Builds an [AppPulseConfig]. A builder instance is not thread-safe.
     */
    class Builder {
        private var apiKey: String = ""
        private var baseUrl: String = AppPulseOptions.DEFAULT_BASE_URL
        private var debugMode: Boolean = false
        private var enableCrashTracking: Boolean = true
        private var enablePerformanceTracking: Boolean = true
        private var enableMemoryTracking: Boolean = true
        private var enableSecurityTracking: Boolean = true
        private var enableAutoSync: Boolean = true
        private var syncIntervalMinutes: Long = AppPulseOptions.DEFAULT_SYNC_INTERVAL_MINUTES

        /**
         * Sets the backend API key.
         *
         * @param apiKey Key issued for the host application. Blank is allowed.
         */
        fun apiKey(apiKey: String) = apply {
            this.apiKey = apiKey
        }

        /**
         * Sets the remote API origin.
         *
         * @param baseUrl Absolute URL such as `https://api.example.com`.
         */
        fun baseUrl(baseUrl: String) = apply {
            this.baseUrl = baseUrl
        }

        /**
         * Sets whether AppPulse writes verbose and debug logs.
         *
         * @param enabled True to enable debug logging.
         */
        fun debugMode(enabled: Boolean) = apply {
            this.debugMode = enabled
        }

        /**
         * Sets whether crash tracking is installed during initialization.
         *
         * @param enabled True to install the uncaught-exception handler.
         */
        fun enableCrashTracking(enabled: Boolean) = apply {
            this.enableCrashTracking = enabled
        }

        /**
         * Sets whether performance tracking is started during initialization.
         *
         * @param enabled True to start [io.apppulse.sdk.performance.PerformanceTracker].
         */
        fun enablePerformanceTracking(enabled: Boolean) = apply {
            this.enablePerformanceTracking = enabled
        }

        /**
         * Sets whether memory tracking is started during initialization.
         *
         * @param enabled True to start [io.apppulse.sdk.memory.MemoryTracker].
         */
        fun enableMemoryTracking(enabled: Boolean) = apply {
            this.enableMemoryTracking = enabled
        }

        /**
         * Sets whether security checks run during initialization.
         *
         * @param enabled True to start [io.apppulse.sdk.security.SecurityScanner].
         */
        fun enableSecurityTracking(enabled: Boolean) = apply {
            this.enableSecurityTracking = enabled
        }

        /**
         * Sets whether automatic upload is scheduled during initialization.
         *
         * @param enabled True to run [io.apppulse.sdk.sync.SyncWorker] on the sync interval.
         */
        fun enableAutoSync(enabled: Boolean) = apply {
            this.enableAutoSync = enabled
        }

        /**
         * Sets how often automatic upload runs.
         *
         * WorkManager requires at least 15 minutes. Smaller values are raised.
         *
         * @param minutes Interval between sync attempts.
         */
        fun syncIntervalMinutes(minutes: Long) = apply {
            this.syncIntervalMinutes = minutes
        }

        /**
         * Creates the immutable configuration from the current builder values.
         */
        fun build(): AppPulseConfig {
            val interval = syncIntervalMinutes.coerceIn(
                AppPulseOptions.MIN_SYNC_INTERVAL_MINUTES,
                AppPulseOptions.MAX_SYNC_INTERVAL_MINUTES
            )
            return AppPulseConfig(
                apiKey = apiKey.trim(),
                baseUrl = AppPulseOptions.normalizeBaseUrl(baseUrl),
                debugMode = debugMode,
                enableCrashTracking = enableCrashTracking,
                enablePerformanceTracking = enablePerformanceTracking,
                enableMemoryTracking = enableMemoryTracking,
                enableSecurityTracking = enableSecurityTracking,
                enableAutoSync = enableAutoSync,
                syncIntervalMinutes = interval
            )
        }
    }

    /**
     * Returns a copy of the configuration with the API key redacted.
     */
    override fun toString(): String {
        return "AppPulseConfig(" +
            "apiKey=${redact(apiKey)}, " +
            "baseUrl=$baseUrl, " +
            "debugMode=$debugMode, " +
            "enableCrashTracking=$enableCrashTracking, " +
            "enablePerformanceTracking=$enablePerformanceTracking, " +
            "enableMemoryTracking=$enableMemoryTracking, " +
            "enableSecurityTracking=$enableSecurityTracking, " +
            "enableAutoSync=$enableAutoSync, " +
            "syncIntervalMinutes=$syncIntervalMinutes)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AppPulseConfig) return false
        return apiKey == other.apiKey &&
            baseUrl == other.baseUrl &&
            debugMode == other.debugMode &&
            enableCrashTracking == other.enableCrashTracking &&
            enablePerformanceTracking == other.enablePerformanceTracking &&
            enableMemoryTracking == other.enableMemoryTracking &&
            enableSecurityTracking == other.enableSecurityTracking &&
            enableAutoSync == other.enableAutoSync &&
            syncIntervalMinutes == other.syncIntervalMinutes
    }

    override fun hashCode(): Int {
        var result = apiKey.hashCode()
        result = 31 * result + baseUrl.hashCode()
        result = 31 * result + debugMode.hashCode()
        result = 31 * result + enableCrashTracking.hashCode()
        result = 31 * result + enablePerformanceTracking.hashCode()
        result = 31 * result + enableMemoryTracking.hashCode()
        result = 31 * result + enableSecurityTracking.hashCode()
        result = 31 * result + enableAutoSync.hashCode()
        result = 31 * result + syncIntervalMinutes.hashCode()
        return result
    }

    private companion object {
        fun redact(apiKey: String): String {
            if (apiKey.isEmpty()) return "(empty)"
            if (apiKey.length <= 4) return "****"
            return apiKey.take(4) + "…"
        }
    }
}
