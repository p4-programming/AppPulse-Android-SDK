package io.apppulse.sdk.performance

import io.apppulse.sdk.core.AppPulseLogger
import io.apppulse.sdk.performance.model.NetworkMetric
import io.apppulse.sdk.storage.repository.PerformanceRepository
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Stores HTTP timings captured by [AppPulseOkHttpInterceptor].
 *
 * Recording follows [start] and [stop] so a stopped session does not keep
 * writing rows.
 *
 * @param repository Destination for network rows.
 * @param scope Scope used to write rows off the calling thread.
 */
internal class NetworkTracker(
    private val repository: PerformanceRepository,
    private val scope: CoroutineScope
) {

    private val enabled = AtomicBoolean(false)

    /**
     * Allows [record] to persist calls.
     */
    fun start() {
        enabled.set(true)
    }

    /**
     * Ignores later calls until [start].
     */
    fun stop() {
        enabled.set(false)
    }

    /**
     * Persists [metric] when monitoring is running.
     * Failures are logged and do not propagate to the HTTP caller.
     */
    fun record(metric: NetworkMetric) {
        if (!enabled.get()) {
            return
        }
        val label = metric.apiLabel()
        AppPulseLogger.i(TAG, "API Duration: $label ${metric.durationMs} ms")
        if (metric.slowApi) {
            AppPulseLogger.w(TAG, "Slow API: $label ${metric.durationMs} ms")
        }
        scope.launch {
            try {
                repository.saveNetworkMetric(metric)
            } catch (throwable: Throwable) {
                AppPulseLogger.e(TAG, "Failed to store network metric", throwable)
            }
        }
    }

    private companion object {
        const val TAG = "AppPulsePerf"
    }
}
