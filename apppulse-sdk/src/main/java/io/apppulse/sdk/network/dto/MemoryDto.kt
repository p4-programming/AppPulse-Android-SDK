package io.apppulse.sdk.network.dto

/**
 * Memory batch sent to `POST /api/v1/memory`.
 */
data class MemoryDto(
    val samples: List<MemorySampleDto>,
    val leaks: List<LeakDto>,
    val bitmaps: List<BitmapDto>
) {
    /**
     * True when every list is empty.
     */
    fun isEmpty(): Boolean = samples.isEmpty() && leaks.isEmpty() && bitmaps.isEmpty()
}

/**
 * One heap sample.
 */
data class MemorySampleDto(
    val id: Long,
    val timestamp: Long,
    val usedMemoryMb: Double,
    val freeMemoryMb: Double,
    val maxMemoryMb: Double,
    val totalMemoryMb: Double,
    val nativeMemoryMb: Double
)

/**
 * One activity or fragment leak candidate.
 */
data class LeakDto(
    val id: Long,
    val componentName: String,
    val componentType: String,
    val destroyedTimestamp: Long,
    val detectedTimestamp: Long
)

/**
 * One bitmap measurement.
 */
data class BitmapDto(
    val id: Long,
    val timestamp: Long,
    val sizeMb: Double,
    val bitmapCount: Int,
    val largeCount: Int,
    val largeBitmap: Boolean
)
