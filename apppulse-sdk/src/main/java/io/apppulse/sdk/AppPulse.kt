package io.apppulse.sdk

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Looper
import androidx.fragment.app.Fragment
import io.apppulse.sdk.health.CrashHealthInput
import io.apppulse.sdk.health.HealthCalculator
import io.apppulse.sdk.health.HealthEngine
import io.apppulse.sdk.health.HealthScoreManager
import io.apppulse.sdk.health.MemoryHealthInput
import io.apppulse.sdk.health.PerformanceHealthInput
import io.apppulse.sdk.health.SecurityHealthInput
import io.apppulse.sdk.health.model.HealthReport
import io.apppulse.sdk.config.AppPulseConfig
import io.apppulse.sdk.config.AppPulseOptions
import io.apppulse.sdk.core.AppPulseLogger
import io.apppulse.sdk.crash.CrashTracker
import io.apppulse.sdk.memory.MemoryHealthCalculator
import io.apppulse.sdk.memory.MemoryTracker
import io.apppulse.sdk.memory.bytesToMb
import io.apppulse.sdk.performance.PerformanceTracker
import io.apppulse.sdk.performance.model.NetworkMetric
import io.apppulse.sdk.network.ApiClient
import io.apppulse.sdk.security.SecurityHealthCalculator
import io.apppulse.sdk.security.SecurityScanner
import io.apppulse.sdk.storage.AppPulseDatabase
import io.apppulse.sdk.storage.repository.MemoryRepository
import io.apppulse.sdk.storage.repository.PerformanceRepository
import io.apppulse.sdk.storage.repository.SecurityRepository
import io.apppulse.sdk.ui.CrashListActivity
import io.apppulse.sdk.ui.HealthDashboardActivity
import io.apppulse.sdk.ui.MemoryDashboardActivity
import io.apppulse.sdk.ui.PerformanceDashboardActivity
import io.apppulse.sdk.sync.SyncManager
import io.apppulse.sdk.sync.SyncOutcome
import io.apppulse.sdk.sync.UploadRepository
import io.apppulse.sdk.ui.SecurityDashboardActivity
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * Entry point for the AppPulse SDK.
 *
 * Call [init] once from `Application.onCreate`. The object retains the host
 * [Application], then starts the crash, performance, memory, and security
 * modules selected by [AppPulseConfig].
 *
 * Module objects are created at this composition root with explicit constructor
 * dependencies. Hilt is not wired yet; the same constructors can be annotated
 * and provided later without moving their responsibilities.
 */
object AppPulse {

    private val gate = Any()

    @Volatile
    private var applicationRef: Application? = null

    @Volatile
    private var configRef: AppPulseConfig? = null

    @Volatile
    private var crashTracker: CrashTracker? = null

    @Volatile
    private var performanceTracker: PerformanceTracker? = null

    @Volatile
    private var memoryTracker: MemoryTracker? = null

    @Volatile
    private var securityScanner: SecurityScanner? = null

    @Volatile
    private var healthManager: HealthScoreManager? = null

    @Volatile
    private var optionsRef: AppPulseOptions? = null

    @Volatile
    private var syncManagerRef: SyncManager? = null

    private val syncScope = CoroutineScope(
        SupervisorJob() + Dispatchers.IO + CoroutineExceptionHandler { _, throwable ->
            AppPulseLogger.e(TAG, "Sync failed", throwable)
        }
    )

    /**
     * Opens the local crash dashboard.
     *
     * The dashboard lists crashes stored in `apppulse.db`. When [context] is
     * not an [Activity], the screen is started in a new task.
     *
     * @param context Context used to launch [CrashListActivity].
     */
    fun openCrashDashboard(context: Context) {
        val intent = Intent(context, CrashListActivity::class.java)
        if (context !is Activity) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        AppPulseLogger.i(TAG, "Crash dashboard opened")
    }

    /**
     * Opens the local performance dashboard.
     *
     * When [context] is not an [Activity], the screen is started in a new task.
     *
     * @param context Context used to launch [PerformanceDashboardActivity].
     */
    fun openPerformanceDashboard(context: Context) {
        val intent = Intent(context, PerformanceDashboardActivity::class.java)
        if (context !is Activity) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        AppPulseLogger.i(TAG, "Performance dashboard opened")
    }

    /**
     * Opens the local memory dashboard.
     *
     * When [context] is not an [Activity], the screen is started in a new task.
     *
     * @param context Context used to launch [MemoryDashboardActivity].
     */
    fun openMemoryDashboard(context: Context) {
        val intent = Intent(context, MemoryDashboardActivity::class.java)
        if (context !is Activity) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        AppPulseLogger.i(TAG, "Memory dashboard opened")
    }

    /**
     * Opens the local security dashboard.
     *
     * When [context] is not an [Activity], the screen is started in a new task.
     *
     * @param context Context used to launch [SecurityDashboardActivity].
     */
    fun openSecurityDashboard(context: Context) {
        val intent = Intent(context, SecurityDashboardActivity::class.java)
        if (context !is Activity) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        AppPulseLogger.i(TAG, "Security dashboard opened")
    }

    /**
     * Opens the unified health dashboard.
     *
     * When [context] is not an [Activity], the screen is started in a new task.
     *
     * @param context Context used to launch [HealthDashboardActivity].
     */
    fun openHealthDashboard(context: Context) {
        val intent = Intent(context, HealthDashboardActivity::class.java)
        if (context !is Activity) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        AppPulseLogger.i(TAG, "Health dashboard opened")
    }

    /**
     * Uploads unsynced rows immediately.
     *
     * The call returns before the upload finishes. Rows that fail stay
     * unsynced for the next attempt. Auto sync does not need to be enabled.
     */
    fun syncNow() {
        val manager = syncManagerRef
        if (manager == null) {
            AppPulseLogger.w(TAG, "syncNow ignored because the SDK is not initialized")
            return
        }
        syncScope.launch {
            when (manager.sync()) {
                SyncOutcome.SUCCESS -> AppPulseLogger.i(TAG, "Sync finished")
                SyncOutcome.RETRY -> AppPulseLogger.w(TAG, "Sync will retry")
                SyncOutcome.FAILURE -> AppPulseLogger.w(TAG, "Sync failed")
            }
        }
    }

    /**
     * Turns automatic upload on and schedules [io.apppulse.sdk.sync.SyncWorker].
     *
     * When the API key is blank, the switch is stored and no work is scheduled.
     */
    fun enableAutoSync() {
        val application = applicationRef
        val options = optionsRef
        val manager = syncManagerRef
        if (application == null || options == null || manager == null) {
            AppPulseLogger.w(TAG, "enableAutoSync ignored because the SDK is not initialized")
            return
        }
        options.autoSyncEnabled = true
        if (options.canSync()) {
            manager.schedule(application)
        } else {
            AppPulseLogger.w(TAG, "Auto sync skipped because the API key is empty")
        }
    }

    /**
     * Turns automatic upload off and cancels the periodic sync work.
     */
    fun disableAutoSync() {
        val application = applicationRef
        val options = optionsRef
        val manager = syncManagerRef
        if (application == null || options == null || manager == null) {
            AppPulseLogger.w(TAG, "disableAutoSync ignored because the SDK is not initialized")
            return
        }
        options.autoSyncEnabled = false
        manager.cancel(application)
    }

    /**
     * Returns the sync manager created by [init], or null before initialization.
     */
    internal fun syncManager(): SyncManager? = syncManagerRef

    /**
     * Returns the latest overall health score, from 0 to 100.
     *
     * Before the first report is generated, the score is 100.
     */
    fun getHealthScore(): Int {
        val manager = healthManager
        if (manager == null) {
            AppPulseLogger.i(TAG, "Health Score = 100")
            return 100
        }
        return manager.current().overallScore
    }

    /**
     * Builds a new [HealthReport] from crash, performance, memory, and security data.
     *
     * Off the main thread this call waits for the report. On the main thread it
     * starts the report in the background and returns the latest cached report.
     */
    fun generateHealthReport(): HealthReport {
        val manager = healthManager
        if (manager == null) {
            AppPulseLogger.w(TAG, "generateHealthReport ignored because health scoring is unavailable")
            return emptyHealthReport()
        }
        if (Looper.myLooper() == Looper.getMainLooper()) {
            manager.requestGenerate()
            return manager.current()
        }
        return runBlocking {
            manager.generate()
        }
    }

    /**
     * Builds a health report and waits until it is stored.
     */
    internal suspend fun generateHealthReportAwait(): HealthReport {
        val manager = healthManager
        if (manager == null) {
            AppPulseLogger.w(TAG, "Health report ignored because health scoring is unavailable")
            return emptyHealthReport()
        }
        return manager.generate()
    }

    /**
     * Returns the process health manager, or null when [init] has not created it.
     */
    internal fun healthManager(): HealthScoreManager? = healthManager

    /**
     * Runs a security scan without blocking the caller.
     *
     * Findings replace the previous scan in `apppulse.db`. [getSecurityScore]
     * updates when the scan finishes.
     */
    fun runSecurityScan() {
        val scanner = securityScanner
        if (scanner == null) {
            AppPulseLogger.w(TAG, "runSecurityScan ignored because security scanning is unavailable")
            return
        }
        scanner.requestScan()
    }

    /**
     * Returns the latest security health score, from 0 to 100.
     *
     * Before the first scan finishes, the score is 100.
     */
    fun getSecurityScore(): Int {
        val scanner = securityScanner
        if (scanner == null) {
            val score = SecurityHealthCalculator().calculate(emptyList()).score
            AppPulseLogger.i(TAG, "Security Score = $score")
            return score
        }
        return scanner.currentScore().score
    }

    /**
     * Runs a security scan and waits until findings are stored.
     */
    internal suspend fun awaitSecurityScan() {
        val scanner = securityScanner
        if (scanner == null) {
            AppPulseLogger.w(TAG, "Security scan ignored because security scanning is unavailable")
            return
        }
        scanner.performScan()
    }

    /**
     * Starts heap sampling, leak detection, and bitmap scans.
     *
     * [init] calls this when memory tracking is enabled. Later calls are ignored
     * while monitoring is already running.
     */
    fun startMemoryMonitoring() {
        val tracker = memoryTracker
        if (tracker == null) {
            AppPulseLogger.w(TAG, "startMemoryMonitoring ignored because memory tracking is unavailable")
            return
        }
        tracker.start()
    }

    /**
     * Stops memory collectors. Rows already stored in `apppulse.db` remain.
     */
    fun stopMemoryMonitoring() {
        val tracker = memoryTracker
        if (tracker == null) {
            AppPulseLogger.w(TAG, "stopMemoryMonitoring ignored because memory tracking is unavailable")
            return
        }
        tracker.stop()
    }

    /**
     * Returns the latest memory health score, from 0 to 100.
     *
     * When monitoring has not started, the score is calculated from the current
     * heap with no leak or bitmap penalty.
     */
    fun getMemoryHealthScore(): Int {
        val tracker = memoryTracker
        if (tracker != null) {
            return tracker.healthScore()
        }
        val runtime = Runtime.getRuntime()
        val total = runtime.totalMemory().coerceAtLeast(0L)
        val free = runtime.freeMemory().coerceAtLeast(0L)
        val report = MemoryHealthCalculator().calculate(
            usedMemoryMb = bytesToMb((total - free).coerceAtLeast(0L)),
            maxMemoryMb = bytesToMb(runtime.maxMemory()),
            leakCount = 0,
            largeBitmapCount = 0
        )
        AppPulseLogger.i(TAG, "Memory Score = ${report.score}")
        return report.score
    }

    /**
     * Retains [activity] for leak-detector validation.
     *
     * Destroy [activity] after this call. The detector reports a candidate if
     * the instance is still reachable five seconds later. Call [clearSimulatedLeaks]
     * when the check is finished.
     */
    fun simulateActivityLeak(activity: Activity) {
        val tracker = memoryTracker
        if (tracker == null) {
            AppPulseLogger.w(TAG, "simulateActivityLeak ignored because memory tracking is unavailable")
            return
        }
        tracker.simulateActivityLeak(activity)
    }

    /**
     * Retains [fragment] for leak-detector validation.
     *
     * Remove [fragment] after this call, then call [clearSimulatedLeaks] when
     * the check is finished.
     */
    fun simulateFragmentLeak(fragment: Fragment) {
        val tracker = memoryTracker
        if (tracker == null) {
            AppPulseLogger.w(TAG, "simulateFragmentLeak ignored because memory tracking is unavailable")
            return
        }
        tracker.simulateFragmentLeak(fragment)
    }

    /**
     * Allocates a bitmap larger than 5 MB and records it.
     *
     * The bitmap stays reachable until [clearSimulatedLeaks].
     */
    fun simulateLargeBitmap(): Bitmap? {
        val tracker = memoryTracker
        if (tracker == null) {
            AppPulseLogger.w(TAG, "simulateLargeBitmap ignored because memory tracking is unavailable")
            return null
        }
        return tracker.simulateLargeBitmap()
    }

    /**
     * Releases activities, fragments, and bitmaps retained by the validation methods.
     */
    fun clearSimulatedLeaks() {
        memoryTracker?.clearSimulatedLeaks()
    }

    /**
     * Starts startup, screen, FPS, ANR, and network collection.
     *
     * [init] calls this when performance tracking is enabled, so a cold start
     * is not missed. Later calls are ignored while monitoring is already running.
     * [AppPulseOkHttpInterceptor][io.apppulse.sdk.performance.AppPulseOkHttpInterceptor]
     * records calls only while monitoring is running.
     */
    fun startPerformanceMonitoring() {
        val tracker = performanceTracker
        if (tracker == null) {
            AppPulseLogger.w(
                TAG,
                "startPerformanceMonitoring ignored because performance tracking is unavailable"
            )
            return
        }
        tracker.start()
    }

    /**
     * Stops performance collectors. Metrics already stored in `apppulse.db` remain.
     */
    fun stopPerformanceMonitoring() {
        val tracker = performanceTracker
        if (tracker == null) {
            AppPulseLogger.w(
                TAG,
                "stopPerformanceMonitoring ignored because performance tracking is unavailable"
            )
            return
        }
        tracker.stop()
    }

    /**
     * Stores one HTTP timing when performance monitoring is running.
     */
    internal fun recordNetworkCall(metric: NetworkMetric) {
        performanceTracker?.recordNetwork(metric)
    }

    /**
     * Returns the [Application] passed to [init].
     *
     * @throws IllegalStateException when [init] has not completed.
     */
    fun application(): Application {
        return applicationRef ?: throw IllegalStateException(
            "AppPulse.init(Application) must be called before using the SDK."
        )
    }

    /**
     * Returns the configuration captured by [init].
     *
     * @throws IllegalStateException when [init] has not completed.
     */
    fun config(): AppPulseConfig {
        return configRef ?: throw IllegalStateException(
            "AppPulse.init(Application) must be called before reading configuration."
        )
    }

    /**
     * Initializes AppPulse and the enabled trackers.
     *
     * The crash handler is installed on the calling thread before the other
     * modules start. Performance and memory monitoring also start on that
     * thread when they are enabled, so lifecycle callbacks are registered
     * before the first activity. The first security scan and the first health
     * report run off that thread when those features are enabled. A second
     * call is ignored.
     *
     * @param application Host application. Stored for the process lifetime.
     * @param config Feature flags and API key. Defaults enable every local tracker.
     */
    @JvmOverloads
    fun init(
        application: Application,
        config: AppPulseConfig = AppPulseConfig.Builder().build()
    ) {
        synchronized(gate) {
            if (applicationRef != null) {
                AppPulseLogger.w(TAG, "AppPulse.init ignored because the SDK is already initialized")
                return
            }
            applicationRef = application
            configRef = config
            optionsRef = AppPulseOptions.from(config)
            AppPulseLogger.debugMode = config.debugMode
        }

        AppPulseLogger.i(TAG, "Initializing AppPulse with $config")
        if (config.apiKey.isBlank()) {
            AppPulseLogger.w(TAG, "AppPulse apiKey is empty. Local tracking will still run.")
        }

        val options = optionsRef
        if (options != null) {
            val manager = createSyncManager(application, options)
            if (manager != null) {
                syncManagerRef = manager
                if (config.enableAutoSync) {
                    if (options.canSync()) {
                        manager.schedule(application)
                    } else {
                        AppPulseLogger.w(TAG, "Auto sync skipped because the API key is empty")
                    }
                } else {
                    AppPulseLogger.i(TAG, "Auto sync is disabled")
                }
            }
        }

        if (config.enableCrashTracking) {
            val tracker = CrashTracker(application)
            tracker.initialize()
            crashTracker = tracker
        } else {
            AppPulseLogger.i(TAG, "Crash tracking is disabled")
        }

        if (config.enablePerformanceTracking) {
            val tracker = createPerformanceTracker(application)
            if (tracker != null) {
                performanceTracker = tracker
                tracker.initialize()
                startPerformanceMonitoring()
            }
        } else {
            AppPulseLogger.i(TAG, "Performance tracking is disabled")
        }

        if (config.enableMemoryTracking) {
            val tracker = createMemoryTracker(application)
            if (tracker != null) {
                memoryTracker = tracker
                tracker.initialize()
                startMemoryMonitoring()
            }
        } else {
            AppPulseLogger.i(TAG, "Memory tracking is disabled")
        }

        if (config.enableSecurityTracking) {
            val scanner = createSecurityScanner(application)
            if (scanner != null) {
                securityScanner = scanner
                try {
                    runBlocking(Dispatchers.Default) {
                        scanner.initialize()
                    }
                } catch (throwable: Throwable) {
                    AppPulseLogger.e(TAG, "Security scanning failed to initialize", throwable)
                }
            }
        } else {
            AppPulseLogger.i(TAG, "Security tracking is disabled")
        }

        val manager = createHealthManager(application)
        if (manager != null) {
            healthManager = manager
            try {
                runBlocking(Dispatchers.Default) {
                    manager.generate()
                }
            } catch (throwable: Throwable) {
                AppPulseLogger.e(TAG, "Health report failed to generate", throwable)
            }
        }

        AppPulseLogger.i(
            TAG,
            "AppPulse initialized. Modules: " +
                "crash=${crashTracker != null}, " +
                "performance=${performanceTracker != null}, " +
                "memory=${memoryTracker != null}, " +
                "security=${securityScanner != null}, " +
                "health=${healthManager != null}, " +
                "sync=${syncManagerRef != null}"
        )
    }

    private fun createSyncManager(application: Application, options: AppPulseOptions): SyncManager? {
        return try {
            val database = AppPulseDatabase.getInstance(application)
            SyncManager(
                application = application,
                options = options,
                database = database,
                uploads = UploadRepository(ApiClient(options))
            )
        } catch (throwable: Throwable) {
            AppPulseLogger.e(TAG, "Sync failed to initialize", throwable)
            null
        }
    }

    private fun createHealthManager(application: Application): HealthScoreManager? {
        return try {
            val database = AppPulseDatabase.getInstance(application)
            val engine = HealthEngine(
                crashDao = database.crashDao(),
                startupDao = database.startupDao(),
                screenDao = database.screenDao(),
                networkDao = database.networkDao(),
                fpsDao = database.fpsDao(),
                anrDao = database.anrDao(),
                memoryDao = database.memoryDao(),
                leakDao = database.leakDao(),
                bitmapDao = database.bitmapDao(),
                securityIssueDao = database.securityIssueDao()
            )
            HealthScoreManager(engine, database.healthSnapshotDao())
        } catch (throwable: Throwable) {
            AppPulseLogger.e(TAG, "Health scoring failed to initialize", throwable)
            null
        }
    }

    private fun emptyHealthReport(): HealthReport {
        return HealthCalculator().calculate(
            crash = CrashHealthInput(0, 0, 0),
            performance = PerformanceHealthInput(null, null, 0, 0, 0),
            memory = MemoryHealthInput(0, null, null, null, 0),
            security = SecurityHealthInput(0, 0, 0, 0),
            previousOverall = null,
            generatedAt = System.currentTimeMillis()
        )
    }

    private fun createSecurityScanner(application: Application): SecurityScanner? {
        return try {
            val database = AppPulseDatabase.getInstance(application)
            val repository = SecurityRepository(database.securityIssueDao())
            SecurityScanner(
                application = application,
                repository = repository,
                networkDao = database.networkDao()
            )
        } catch (throwable: Throwable) {
            AppPulseLogger.e(TAG, "Security scanning failed to initialize", throwable)
            null
        }
    }

    private fun createMemoryTracker(application: Application): MemoryTracker? {
        return try {
            val database = AppPulseDatabase.getInstance(application)
            val repository = MemoryRepository(
                memoryDao = database.memoryDao(),
                leakDao = database.leakDao(),
                bitmapDao = database.bitmapDao()
            )
            MemoryTracker(application, repository)
        } catch (throwable: Throwable) {
            AppPulseLogger.e(TAG, "Memory tracking failed to initialize", throwable)
            null
        }
    }

    private fun createPerformanceTracker(application: Application): PerformanceTracker? {
        return try {
            val database = AppPulseDatabase.getInstance(application)
            val repository = PerformanceRepository(
                startupDao = database.startupDao(),
                screenDao = database.screenDao(),
                networkDao = database.networkDao(),
                fpsDao = database.fpsDao(),
                anrDao = database.anrDao()
            )
            PerformanceTracker(application, repository)
        } catch (throwable: Throwable) {
            AppPulseLogger.e(TAG, "Performance tracking failed to initialize", throwable)
            null
        }
    }

    private const val TAG = "AppPulse"
}
