package io.apppulse.sdk.health

import io.apppulse.sdk.core.AppPulseLogger
import io.apppulse.sdk.health.model.HealthLevel
import io.apppulse.sdk.health.model.HealthReport
import io.apppulse.sdk.health.model.HealthSnapshot
import io.apppulse.sdk.health.model.HealthTrend
import io.apppulse.sdk.storage.dao.HealthSnapshotDao
import io.apppulse.sdk.storage.entity.HealthSnapshotEntity
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicReference

/**
 * Generates health reports, keeps the latest one, and stores history.
 *
 * The cached report is what [io.apppulse.sdk.AppPulse.getHealthScore] returns.
 *
 * @param engine Reader and calculator for the current module data.
 * @param snapshotDao History used for trend analysis.
 */
class HealthScoreManager(
    private val engine: HealthEngine,
    private val snapshotDao: HealthSnapshotDao
) {

    private val gate = Mutex()
    private val cached = AtomicReference<HealthReport?>(null)
    private val scope = CoroutineScope(
        SupervisorJob() + Dispatchers.Default + CoroutineExceptionHandler { _, throwable ->
            AppPulseLogger.e(TAG, "Health report failed", throwable)
        }
    )

    /**
     * Reads module data, stores a snapshot, and returns the new report.
     */
    suspend fun generate(): HealthReport {
        return gate.withLock {
            val previous = snapshotDao.latest()?.overallScore
            val report = engine.evaluate(previous)
            snapshotDao.insert(report.toEntity())
            cached.set(report)
            AppPulseLogger.i(TAG, "Health Score Calculated")
            AppPulseLogger.i(TAG, "Health Score = ${report.overallScore}")
            AppPulseLogger.i(TAG, "Health Report Generated")
            AppPulseLogger.i(TAG, "Recommendations Generated")
            report.recommendations.forEach { line ->
                AppPulseLogger.i(TAG, line)
            }
            report
        }
    }

    /**
     * Generates a report without blocking the caller.
     */
    fun requestGenerate() {
        scope.launch { generate() }
    }

    /**
     * Returns the latest generated report, or a perfect score before the first run.
     */
    fun current(): HealthReport {
        return cached.get() ?: baseline()
    }

    /**
     * Returns the latest generated report, or null before the first run.
     */
    fun cached(): HealthReport? = cached.get()

    /**
     * Returns recent snapshots, oldest first.
     */
    suspend fun history(limit: Int = HISTORY_LIMIT): List<HealthSnapshot> {
        return snapshotDao.recent(limit).asReversed().map { entity ->
            HealthSnapshot(
                generatedAt = entity.generatedAt,
                overallScore = entity.overallScore,
                overallStatus = HealthLevel.valueOf(entity.overallStatus),
                crashScore = entity.crashScore,
                performanceScore = entity.performanceScore,
                memoryScore = entity.memoryScore,
                securityScore = entity.securityScore
            )
        }
    }

    private fun HealthReport.toEntity(): HealthSnapshotEntity {
        return HealthSnapshotEntity(
            generatedAt = generatedAt,
            overallScore = overallScore,
            overallStatus = overallStatus.name,
            crashScore = crashScore,
            performanceScore = performanceScore,
            memoryScore = memoryScore,
            securityScore = securityScore
        )
    }

    private fun baseline(): HealthReport {
        val calculator = HealthCalculator()
        return calculator.calculate(
            crash = CrashHealthInput(0, 0, 0),
            performance = PerformanceHealthInput(null, null, 0, 0, 0),
            memory = MemoryHealthInput(0, null, null, null, 0),
            security = SecurityHealthInput(0, 0, 0, 0),
            previousOverall = null,
            generatedAt = System.currentTimeMillis()
        ).copy(trend = HealthTrend.UNKNOWN)
    }

    private companion object {
        const val TAG = "AppPulseHealth"
        const val HISTORY_LIMIT = 12
    }
}
