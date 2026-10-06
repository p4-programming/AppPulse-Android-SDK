package io.apppulse.sdk.health

import io.apppulse.sdk.health.model.HealthReport
import io.apppulse.sdk.security.model.SecuritySeverity
import io.apppulse.sdk.storage.dao.AnrDao
import io.apppulse.sdk.storage.dao.BitmapDao
import io.apppulse.sdk.storage.dao.CrashDao
import io.apppulse.sdk.storage.dao.FpsDao
import io.apppulse.sdk.storage.dao.LeakDao
import io.apppulse.sdk.storage.dao.MemoryDao
import io.apppulse.sdk.storage.dao.NetworkDao
import io.apppulse.sdk.storage.dao.ScreenDao
import io.apppulse.sdk.storage.dao.SecurityIssueDao
import io.apppulse.sdk.storage.dao.StartupDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Reads every AppPulse module and asks [HealthCalculator] for one report.
 *
 * Queries run on [Dispatchers.IO].
 */
class HealthEngine(
    private val crashDao: CrashDao,
    private val startupDao: StartupDao,
    private val screenDao: ScreenDao,
    private val networkDao: NetworkDao,
    private val fpsDao: FpsDao,
    private val anrDao: AnrDao,
    private val memoryDao: MemoryDao,
    private val leakDao: LeakDao,
    private val bitmapDao: BitmapDao,
    private val securityIssueDao: SecurityIssueDao,
    private val calculator: HealthCalculator = HealthCalculator()
) {

    /**
     * Builds a report from the rows currently stored in `apppulse.db`.
     *
     * @param previousOverall Overall score of the previous report, or null.
     */
    suspend fun evaluate(previousOverall: Int?): HealthReport {
        return withContext(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            val totalCrashes = crashDao.count()
            val crash = CrashHealthInput(
                totalCrashes = totalCrashes,
                crashesLastDay = crashDao.countSince(now - DAY_MS),
                fatalCrashes = totalCrashes
            )
            val performance = PerformanceHealthInput(
                averageStartupMs = startupDao.averageStartupTime(),
                averageFps = fpsDao.averageFps(),
                anrCount = anrDao.count(),
                slowApiCount = networkDao.slowCount(),
                slowScreenCount = screenDao.slowCount()
            )
            val memory = MemoryHealthInput(
                leakCount = leakDao.count(),
                peakMemoryMb = memoryDao.peakUsedMemoryMb(),
                averageMemoryMb = memoryDao.averageUsedMemoryMb(),
                maxMemoryMb = memoryDao.latestMaxMemoryMb(),
                bitmapViolations = bitmapDao.latestLargeCount() ?: 0
            )
            val issues = securityIssueDao.getAll()
            val security = SecurityHealthInput(
                criticalCount = issues.count { it.severity == SecuritySeverity.CRITICAL.name },
                highCount = issues.count { it.severity == SecuritySeverity.HIGH.name },
                mediumCount = issues.count { it.severity == SecuritySeverity.MEDIUM.name },
                lowCount = issues.count { it.severity == SecuritySeverity.LOW.name }
            )
            calculator.calculate(
                crash = crash,
                performance = performance,
                memory = memory,
                security = security,
                previousOverall = previousOverall,
                generatedAt = now
            )
        }
    }

    private companion object {
        const val DAY_MS = 24L * 60L * 60L * 1000L
    }
}
