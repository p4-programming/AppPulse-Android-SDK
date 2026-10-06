package io.apppulse.sdk.sync

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import io.apppulse.sdk.config.AppPulseOptions
import io.apppulse.sdk.core.AppPulseLogger
import io.apppulse.sdk.crash.CrashEntity
import io.apppulse.sdk.network.dto.AnrDto
import io.apppulse.sdk.network.dto.BitmapDto
import io.apppulse.sdk.network.dto.CrashDto
import io.apppulse.sdk.network.dto.FpsDto
import io.apppulse.sdk.network.dto.HealthDto
import io.apppulse.sdk.network.dto.HealthReportDto
import io.apppulse.sdk.network.dto.LeakDto
import io.apppulse.sdk.network.dto.MemoryDto
import io.apppulse.sdk.network.dto.MemorySampleDto
import io.apppulse.sdk.network.dto.NetworkCallDto
import io.apppulse.sdk.network.dto.PerformanceDto
import io.apppulse.sdk.network.dto.ScreenDto
import io.apppulse.sdk.network.dto.SecurityDto
import io.apppulse.sdk.network.dto.SecurityIssueDto
import io.apppulse.sdk.network.dto.StartupDto
import io.apppulse.sdk.storage.AppPulseDatabase
import io.apppulse.sdk.storage.entity.AnrEntity
import io.apppulse.sdk.storage.entity.BitmapEntity
import io.apppulse.sdk.storage.entity.FpsEntity
import io.apppulse.sdk.storage.entity.HealthSnapshotEntity
import io.apppulse.sdk.storage.entity.LeakEntity
import io.apppulse.sdk.storage.entity.MemoryEntity
import io.apppulse.sdk.storage.entity.NetworkEntity
import io.apppulse.sdk.storage.entity.ScreenEntity
import io.apppulse.sdk.storage.entity.SecurityIssueEntity
import io.apppulse.sdk.storage.entity.StartupEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * Reads unsynced Room rows, uploads them, and marks the accepted rows.
 *
 * A failed batch stays unsynced. The next [sync] or [SyncWorker] run tries it
 * again. Timeouts, lost connectivity, and HTTP 5xx ask WorkManager to retry.
 * HTTP 4xx other than 408 and 429 is a permanent failure for that attempt.
 *
 * @param application Host application. Used for connectivity and WorkManager.
 * @param options API key, base URL, and the auto-sync switch.
 * @param database Local store of unsynced rows.
 * @param uploads Retrofit upload calls.
 */
class SyncManager(
    private val application: Application,
    private val options: AppPulseOptions,
    private val database: AppPulseDatabase,
    private val uploads: UploadRepository
) {

    private val gate = Mutex()

    /**
     * True when automatic upload is currently enabled.
     */
    fun isAutoSyncEnabled(): Boolean = options.autoSyncEnabled

    /**
     * Uploads every unsynced batch that fits in one run.
     */
    suspend fun sync(): SyncOutcome {
        if (!options.canSync()) {
            AppPulseLogger.w(TAG, "Sync skipped because the API key is empty")
            return SyncOutcome.FAILURE
        }
        if (!isOnline()) {
            AppPulseLogger.w(TAG, "No Internet")
            return SyncOutcome.RETRY
        }
        return gate.withLock {
            withContext(Dispatchers.IO) {
                AppPulseLogger.i(TAG, "Sync started")
                var retry = false
                var failure = false
                listOf(
                    ::uploadCrashes,
                    ::uploadPerformance,
                    ::uploadMemory,
                    ::uploadSecurity,
                    ::uploadHealth
                ).forEach { step ->
                    when (step()) {
                        SyncOutcome.RETRY -> retry = true
                        SyncOutcome.FAILURE -> failure = true
                        SyncOutcome.SUCCESS -> Unit
                    }
                }
                val outcome = when {
                    retry -> SyncOutcome.RETRY
                    failure -> SyncOutcome.FAILURE
                    else -> SyncOutcome.SUCCESS
                }
                AppPulseLogger.i(TAG, "Sync finished")
                outcome
            }
        }
    }

    /**
     * Schedules [SyncWorker] for [AppPulseOptions.syncIntervalMinutes].
     *
     * The work requires a network connection and a battery that is not low.
     */
    fun schedule(context: Context) {
        val minutes = options.syncIntervalMinutes.coerceAtLeast(AppPulseOptions.MIN_SYNC_INTERVAL_MINUTES)
        val request = PeriodicWorkRequestBuilder<SyncWorker>(minutes, TimeUnit.MINUTES)
            .setConstraints(constraints())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, BACKOFF_SECONDS, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context.applicationContext).enqueueUniquePeriodicWork(
            UNIQUE_WORK,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
        AppPulseLogger.i(TAG, "Auto sync enabled. Interval = $minutes minutes")
    }

    /**
     * Cancels the periodic sync work. Rows already stored stay on the device.
     */
    fun cancel(context: Context) {
        WorkManager.getInstance(context.applicationContext).cancelUniqueWork(UNIQUE_WORK)
        AppPulseLogger.i(TAG, "Auto sync disabled")
    }

    private suspend fun uploadCrashes(): SyncOutcome {
        val rows = database.crashDao().unsynced(BATCH_LIMIT)
        if (rows.isEmpty()) return SyncOutcome.SUCCESS
        return upload(rows.size, "crashes") {
            uploads.uploadCrashes(rows.map { it.toDto() })
            database.crashDao().markSynced(rows.map { it.id })
        }
    }

    private suspend fun uploadPerformance(): SyncOutcome {
        val startups = database.startupDao().unsynced(BATCH_LIMIT)
        val screens = database.screenDao().unsynced(BATCH_LIMIT)
        val calls = database.networkDao().unsynced(BATCH_LIMIT)
        val fps = database.fpsDao().unsynced(BATCH_LIMIT)
        val anrs = database.anrDao().unsynced(BATCH_LIMIT)
        val count = startups.size + screens.size + calls.size + fps.size + anrs.size
        if (count == 0) return SyncOutcome.SUCCESS
        return upload(count, "performance") {
            uploads.uploadPerformance(
                PerformanceDto(
                    startups = startups.map { it.toDto() },
                    screens = screens.map { it.toDto() },
                    calls = calls.map { it.toDto() },
                    fps = fps.map { it.toDto() },
                    anrs = anrs.map { it.toDto() }
                )
            )
            if (startups.isNotEmpty()) database.startupDao().markSynced(startups.map { it.id })
            if (screens.isNotEmpty()) database.screenDao().markSynced(screens.map { it.id })
            if (calls.isNotEmpty()) database.networkDao().markSynced(calls.map { it.id })
            if (fps.isNotEmpty()) database.fpsDao().markSynced(fps.map { it.id })
            if (anrs.isNotEmpty()) database.anrDao().markSynced(anrs.map { it.id })
        }
    }

    private suspend fun uploadMemory(): SyncOutcome {
        val samples = database.memoryDao().unsynced(BATCH_LIMIT)
        val leaks = database.leakDao().unsynced(BATCH_LIMIT)
        val bitmaps = database.bitmapDao().unsynced(BATCH_LIMIT)
        val count = samples.size + leaks.size + bitmaps.size
        if (count == 0) return SyncOutcome.SUCCESS
        return upload(count, "memory") {
            uploads.uploadMemory(
                MemoryDto(
                    samples = samples.map { it.toDto() },
                    leaks = leaks.map { it.toDto() },
                    bitmaps = bitmaps.map { it.toDto() }
                )
            )
            if (samples.isNotEmpty()) database.memoryDao().markSynced(samples.map { it.id })
            if (leaks.isNotEmpty()) database.leakDao().markSynced(leaks.map { it.id })
            if (bitmaps.isNotEmpty()) database.bitmapDao().markSynced(bitmaps.map { it.id })
        }
    }

    private suspend fun uploadSecurity(): SyncOutcome {
        val issues = database.securityIssueDao().unsynced(BATCH_LIMIT)
        if (issues.isEmpty()) return SyncOutcome.SUCCESS
        return upload(issues.size, "security") {
            uploads.uploadSecurity(SecurityDto(issues.map { it.toDto() }))
            database.securityIssueDao().markSynced(issues.map { it.id })
        }
    }

    private suspend fun uploadHealth(): SyncOutcome {
        val reports = database.healthSnapshotDao().unsynced(BATCH_LIMIT)
        if (reports.isEmpty()) return SyncOutcome.SUCCESS
        return upload(reports.size, "health") {
            uploads.uploadHealth(HealthDto(reports.map { it.toDto() }))
            database.healthSnapshotDao().markSynced(reports.map { it.id })
        }
    }

    private suspend fun upload(count: Int, name: String, block: suspend () -> Unit): SyncOutcome {
        return try {
            block()
            AppPulseLogger.i(TAG, "Sync uploaded $name=$count")
            SyncOutcome.SUCCESS
        } catch (error: SyncCallException) {
            AppPulseLogger.w(TAG, "Sync $name failed: ${error.message}")
            error.outcome
        } catch (error: Throwable) {
            AppPulseLogger.e(TAG, "Sync $name failed", error)
            SyncOutcome.RETRY
        }
    }

    private fun isOnline(): Boolean {
        val manager = application.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = manager.activeNetwork ?: return false
        val capabilities = manager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun constraints(): Constraints {
        return Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()
    }

    private fun CrashEntity.toDto(): CrashDto {
        return CrashDto(
            id = id,
            timestamp = timestamp,
            exceptionType = exceptionType,
            message = message,
            stackTrace = stackTrace,
            deviceManufacturer = deviceManufacturer,
            deviceModel = deviceModel,
            androidVersion = androidVersion,
            appVersion = appVersion
        )
    }

    private fun StartupEntity.toDto(): StartupDto {
        return StartupDto(
            id = id,
            launchTimestamp = launchTimestamp,
            firstActivityTimestamp = firstActivityTimestamp,
            startupTimeMs = startupTimeMs,
            slowStartup = slowStartup,
            activityName = activityName
        )
    }

    private fun ScreenEntity.toDto(): ScreenDto {
        return ScreenDto(
            id = id,
            activityName = activityName,
            openTimestamp = openTimestamp,
            fullyDrawnTimestamp = fullyDrawnTimestamp,
            renderTimeMs = renderTimeMs,
            slowScreen = slowScreen
        )
    }

    private fun NetworkEntity.toDto(): NetworkCallDto {
        return NetworkCallDto(
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

    private fun FpsEntity.toDto(): FpsDto {
        return FpsDto(
            id = id,
            timestamp = timestamp,
            fps = fps,
            droppedFrames = droppedFrames,
            averageFps = averageFps,
            performanceIssue = performanceIssue
        )
    }

    private fun AnrEntity.toDto(): AnrDto {
        return AnrDto(
            id = id,
            timestamp = timestamp,
            blockedDurationMs = blockedDurationMs,
            stackTrace = stackTrace,
            threadName = threadName
        )
    }

    private fun MemoryEntity.toDto(): MemorySampleDto {
        return MemorySampleDto(
            id = id,
            timestamp = timestamp,
            usedMemoryMb = usedMemoryMb,
            freeMemoryMb = freeMemoryMb,
            maxMemoryMb = maxMemoryMb,
            totalMemoryMb = totalMemoryMb,
            nativeMemoryMb = nativeMemoryMb
        )
    }

    private fun LeakEntity.toDto(): LeakDto {
        return LeakDto(
            id = id,
            componentName = componentName,
            componentType = componentType,
            destroyedTimestamp = destroyedTimestamp,
            detectedTimestamp = detectedTimestamp
        )
    }

    private fun BitmapEntity.toDto(): BitmapDto {
        return BitmapDto(
            id = id,
            timestamp = timestamp,
            sizeMb = sizeMb,
            bitmapCount = bitmapCount,
            largeCount = largeCount,
            largeBitmap = largeBitmap
        )
    }

    private fun SecurityIssueEntity.toDto(): SecurityIssueDto {
        return SecurityIssueDto(
            id = id,
            timestamp = timestamp,
            category = category,
            code = code,
            title = title,
            description = description,
            severity = severity,
            target = target
        )
    }

    private fun HealthSnapshotEntity.toDto(): HealthReportDto {
        return HealthReportDto(
            id = id,
            generatedAt = generatedAt,
            overallScore = overallScore,
            overallStatus = overallStatus,
            crashScore = crashScore,
            performanceScore = performanceScore,
            memoryScore = memoryScore,
            securityScore = securityScore
        )
    }

    companion object {
        const val UNIQUE_WORK = "apppulse-sync"
        private const val TAG = "AppPulseSync"
        private const val BATCH_LIMIT = 50
        private const val BACKOFF_SECONDS = 30L
    }
}
