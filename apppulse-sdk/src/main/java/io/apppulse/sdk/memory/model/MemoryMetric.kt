package io.apppulse.sdk.memory.model

/**
 * One heap sample.
 *
 * @property id Stored row id. `0` before insert.
 * @property timestamp Wall-clock time of the sample.
 * @property usedMemoryMb Bytes retained by the Java heap, in MB.
 * @property freeMemoryMb Bytes free inside the current Java heap, in MB.
 * @property maxMemoryMb Maximum Java heap the runtime will use, in MB.
 * @property totalMemoryMb Current Java heap size, in MB.
 * @property nativeMemoryMb Native heap allocated by the process, in MB.
 */
data class MemoryMetric(
    val id: Long = 0,
    val timestamp: Long,
    val usedMemoryMb: Double,
    val freeMemoryMb: Double,
    val maxMemoryMb: Double,
    val totalMemoryMb: Double,
    val nativeMemoryMb: Double
)
