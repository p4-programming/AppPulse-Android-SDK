package io.apppulse.sdk.performance

import io.apppulse.sdk.AppPulse
import io.apppulse.sdk.core.AppPulseLogger
import io.apppulse.sdk.performance.model.NetworkMetric
import java.io.IOException
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import okhttp3.ResponseBody

/**
 * OkHttp interceptor that records method, URL, status, sizes, and duration.
 *
 * Add it to the application client:
 * ```
 * OkHttpClient.Builder()
 *     .addInterceptor(AppPulseOkHttpInterceptor())
 *     .build()
 * ```
 *
 * Query strings are removed before the URL is stored. A failure inside
 * AppPulse does not fail the HTTP call. Calls are stored only while
 * [AppPulse.startPerformanceMonitoring] is active.
 * A call longer than 3000 ms is flagged as a slow API.
 */
class AppPulseOkHttpInterceptor : Interceptor {

    /**
     * Times [chain] and forwards the result to AppPulse.
     *
     * @throws IOException when the downstream call fails. The failure is
     * recorded with response code `0` and then rethrown.
     */
    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val requestSize = bodySize(request.body)
        val started = System.nanoTime()
        return try {
            val response = chain.proceed(request)
            record(
                request = request,
                responseCode = response.code,
                requestSize = requestSize,
                responseSize = bodySize(response.body),
                startedNanos = started
            )
            response
        } catch (error: IOException) {
            record(
                request = request,
                responseCode = 0,
                requestSize = requestSize,
                responseSize = 0L,
                startedNanos = started
            )
            throw error
        }
    }

    private fun record(
        request: Request,
        responseCode: Int,
        requestSize: Long,
        responseSize: Long,
        startedNanos: Long
    ) {
        try {
            val durationMs = ((System.nanoTime() - startedNanos) / 1_000_000L).coerceAtLeast(0L)
            val url = request.url.newBuilder().query(null).fragment(null).build().toString()
            AppPulse.recordNetworkCall(
                NetworkMetric(
                    url = url,
                    method = request.method,
                    responseCode = responseCode,
                    requestSize = requestSize,
                    responseSize = responseSize,
                    durationMs = durationMs,
                    slowApi = durationMs > PerformanceLimits.SLOW_API_MS,
                    timestamp = System.currentTimeMillis()
                )
            )
        } catch (throwable: Throwable) {
            AppPulseLogger.e(TAG, "Failed to record API timing", throwable)
        }
    }

    private fun bodySize(body: RequestBody?): Long {
        if (body == null) {
            return 0L
        }
        return try {
            body.contentLength().coerceAtLeast(0L)
        } catch (exception: IOException) {
            0L
        }
    }

    private fun bodySize(body: ResponseBody?): Long {
        if (body == null) {
            return 0L
        }
        return try {
            body.contentLength().coerceAtLeast(0L)
        } catch (exception: IOException) {
            0L
        }
    }

    private companion object {
        const val TAG = "AppPulsePerf"
    }
}
