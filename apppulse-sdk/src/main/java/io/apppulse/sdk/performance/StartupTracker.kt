package io.apppulse.sdk.performance

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.os.Process
import android.os.SystemClock
import android.view.Choreographer
import io.apppulse.sdk.core.AppPulseLogger
import io.apppulse.sdk.performance.model.StartupMetric
import io.apppulse.sdk.storage.repository.PerformanceRepository
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Measures cold start as the time from process creation to the first drawn
 * frame of a host activity.
 *
 * A start longer than 2000 ms is stored as a slow startup.
 *
 * @param application Host application. Callbacks are registered on it.
 * @param repository Destination for the measurement.
 * @param scope Scope used to write the row off the main thread.
 */
internal class StartupTracker(
    private val application: Application,
    private val repository: PerformanceRepository,
    private val scope: CoroutineScope
) : Application.ActivityLifecycleCallbacks {

    private val launchElapsedRealtime = Process.getStartElapsedRealtime()
    private val launchWallClock = System.currentTimeMillis() -
        (SystemClock.elapsedRealtime() - launchElapsedRealtime)
    private val recorded = AtomicBoolean(false)
    private val listening = AtomicBoolean(false)

    /**
     * Starts waiting for the first host activity frame.
     * A second call while already listening does nothing.
     */
    fun start() {
        if (!listening.compareAndSet(false, true)) {
            return
        }
        application.registerActivityLifecycleCallbacks(this)
    }

    /**
     * Stops waiting. A cold start already stored is kept.
     */
    fun stop() {
        if (!listening.compareAndSet(true, false)) {
            return
        }
        application.unregisterActivityLifecycleCallbacks(this)
    }

    override fun onActivityResumed(activity: Activity) {
        if (!listening.get() || recorded.get() || !activity.shouldMeasure()) {
            return
        }
        val activityName = activity.javaClass.simpleName
        Choreographer.getInstance().postFrameCallback {
            if (!listening.get() || !recorded.compareAndSet(false, true)) {
                return@postFrameCallback
            }
            stop()
            val nowElapsed = SystemClock.elapsedRealtime()
            val startupTimeMs = (nowElapsed - launchElapsedRealtime).coerceAtLeast(0L)
            val metric = StartupMetric(
                launchTimestamp = launchWallClock,
                firstActivityTimestamp = System.currentTimeMillis(),
                startupTimeMs = startupTimeMs,
                slowStartup = startupTimeMs > PerformanceLimits.SLOW_STARTUP_MS,
                activityName = activityName
            )
            store(metric)
        }
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit

    override fun onActivityStarted(activity: Activity) = Unit

    override fun onActivityPaused(activity: Activity) = Unit

    override fun onActivityStopped(activity: Activity) = Unit

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit

    override fun onActivityDestroyed(activity: Activity) = Unit

    private fun store(metric: StartupMetric) {
        scope.launch {
            try {
                repository.saveStartupMetric(metric)
                AppPulseLogger.i(TAG, "Startup Time: ${metric.startupTimeMs} ms")
                if (metric.slowStartup) {
                    AppPulseLogger.w(TAG, "Slow Startup: ${metric.activityName} ${metric.startupTimeMs} ms")
                }
            } catch (throwable: Throwable) {
                AppPulseLogger.e(TAG, "Failed to store startup metric", throwable)
            }
        }
    }

    private companion object {
        const val TAG = "AppPulsePerf"
    }
}
