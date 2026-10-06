package io.apppulse.sdk.storage.repository

import io.apppulse.sdk.performance.model.AnrMetric
import io.apppulse.sdk.performance.model.FpsMetric
import io.apppulse.sdk.performance.model.NetworkMetric
import io.apppulse.sdk.performance.model.PerformanceReport
import io.apppulse.sdk.performance.model.ScreenMetric
import io.apppulse.sdk.performance.model.StartupMetric
import io.apppulse.sdk.storage.dao.AnrDao
import io.apppulse.sdk.storage.dao.FpsDao
import io.apppulse.sdk.storage.dao.NetworkDao
import io.apppulse.sdk.storage.dao.ScreenDao
import io.apppulse.sdk.storage.dao.StartupDao
import io.apppulse.sdk.storage.entity.AnrEntity
import io.apppulse.sdk.storage.entity.FpsEntity
import io.apppulse.sdk.storage.entity.NetworkEntity
import io.apppulse.sdk.storage.entity.ScreenEntity
import io.apppulse.sdk.storage.entity.StartupEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Reads and writes performance metrics.
 *
 * Database work runs on [Dispatchers.IO]. Trackers call the save methods.
 * The dashboard calls the load methods.
 */
class PerformanceRepository(
    private val startupDao: StartupDao,
    private val screenDao: ScreenDao,
    private val networkDao: NetworkDao,
    private val fpsDao: FpsDao,
    private val anrDao: AnrDao
) {

    /**
     * Stores a cold-start measurement.
     */
    suspend fun saveStartupMetric(metric: StartupMetric) {
        withContext(Dispatchers.IO) {
            startupDao.insert(metric.toEntity())
        }
    }

    /**
     * Stores a screen render measurement.
     */
    suspend fun saveScreenMetric(metric: ScreenMetric) {
        withContext(Dispatchers.IO) {
            screenDao.insert(metric.toEntity())
        }
    }

    /**
     * Stores an HTTP call measurement.
     */
    suspend fun saveNetworkMetric(metric: NetworkMetric) {
        withContext(Dispatchers.IO) {
            networkDao.insert(metric.toEntity())
        }
    }

    /**
     * Stores an FPS sample.
     */
    suspend fun saveFpsMetric(metric: FpsMetric) {
        withContext(Dispatchers.IO) {
            fpsDao.insert(metric.toEntity())
        }
    }

    /**
     * Stores a detected main-thread stall.
     */
    suspend fun saveAnrMetric(metric: AnrMetric) {
        withContext(Dispatchers.IO) {
            anrDao.insert(metric.toEntity())
        }
    }

    /**
     * Returns the summary card values.
     */
    suspend fun loadReport(): PerformanceReport {
        return withContext(Dispatchers.IO) {
            val slowestScreen = screenDao.slowestScreen()
            val slowestRequest = networkDao.slowestRequest()
            PerformanceReport(
                averageStartupMs = startupDao.averageStartupTime()?.toLong(),
                averageFps = fpsDao.averageFps(),
                slowestScreenName = slowestScreen?.activityName,
                slowestScreenMs = slowestScreen?.renderTimeMs,
                slowestApiLabel = slowestRequest?.toMetric()?.apiLabel(),
                slowestApiMs = slowestRequest?.durationMs,
                anrCount = anrDao.count()
            )
        }
    }

    /**
     * Returns recent cold starts, newest first.
     */
    suspend fun loadStartupMetrics(limit: Int = LIST_LIMIT): List<StartupMetric> {
        return withContext(Dispatchers.IO) {
            startupDao.getAll(limit).map { it.toMetric() }
        }
    }

    /**
     * Returns screens that rendered slower than the slow-screen threshold.
     */
    suspend fun loadSlowScreens(limit: Int = LIST_LIMIT): List<ScreenMetric> {
        return withContext(Dispatchers.IO) {
            screenDao.getSlowScreens(limit).map { it.toMetric() }
        }
    }

    /**
     * Returns recent HTTP calls, newest first.
     */
    suspend fun loadNetworkMetrics(limit: Int = LIST_LIMIT): List<NetworkMetric> {
        return withContext(Dispatchers.IO) {
            networkDao.getAll(limit).map { it.toMetric() }
        }
    }

    /**
     * Returns recent FPS samples, newest first.
     */
    suspend fun loadFpsMetrics(limit: Int = LIST_LIMIT): List<FpsMetric> {
        return withContext(Dispatchers.IO) {
            fpsDao.getAll(limit).map { it.toMetric() }
        }
    }

    /**
     * Returns recent ANR records, newest first.
     */
    suspend fun loadAnrMetrics(limit: Int = LIST_LIMIT): List<AnrMetric> {
        return withContext(Dispatchers.IO) {
            anrDao.getAll(limit).map { it.toMetric() }
        }
    }

    private fun StartupMetric.toEntity(): StartupEntity {
        return StartupEntity(
            id = id,
            launchTimestamp = launchTimestamp,
            firstActivityTimestamp = firstActivityTimestamp,
            startupTimeMs = startupTimeMs,
            slowStartup = slowStartup,
            activityName = activityName
        )
    }

    private fun StartupEntity.toMetric(): StartupMetric {
        return StartupMetric(
            id = id,
            launchTimestamp = launchTimestamp,
            firstActivityTimestamp = firstActivityTimestamp,
            startupTimeMs = startupTimeMs,
            slowStartup = slowStartup,
            activityName = activityName
        )
    }

    private fun ScreenMetric.toEntity(): ScreenEntity {
        return ScreenEntity(
            id = id,
            activityName = activityName,
            openTimestamp = openTimestamp,
            fullyDrawnTimestamp = fullyDrawnTimestamp,
            renderTimeMs = renderTimeMs,
            slowScreen = slowScreen
        )
    }

    private fun ScreenEntity.toMetric(): ScreenMetric {
        return ScreenMetric(
            id = id,
            activityName = activityName,
            openTimestamp = openTimestamp,
            fullyDrawnTimestamp = fullyDrawnTimestamp,
            renderTimeMs = renderTimeMs,
            slowScreen = slowScreen
        )
    }

    private fun NetworkMetric.toEntity(): NetworkEntity {
        return NetworkEntity(
            id = id,
            url = url,
            method = method,
            responseCode = responseCode,
            requestSize = requestSize,
            responseSize = responseSize,
            durationMs = durationMs,
            slowApi = slowApi,
            timestamp = timestamp
        )
    }

    private fun NetworkEntity.toMetric(): NetworkMetric {
        return NetworkMetric(
            id = id,
            url = url,
            method = method,
            responseCode = responseCode,
            requestSize = requestSize,
            responseSize = responseSize,
            durationMs = durationMs,
            slowApi = slowApi,
            timestamp = timestamp
        )
    }

    private fun FpsMetric.toEntity(): FpsEntity {
        return FpsEntity(
            id = id,
            timestamp = timestamp,
            fps = fps,
            droppedFrames = droppedFrames,
            averageFps = averageFps,
            performanceIssue = performanceIssue
        )
    }

    private fun FpsEntity.toMetric(): FpsMetric {
        return FpsMetric(
            id = id,
            timestamp = timestamp,
            fps = fps,
            droppedFrames = droppedFrames,
            averageFps = averageFps,
            performanceIssue = performanceIssue
        )
    }

    private fun AnrMetric.toEntity(): AnrEntity {
        return AnrEntity(
            id = id,
            timestamp = timestamp,
            blockedDurationMs = blockedDurationMs,
            stackTrace = stackTrace,
            threadName = threadName
        )
    }

    private fun AnrEntity.toMetric(): AnrMetric {
        return AnrMetric(
            id = id,
            timestamp = timestamp,
            blockedDurationMs = blockedDurationMs,
            stackTrace = stackTrace,
            threadName = threadName
        )
    }

    private companion object {
        const val LIST_LIMIT = 50
    }
}
