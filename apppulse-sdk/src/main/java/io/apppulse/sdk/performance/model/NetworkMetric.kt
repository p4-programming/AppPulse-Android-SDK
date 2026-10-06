package io.apppulse.sdk.performance.model

/**
 * One HTTP call observed by [io.apppulse.sdk.performance.AppPulseOkHttpInterceptor].
 *
 * @property id Stored row id. `0` before insert.
 * @property url Request URL without the query string.
 * @property method HTTP method.
 * @property responseCode HTTP status, or `0` when the call failed before a response.
 * @property requestSize Request body size in bytes, or `0` when unknown.
 * @property responseSize Response body size in bytes, or `0` when unknown.
 * @property durationMs Time spent in the interceptor chain.
 * @property slowApi True when [durationMs] is greater than 3000.
 * @property timestamp Wall-clock time when the call finished.
 */
data class NetworkMetric(
    val id: Long = 0,
    val url: String,
    val method: String,
    val responseCode: Int,
    val requestSize: Long,
    val responseSize: Long,
    val durationMs: Long,
    val slowApi: Boolean,
    val timestamp: Long
) {

    /**
     * Returns a short label from the URL path, such as `login`.
     */
    fun apiLabel(): String {
        val withoutQuery = url.substringBefore('?')
        val path = withoutQuery.substringAfter("://").substringAfter('/', missingDelimiterValue = "")
        val trimmed = path.trim('/')
        if (trimmed.isEmpty()) {
            return withoutQuery.substringAfter("://").substringBefore('/').ifBlank { withoutQuery }
        }
        return trimmed.substringAfterLast('/').ifBlank { trimmed }
    }
}
