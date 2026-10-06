package io.apppulse.sdk.memory

import android.app.Application
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import androidx.fragment.app.Fragment
import io.apppulse.sdk.core.AppPulseLogger
import io.apppulse.sdk.memory.model.MemoryHealthReport
import io.apppulse.sdk.memory.model.MemoryMetric
import io.apppulse.sdk.storage.repository.MemoryRepository
import java.util.ArrayDeque
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Coordinates heap sampling, leak checks, and bitmap tracking.
 *
 * [start] registers lifecycle callbacks on the main thread and samples the heap
 * every 30 seconds. [healthReport] is updated after each sample and each leak.
 *
 * @param application Host application.
 * @param repository Storage for memory, leak, and bitmap rows.
 * @param runtime Runtime used for Java heap counters.
 */
class MemoryTracker(
    private val application: Application,
    private val repository: MemoryRepository,
    private val runtime: Runtime = Runtime.getRuntime()
) {

    private val mainHandler = Handler(Looper.getMainLooper())
    private val started = AtomicBoolean(false)
    private val leakCount = AtomicInteger(0)
    private val recentUsedMb = ArrayDeque<Double>()
    private val stateLock = Any()

    @Volatile
    private var scannedLargeCount = 0

    @Volatile
    private var latestReport: MemoryHealthReport

    private val scope = CoroutineScope(
        SupervisorJob() + Dispatchers.Default + CoroutineExceptionHandler { _, throwable ->
            AppPulseLogger.e(TAG, "Memory coroutine failed", throwable)
        }
    )
    private val calculator = MemoryHealthCalculator()
    private val bitmapTracker = BitmapTracker(repository, scope)
    private val leakDetector = LeakDetector(repository, scope, ::onLeakFound)
    private val activityTracker = ActivityLeakTracker(
        application = application,
        detector = leakDetector,
        fragmentTracker = FragmentLeakTracker(leakDetector),
        activityStarted = bitmapTracker::addActivity,
        activityStopped = bitmapTracker::removeActivity
    )
    private val heapMonitor = HeapMonitor(runtime, scope, ::handleSample)
    private val samples = MemorySamples(bitmapTracker)

    init {
        val metric = heapMonitor.read()
        remember(metric)
        latestReport = buildReport(metric)
    }

    /**
     * Logs the current heap. Monitoring begins at [start].
     */
    fun initialize() {
        val used = latestReport.usedMemoryMb.roundToInt()
        AppPulseLogger.i(TAG, "Memory tracking initialized. Memory Usage = $used MB")
    }

    /**
     * Starts heap sampling, leak watches, and bitmap scans.
     * Does nothing when monitoring is already running.
     */
    fun start() {
        if (!started.compareAndSet(false, true)) {
            AppPulseLogger.i(TAG, "Memory monitoring is already running")
            return
        }
        runOnMain {
            if (!started.get()) {
                return@runOnMain
            }
            activityTracker.start()
            heapMonitor.start()
            scope.launch { refreshStoredLeakCount() }
            AppPulseLogger.i(TAG, "Memory monitoring started")
        }
    }

    /**
     * Stops sampling and lifecycle watches. Stored rows are kept.
     * A leak check already waiting is allowed to finish.
     */
    fun stop() {
        if (!started.compareAndSet(true, false)) {
            AppPulseLogger.i(TAG, "Memory monitoring is not running")
            return
        }
        heapMonitor.stop()
        runOnMain {
            activityTracker.stop()
            AppPulseLogger.i(TAG, "Memory monitoring stopped")
        }
    }

    /**
     * Returns the latest health score, from 0 to 100.
     */
    fun healthScore(): Int = latestReport.score

    /**
     * Returns the latest full health report.
     */
    fun healthReport(): MemoryHealthReport = latestReport

    /**
     * Retains [activity] so a later destroy is reported as a leak candidate.
     */
    fun simulateActivityLeak(activity: android.app.Activity) {
        samples.leakActivity(activity)
    }

    /**
     * Retains [fragment] so a later destroy is reported as a leak candidate.
     */
    fun simulateFragmentLeak(fragment: Fragment) {
        samples.leakFragment(fragment)
    }

    /**
     * Allocates a bitmap larger than 5 MB and records it.
     */
    fun simulateLargeBitmap(): Bitmap {
        return samples.createLargeBitmap()
    }

    /**
     * Releases objects retained by the validation methods.
     */
    fun clearSimulatedLeaks() {
        samples.clear()
        publishHealth(heapMonitor.read())
    }

    private fun handleSample(metric: MemoryMetric) {
        remember(metric)
        AppPulseLogger.i(TAG, "Memory Usage = ${metric.usedMemoryMb.roundToInt()} MB")
        scope.launch {
            try {
                repository.saveMemoryMetric(metric)
            } catch (throwable: Throwable) {
                AppPulseLogger.e(TAG, "Failed to store memory metric", throwable)
            }
        }
        bitmapTracker.scan { snapshot ->
            scannedLargeCount = snapshot.largeCount
            publishHealth(metric)
        }
        publishHealth(metric)
    }

    private fun onLeakFound() {
        leakCount.incrementAndGet()
        publishHealth(heapMonitor.read())
    }

    private suspend fun refreshStoredLeakCount() {
        try {
            val stored = repository.leakCount()
            leakCount.updateAndGet { current -> maxOf(current, stored) }
            publishHealth(heapMonitor.read())
        } catch (throwable: Throwable) {
            AppPulseLogger.e(TAG, "Failed to read leak count", throwable)
        }
    }

    private fun publishHealth(metric: MemoryMetric) {
        latestReport = buildReport(metric)
        AppPulseLogger.i(TAG, "Memory Score = ${latestReport.score}")
    }

    private fun buildReport(metric: MemoryMetric): MemoryHealthReport {
        val recent = synchronized(stateLock) { recentUsedMb.toList() }
        return calculator.calculate(
            usedMemoryMb = metric.usedMemoryMb,
            maxMemoryMb = metric.maxMemoryMb,
            leakCount = leakCount.get(),
            largeBitmapCount = scannedLargeCount + bitmapTracker.explicitLargeCount,
            recentUsedMb = recent
        )
    }

    private fun remember(metric: MemoryMetric) {
        synchronized(stateLock) {
            recentUsedMb.addLast(metric.usedMemoryMb)
            while (recentUsedMb.size > TREND_WINDOW) {
                recentUsedMb.removeFirst()
            }
        }
    }

    private fun runOnMain(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            block()
        } else {
            mainHandler.post(block)
        }
    }

    private companion object {
        const val TAG = "AppPulseMem"
        const val TREND_WINDOW = 6
    }
}
