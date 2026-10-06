package io.apppulse.sdk.security

import android.app.Application
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.webkit.WebView
import io.apppulse.sdk.core.AppPulseLogger
import io.apppulse.sdk.security.model.SecurityIssue
import io.apppulse.sdk.security.model.SecurityReport
import io.apppulse.sdk.security.model.SecurityScore
import io.apppulse.sdk.storage.dao.NetworkDao
import io.apppulse.sdk.storage.repository.SecurityRepository
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicReference

/**
 * Runs manifest, permission, network, WebView, and application-configuration scans.
 *
 * Findings are replaced in Room on each full scan. WebView settings discovered
 * later replace only the WebView rows. The constructor dependencies can be
 * provided by Hilt later without moving the scan work.
 *
 * @param application Host application.
 * @param repository Security finding storage.
 * @param networkDao Recorded HTTP URLs.
 * @param calculator Score calculator.
 */
class SecurityScanner(
    private val application: Application,
    private val repository: SecurityRepository,
    private val networkDao: NetworkDao,
    private val calculator: SecurityHealthCalculator = SecurityHealthCalculator()
) {

    private val manifestScanner = ManifestScanner(application.packageManager, application.packageName)
    private val permissionScanner = PermissionScanner()
    private val networkScanner = NetworkScanner(networkDao)
    private val webViewScanner = WebViewScanner { findings ->
        scope.launch { updateWebViews(findings) }
    }

    private val scanMutex = Mutex()
    private val latestScore = AtomicReference(calculator.calculate(emptyList()))
    private val scope = CoroutineScope(
        SupervisorJob() + Dispatchers.Default + CoroutineExceptionHandler { _, throwable ->
            AppPulseLogger.e(TAG, "Security scan failed", throwable)
        }
    )

    /**
     * Registers WebView tracking and runs the first scan.
     */
    suspend fun initialize() {
        webViewScanner.attach(application)
        AppPulseLogger.i(TAG, "Security scan initialized for ${application.packageName}")
        performScan()
    }

    /**
     * Runs a scan without blocking the caller.
     */
    fun requestScan() {
        scope.launch { performScan() }
    }

    /**
     * Runs a scan and stores the findings.
     */
    suspend fun performScan() {
        scanMutex.withLock {
            val now = System.currentTimeMillis()
            val packageInfo = withContext(Dispatchers.IO) { loadPackageInfo() }
            val issues = ArrayList<SecurityIssue>()
            if (packageInfo != null) {
                issues += manifestScanner.scanComponents(packageInfo)
                issues += permissionScanner.scan(packageInfo)
            }
            issues += manifestScanner.scanConfiguration(application.applicationInfo)
            issues += networkScanner.scan()
            issues += webViewScanner.snapshot()
            val stamped = issues.map { it.copy(timestamp = now) }
            repository.replaceIssues(stamped)
            publish(stamped)
        }
    }

    /**
     * Returns the score from the latest completed scan.
     *
     * Before the first scan finishes, the score is 100.
     */
    fun currentScore(): SecurityScore = latestScore.get()

    /**
     * Reads one [WebView] immediately and stores any weak settings.
     *
     * @param webView Instance whose settings should be checked.
     * @param owner Name stored on the finding. Defaults to the WebView class name.
     */
    fun inspect(webView: WebView, owner: String = webView.javaClass.name) {
        val found = webViewScanner.inspect(webView, owner)
        if (found.isEmpty()) {
            return
        }
        webViewScanner.remember(found)
        scope.launch { updateWebViews(webViewScanner.snapshot()) }
    }

    private suspend fun updateWebViews(findings: List<SecurityIssue>) {
        scanMutex.withLock {
            val now = System.currentTimeMillis()
            val stamped = findings.map { it.copy(timestamp = now) }
            repository.replaceCategory(SecurityIssue.CATEGORY_WEBVIEW, stamped)
            publish(repository.getIssues())
        }
    }

    private fun publish(issues: List<SecurityIssue>) {
        val score = calculator.calculate(issues)
        latestScore.set(score)
        issues.forEach { issue ->
            AppPulseLogger.i(TAG, "${issue.title}: ${issue.target}")
        }
        AppPulseLogger.i(TAG, "Security Score = ${score.score}")
    }

    /**
     * Builds the report currently stored in Room.
     */
    suspend fun loadReport(): SecurityReport {
        val issues = repository.getIssues()
        val score = calculator.calculate(issues)
        latestScore.set(score)
        return SecurityReport(
            scannedAt = issues.maxOfOrNull { it.timestamp } ?: 0L,
            issues = issues,
            score = score
        )
    }

    private fun loadPackageInfo(): PackageInfo? {
        return try {
            val flags = PackageManager.GET_ACTIVITIES or
                PackageManager.GET_SERVICES or
                PackageManager.GET_RECEIVERS or
                PackageManager.GET_PROVIDERS or
                PackageManager.GET_PERMISSIONS
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                application.packageManager.getPackageInfo(
                    application.packageName,
                    PackageManager.PackageInfoFlags.of(flags.toLong())
                )
            } else {
                @Suppress("DEPRECATION")
                application.packageManager.getPackageInfo(application.packageName, flags)
            }
        } catch (throwable: Throwable) {
            AppPulseLogger.e(TAG, "Package info could not be read", throwable)
            null
        }
    }

    private companion object {
        const val TAG = "AppPulseSec"
    }
}
