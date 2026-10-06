package io.apppulse.sdk.storage.repository

import io.apppulse.sdk.memory.model.BitmapMetric
import io.apppulse.sdk.memory.model.LeakCandidate
import io.apppulse.sdk.memory.model.MemoryMetric
import io.apppulse.sdk.storage.dao.BitmapDao
import io.apppulse.sdk.storage.dao.LeakDao
import io.apppulse.sdk.storage.dao.MemoryDao
import io.apppulse.sdk.storage.entity.BitmapEntity
import io.apppulse.sdk.storage.entity.LeakEntity
import io.apppulse.sdk.storage.entity.MemoryEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Reads and writes memory, leak, and bitmap rows.
 *
 * Database work runs on [Dispatchers.IO].
 */
class MemoryRepository(
    private val memoryDao: MemoryDao,
    private val leakDao: LeakDao,
    private val bitmapDao: BitmapDao
) {

    /**
     * Stores one heap sample.
     */
    suspend fun saveMemoryMetric(metric: MemoryMetric) {
        withContext(Dispatchers.IO) {
            memoryDao.insert(metric.toEntity())
        }
    }

    /**
     * Stores one leak candidate.
     */
    suspend fun saveLeak(candidate: LeakCandidate) {
        withContext(Dispatchers.IO) {
            leakDao.insert(candidate.toEntity())
        }
    }

    /**
     * Stores one bitmap measurement.
     */
    suspend fun saveBitmapMetric(metric: BitmapMetric) {
        withContext(Dispatchers.IO) {
            bitmapDao.insert(metric.toEntity())
        }
    }

    /**
     * Returns recent heap samples, newest first.
     */
    suspend fun getMemoryMetrics(limit: Int = LIST_LIMIT): List<MemoryMetric> {
        return withContext(Dispatchers.IO) {
            memoryDao.getAll(limit).map { it.toMetric() }
        }
    }

    /**
     * Returns stored leak candidates, newest detection first.
     */
    suspend fun getLeakCandidates(limit: Int = LIST_LIMIT): List<LeakCandidate> {
        return withContext(Dispatchers.IO) {
            leakDao.getAll(limit).map { it.toMetric() }
        }
    }

    /**
     * Returns recent bitmap rows, newest first.
     */
    suspend fun getBitmapMetrics(limit: Int = LIST_LIMIT): List<BitmapMetric> {
        return withContext(Dispatchers.IO) {
            bitmapDao.getAll(limit).map { it.toMetric() }
        }
    }

    /**
     * Returns the largest used-heap value, or null when none are stored.
     */
    suspend fun peakUsedMemoryMb(): Double? {
        return withContext(Dispatchers.IO) {
            memoryDao.peakUsedMemoryMb()
        }
    }

    /**
     * Returns how many leak candidates are stored.
     */
    suspend fun leakCount(): Int {
        return withContext(Dispatchers.IO) {
            leakDao.count()
        }
    }

    /**
     * Returns the mean used heap, or null when none are stored.
     */
    suspend fun averageUsedMemoryMb(): Double? {
        return withContext(Dispatchers.IO) {
            memoryDao.averageUsedMemoryMb()
        }
    }

    private fun MemoryMetric.toEntity(): MemoryEntity {
        return MemoryEntity(
            id = id,
            timestamp = timestamp,
            usedMemoryMb = usedMemoryMb,
            freeMemoryMb = freeMemoryMb,
            maxMemoryMb = maxMemoryMb,
            totalMemoryMb = totalMemoryMb,
            nativeMemoryMb = nativeMemoryMb
        )
    }

    private fun MemoryEntity.toMetric(): MemoryMetric {
        return MemoryMetric(
            id = id,
            timestamp = timestamp,
            usedMemoryMb = usedMemoryMb,
            freeMemoryMb = freeMemoryMb,
            maxMemoryMb = maxMemoryMb,
            totalMemoryMb = totalMemoryMb,
            nativeMemoryMb = nativeMemoryMb
        )
    }

    private fun LeakCandidate.toEntity(): LeakEntity {
        return LeakEntity(
            id = id,
            componentName = componentName,
            componentType = componentType,
            destroyedTimestamp = destroyedTimestamp,
            detectedTimestamp = detectedTimestamp
        )
    }

    private fun LeakEntity.toMetric(): LeakCandidate {
        return LeakCandidate(
            id = id,
            componentName = componentName,
            componentType = componentType,
            destroyedTimestamp = destroyedTimestamp,
            detectedTimestamp = detectedTimestamp
        )
    }

    private fun BitmapMetric.toEntity(): BitmapEntity {
        return BitmapEntity(
            id = id,
            timestamp = timestamp,
            sizeMb = sizeMb,
            bitmapCount = bitmapCount,
            largeCount = largeCount,
            largeBitmap = largeBitmap
        )
    }

    private fun BitmapEntity.toMetric(): BitmapMetric {
        return BitmapMetric(
            id = id,
            timestamp = timestamp,
            sizeMb = sizeMb,
            bitmapCount = bitmapCount,
            largeCount = largeCount,
            largeBitmap = largeBitmap
        )
    }

    private companion object {
        const val LIST_LIMIT = 50
    }
}
