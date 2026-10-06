package io.apppulse.sdk.performance

import android.os.Handler
import android.os.Looper
import android.view.Choreographer
import io.apppulse.sdk.core.AppPulseLogger
import io.apppulse.sdk.performance.model.FpsMetric
import io.apppulse.sdk.storage.repository.PerformanceRepository
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Samples frames with [Choreographer] while a host activity is in the foreground.
 *
 * Each completed one-second window stores FPS, dropped frames, and the running
 * average. A window below 45 FPS is flagged as a performance issue.
 *
 * @param repository Destination for FPS rows.
 * @param scope Scope used to write rows off the main thread.
 */
internal class FpsTracker(
    private val repository: PerformanceRepository,
    private val scope: CoroutineScope
) {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var running = false
    private var foreground = false
    private var scheduled = false
    private var windowStartNanos = 0L
    private var lastFrameNanos = 0L
    private var frames = 0
    private var droppedFrames = 0
    private var fpsTotal = 0.0
    private var sampleCount = 0

    private val frameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            scheduled = false
            if (!running || !foreground) {
                return
            }
            if (windowStartNanos == 0L) {
                windowStartNanos = frameTimeNanos
                lastFrameNanos = frameTimeNanos
                scheduleNext()
                return
            }
            val delta = frameTimeNanos - lastFrameNanos
            if (delta > 0) {
                frames += 1
                val missed = (delta / PerformanceLimits.FRAME_BUDGET_NANOS).toInt() - 1
                if (missed > 0) {
                    droppedFrames += missed
                }
            }
            lastFrameNanos = frameTimeNanos
            val elapsed = frameTimeNanos - windowStartNanos
            if (elapsed >= ONE_SECOND_NANOS && frames > 0) {
                val fps = frames / (elapsed / ONE_SECOND_NANOS.toDouble())
                publish(fps, droppedFrames)
                frames = 0
                droppedFrames = 0
                windowStartNanos = frameTimeNanos
            }
            scheduleNext()
        }
    }

    /**
     * Arms sampling. Frames are collected only while the app is in the foreground.
     */
    fun start() {
        mainHandler.post {
            running = true
            if (foreground) {
                resetWindow()
                scheduleNext()
            }
        }
    }

    /**
     * Stops frame callbacks. Samples already stored are kept.
     */
    fun stop() {
        mainHandler.post {
            running = false
            resetWindow()
            removeCallback()
        }
    }

    /**
     * Pauses or resumes frame callbacks as host activities start and stop.
     *
     * @param inForeground True when at least one measured activity is started.
     */
    fun onForegroundChanged(inForeground: Boolean) {
        mainHandler.post {
            foreground = inForeground
            if (!running) {
                return@post
            }
            if (inForeground) {
                resetWindow()
                scheduleNext()
            } else {
                resetWindow()
                removeCallback()
            }
        }
    }

    private fun scheduleNext() {
        if (scheduled || !running || !foreground) {
            return
        }
        scheduled = true
        Choreographer.getInstance().postFrameCallback(frameCallback)
    }

    private fun removeCallback() {
        if (!scheduled) {
            return
        }
        Choreographer.getInstance().removeFrameCallback(frameCallback)
        scheduled = false
    }

    private fun resetWindow() {
        windowStartNanos = 0L
        lastFrameNanos = 0L
        frames = 0
        droppedFrames = 0
    }

    private fun publish(fps: Double, dropped: Int) {
        sampleCount += 1
        fpsTotal += fps
        val average = fpsTotal / sampleCount
        val metric = FpsMetric(
            timestamp = System.currentTimeMillis(),
            fps = fps,
            droppedFrames = dropped,
            averageFps = average,
            performanceIssue = fps < PerformanceLimits.LOW_FPS
        )
        val rounded = fps.roundToInt()
        if (metric.performanceIssue) {
            AppPulseLogger.w(TAG, "FPS: $rounded")
        } else {
            AppPulseLogger.i(TAG, "FPS: $rounded")
        }
        scope.launch {
            try {
                repository.saveFpsMetric(metric)
            } catch (throwable: Throwable) {
                AppPulseLogger.e(TAG, "Failed to store FPS metric", throwable)
            }
        }
    }

    private companion object {
        const val TAG = "AppPulsePerf"
        const val ONE_SECOND_NANOS = 1_000_000_000L
    }
}
