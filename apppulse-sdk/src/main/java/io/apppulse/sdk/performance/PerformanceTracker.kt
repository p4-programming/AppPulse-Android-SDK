package io.apppulse.sdk.performance

import android.app.Application
import android.os.Handler
import android.os.Looper
import io.apppulse.sdk.core.AppPulseLogger
import io.apppulse.sdk.performance.model.NetworkMetric
import io.apppulse.sdk.storage.repository.PerformanceRepository
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Coordinates startup, screen, network, FPS, and ANR collection.
 *
 * [start] and [stop] are safe to call from any thread. Lifecycle and frame
 * work is posted to the main looper. Rows are written on a background scope
 * owned by this tracker for the process lifetime.
 *
 * @param application Host application.
 * @param repository Storage for every performance metric.
 */
class PerformanceTracker(
    application: Application,
    repository: PerformanceRepository
) {

    private val mainHandler = Handler(Looper.getMainLooper())
    private val started = AtomicBoolean(false)
    private val scope = CoroutineScope(
        SupervisorJob() + Dispatchers.IO + CoroutineExceptionHandler { _, throwable ->
            AppPulseLogger.e(TAG, "Performance coroutine failed", throwable)
        }
    )
    private val fpsTracker = FpsTracker(repository, scope)
    private val startupTracker = StartupTracker(application, repository, scope)
    private val screenTracker = ScreenTracker(
        application = application,
        repository = repository,
        scope = scope,
        onForegroundChanged = fpsTracker::onForegroundChanged
    )
    private val networkTracker = NetworkTracker(repository, scope)
    private val anrTracker = AnrTracker(repository, scope)

    /**
     * Logs that the performance module is ready. Monitoring begins at [start].
     */
    fun initialize() {
        AppPulseLogger.i(TAG, "Performance tracking initialized")
    }

    /**
     * Starts cold-start, screen, FPS, ANR, and network collection.
     * Does nothing when monitoring is already running.
     */
    fun start() {
        if (!started.compareAndSet(false, true)) {
            AppPulseLogger.i(TAG, "Performance monitoring is already running")
            return
        }
        runOnMain {
            if (!started.get()) {
                return@runOnMain
            }
            networkTracker.start()
            startupTracker.start()
            screenTracker.start()
            fpsTracker.start()
            anrTracker.start()
            AppPulseLogger.i(TAG, "Performance monitoring started")
        }
    }

    /**
     * Stops collectors. Stored metrics are kept.
     * Does nothing when monitoring is not running.
     */
    fun stop() {
        if (!started.compareAndSet(true, false)) {
            AppPulseLogger.i(TAG, "Performance monitoring is not running")
            return
        }
        networkTracker.stop()
        runOnMain {
            startupTracker.stop()
            screenTracker.stop()
            fpsTracker.stop()
            anrTracker.stop()
            AppPulseLogger.i(TAG, "Performance monitoring stopped")
        }
    }

    private fun runOnMain(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            block()
        } else {
            mainHandler.post(block)
        }
    }

    /**
     * Records one HTTP call when monitoring is running.
     */
    internal fun recordNetwork(metric: NetworkMetric) {
        networkTracker.record(metric)
    }

    private companion object {
        const val TAG = "AppPulsePerf"
    }
}
