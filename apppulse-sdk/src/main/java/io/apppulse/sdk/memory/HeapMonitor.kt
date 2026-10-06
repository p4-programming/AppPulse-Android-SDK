package io.apppulse.sdk.memory

import android.os.Debug
import io.apppulse.sdk.memory.model.MemoryMetric
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Reads Java and native heap sizes and repeats the read every 30 seconds.
 *
 * @param runtime Runtime that supplies the Java heap counters.
 * @param scope Scope that owns the sampling loop.
 * @param onSample Called with each sample, including the first one from [start].
 */
internal class HeapMonitor(
    private val runtime: Runtime,
    private val scope: CoroutineScope,
    private val onSample: (MemoryMetric) -> Unit
) {

    private var job: Job? = null

    /**
     * Reads the heap once without storing it.
     */
    fun read(): MemoryMetric {
        val total = runtime.totalMemory().coerceAtLeast(0L)
        val free = runtime.freeMemory().coerceAtLeast(0L)
        val used = (total - free).coerceAtLeast(0L)
        val max = runtime.maxMemory().coerceAtLeast(0L)
        val native = Debug.getNativeHeapAllocatedSize().coerceAtLeast(0L)
        return MemoryMetric(
            timestamp = System.currentTimeMillis(),
            usedMemoryMb = bytesToMb(used),
            freeMemoryMb = bytesToMb(free),
            maxMemoryMb = bytesToMb(max),
            totalMemoryMb = bytesToMb(total),
            nativeMemoryMb = bytesToMb(native)
        )
    }

    /**
     * Emits one sample immediately, then one every 30 seconds until [stop].
     */
    fun start() {
        if (job?.isActive == true) {
            return
        }
        job = scope.launch {
            while (isActive) {
                onSample(read())
                delay(MEMORY_SAMPLE_INTERVAL_MS)
            }
        }
    }

    /**
     * Cancels the sampling loop. Samples already delivered are kept.
     */
    fun stop() {
        job?.cancel()
        job = null
    }
}
