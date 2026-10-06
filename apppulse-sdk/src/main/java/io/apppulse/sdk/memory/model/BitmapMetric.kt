package io.apppulse.sdk.memory.model

/**
 * A bitmap snapshot or a single bitmap that crossed the 5 MB threshold.
 *
 * @property id Stored row id. `0` before insert.
 * @property timestamp Wall-clock time of the measurement.
 * @property sizeMb Total bytes represented by this row, in MB.
 * @property bitmapCount How many bitmaps this row describes.
 * @property largeCount How many of those bitmaps are larger than 5 MB.
 * @property largeBitmap True when [largeCount] is greater than zero.
 */
data class BitmapMetric(
    val id: Long = 0,
    val timestamp: Long,
    val sizeMb: Double,
    val bitmapCount: Int,
    val largeCount: Int,
    val largeBitmap: Boolean
)
