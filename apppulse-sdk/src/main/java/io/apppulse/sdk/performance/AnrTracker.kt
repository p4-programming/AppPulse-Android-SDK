package io.apppulse.sdk.performance

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import io.apppulse.sdk.core.AppPulseLogger
import io.apppulse.sdk.performance.model.AnrMetric
import io.apppulse.sdk.storage.repository.PerformanceRepository
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Detects a main-thread stall by posting a probe and waiting for it to run.
 *
 * If the probe is still pending after five seconds, an ANR row is stored with
 * the main-thread stack. The next probe starts only after the stalled one runs,
 * so one blockage produces one record.
 *
 * @param repository Destination for ANR rows.
 * @param scope Scope used to write rows off the watchdog thread.
 */
internal class AnrTracker(
    private val repository: PerformanceRepository,
    private val scope: CoroutineScope
) {

    private val mainHandler = Handler(Looper.getMainLooper())
    private val running = AtomicBoolean(false)
    private var worker: Thread? = null

    /**
     * Starts the watchdog thread. A second call while it is alive does nothing.
     */
    fun start() {
        if (!running.compareAndSet(false, true)) {
            return
        }
        val thread = Thread(::watch, "AppPulseAnr")
        thread.isDaemon = true
        worker = thread
        thread.start()
    }

    /**
     * Asks the watchdog to exit.
     */
    fun stop() {
        if (!running.compareAndSet(true, false)) {
            return
        }
        worker?.interrupt()
        worker = null
    }

    private fun watch() {
        while (running.get() && !Thread.currentThread().isInterrupted) {
            val completed = AtomicBoolean(false)
            val postedAt = SystemClock.elapsedRealtime()
            mainHandler.post { completed.set(true) }
            val deadline = postedAt + PerformanceLimits.ANR_BLOCKED_MS
            while (running.get() && !completed.get() && SystemClock.elapsedRealtime() < deadline) {
                if (!sleep(POLL_MS)) {
                    return
                }
            }
            if (!running.get()) {
                return
            }
            if (!completed.get()) {
                val blockedMs = SystemClock.elapsedRealtime() - postedAt
                report(blockedMs)
                while (running.get() && !completed.get()) {
                    if (!sleep(POLL_MS)) {
                        return
                    }
                }
            }
            if (!sleep(POLL_MS)) {
                return
            }
        }
    }

    private fun report(blockedMs: Long) {
        val stack = Looper.getMainLooper().thread.stackTrace
            .joinToString(separator = "\n")
            .take(MAX_STACK_CHARS)
        val metric = AnrMetric(
            timestamp = System.currentTimeMillis(),
            blockedDurationMs = blockedMs,
            stackTrace = stack,
            threadName = "main"
        )
        AppPulseLogger.e(TAG, "ANR Detected")
        scope.launch {
            try {
                repository.saveAnrMetric(metric)
            } catch (throwable: Throwable) {
                AppPulseLogger.e(TAG, "Failed to store ANR metric", throwable)
            }
        }
    }

    private fun sleep(millis: Long): Boolean {
        return try {
            Thread.sleep(millis)
            true
        } catch (interrupted: InterruptedException) {
            Thread.currentThread().interrupt()
            false
        }
    }

    private companion object {
        const val TAG = "AppPulsePerf"
        const val POLL_MS = 250L
        const val MAX_STACK_CHARS = 12_000
    }
}
