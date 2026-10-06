package io.apppulse.sdk.performance

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.os.SystemClock
import android.view.Choreographer
import io.apppulse.sdk.core.AppPulseLogger
import io.apppulse.sdk.performance.model.ScreenMetric
import io.apppulse.sdk.storage.repository.PerformanceRepository
import java.util.IdentityHashMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Records how long each host activity takes to draw its first frame.
 *
 * Open time is [Application.ActivityLifecycleCallbacks.onActivityStarted].
 * Fully drawn time is the next [Choreographer] frame after
 * [Application.ActivityLifecycleCallbacks.onActivityResumed].
 * A render longer than 1000 ms is stored as a slow screen.
 *
 * @param application Host application that receives the lifecycle callbacks.
 * @param repository Destination for screen rows.
 * @param scope Scope used to write rows off the main thread.
 * @param onForegroundChanged Called on the main thread when the first host
 * activity starts and when the last one stops. Used to pause FPS sampling.
 */
internal class ScreenTracker(
    private val application: Application,
    private val repository: PerformanceRepository,
    private val scope: CoroutineScope,
    private val onForegroundChanged: (Boolean) -> Unit
) : Application.ActivityLifecycleCallbacks {

    private val sessions = IdentityHashMap<Activity, Session>()
    private var registered = false

    /**
     * Registers lifecycle callbacks. Safe to call again after [stop].
     */
    fun start() {
        if (registered) {
            return
        }
        registered = true
        application.registerActivityLifecycleCallbacks(this)
    }

    /**
     * Removes callbacks and drops in-flight screen sessions.
     */
    fun stop() {
        if (!registered) {
            return
        }
        registered = false
        val hadForeground = sessions.isNotEmpty()
        sessions.clear()
        application.unregisterActivityLifecycleCallbacks(this)
        if (hadForeground) {
            onForegroundChanged(false)
        }
    }

    override fun onActivityStarted(activity: Activity) {
        if (!registered || !activity.shouldMeasure() || sessions.containsKey(activity)) {
            return
        }
        val wasBackground = sessions.isEmpty()
        sessions[activity] = Session(
            activityName = activity.javaClass.simpleName,
            openElapsed = SystemClock.elapsedRealtime(),
            openWallClock = System.currentTimeMillis()
        )
        if (wasBackground) {
            onForegroundChanged(true)
        }
    }

    override fun onActivityResumed(activity: Activity) {
        val session = sessions[activity] ?: return
        if (session.drawScheduled) {
            return
        }
        session.drawScheduled = true
        val name = session.activityName
        val openElapsed = session.openElapsed
        val openWallClock = session.openWallClock
        Choreographer.getInstance().postFrameCallback {
            val current = sessions[activity]
            if (current == null || current.openElapsed != openElapsed || current.measured) {
                return@postFrameCallback
            }
            current.measured = true
            val fullyDrawnElapsed = SystemClock.elapsedRealtime()
            val renderTimeMs = (fullyDrawnElapsed - openElapsed).coerceAtLeast(0L)
            store(
                ScreenMetric(
                    activityName = name,
                    openTimestamp = openWallClock,
                    fullyDrawnTimestamp = System.currentTimeMillis(),
                    renderTimeMs = renderTimeMs,
                    slowScreen = renderTimeMs > PerformanceLimits.SLOW_SCREEN_MS
                )
            )
        }
    }

    override fun onActivityStopped(activity: Activity) {
        if (sessions.remove(activity) != null && sessions.isEmpty()) {
            onForegroundChanged(false)
        }
    }

    override fun onActivityDestroyed(activity: Activity) {
        sessions.remove(activity)
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit

    override fun onActivityPaused(activity: Activity) = Unit

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit

    private fun store(metric: ScreenMetric) {
        scope.launch {
            try {
                repository.saveScreenMetric(metric)
                AppPulseLogger.i(
                    TAG,
                    "Screen Render: ${metric.activityName} ${metric.renderTimeMs} ms"
                )
                if (metric.slowScreen) {
                    AppPulseLogger.w(
                        TAG,
                        "Slow Screen: ${metric.activityName} ${metric.renderTimeMs} ms"
                    )
                }
            } catch (throwable: Throwable) {
                AppPulseLogger.e(TAG, "Failed to store screen metric", throwable)
            }
        }
    }

    private class Session(
        val activityName: String,
        val openElapsed: Long,
        val openWallClock: Long,
        var drawScheduled: Boolean = false,
        var measured: Boolean = false
    )

    private companion object {
        const val TAG = "AppPulsePerf"
    }
}
