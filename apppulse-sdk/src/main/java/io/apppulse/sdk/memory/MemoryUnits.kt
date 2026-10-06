package io.apppulse.sdk.memory

internal const val BYTES_PER_MEGABYTE = 1024.0 * 1024.0

internal const val LARGE_BITMAP_BYTES = (5.0 * BYTES_PER_MEGABYTE).toLong()

internal const val MEMORY_SAMPLE_INTERVAL_MS = 30_000L

internal const val LEAK_CHECK_DELAY_MS = 5_000L

/**
 * Converts a byte count into mebibytes.
 */
internal fun bytesToMb(bytes: Long): Double {
    return bytes.coerceAtLeast(0L).toDouble() / BYTES_PER_MEGABYTE
}
