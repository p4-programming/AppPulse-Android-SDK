package io.apppulse.sdk.security

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import io.apppulse.sdk.performance.shouldMeasure
import io.apppulse.sdk.security.model.SecurityIssue
import io.apppulse.sdk.security.model.SecuritySeverity
import java.util.IdentityHashMap

/**
 * Inspects [WebView] settings in started host activities.
 *
 * JavaScript is [SecuritySeverity.HIGH]. File access is [SecuritySeverity.MEDIUM].
 * Universal access from file URLs is [SecuritySeverity.CRITICAL]. AppPulse
 * dashboard activities are skipped.
 *
 * @param onFindings Called on the main thread with the current WebView findings
 * after a host activity resumes or is destroyed.
 */
class WebViewScanner(
    private val onFindings: (List<SecurityIssue>) -> Unit
) : Application.ActivityLifecycleCallbacks {

    private val mainHandler = Handler(Looper.getMainLooper())
    private val gate = Any()
    private val byActivity = IdentityHashMap<Activity, List<SecurityIssue>>()
    private val explicit = ArrayList<SecurityIssue>()

    @Volatile
    private var attached = false

    /**
     * Registers lifecycle callbacks. A second call is ignored.
     */
    fun attach(application: Application) {
        if (attached) {
            return
        }
        attached = true
        application.registerActivityLifecycleCallbacks(this)
    }

    /**
     * Returns the WebView findings collected from activities that are still alive.
     */
    fun snapshot(): List<SecurityIssue> {
        synchronized(gate) {
            return (byActivity.values.flatten() + explicit).distinctBy { it.code + it.target }
        }
    }

    /**
     * Keeps [issues] in future snapshots until a full scan replaces the WebView rows.
     */
    fun remember(issues: List<SecurityIssue>) {
        if (issues.isEmpty()) {
            return
        }
        synchronized(gate) {
            explicit += issues
            val unique = explicit.distinctBy { it.code + it.target }
            explicit.clear()
            explicit += unique
        }
    }

    /**
     * Inspects [webView] without waiting for a lifecycle event.
     *
     * @param owner Name stored as the finding target, usually the hosting activity.
     */
    fun inspect(webView: WebView, owner: String): List<SecurityIssue> {
        return settingsIssues(webView, owner)
    }

    override fun onActivityResumed(activity: Activity) {
        if (!activity.shouldMeasure()) {
            return
        }
        mainHandler.post {
            if (activity.isFinishing || activity.isDestroyed) {
                return@post
            }
            val root = activity.window?.decorView ?: return@post
            val issues = ArrayList<SecurityIssue>()
            collect(root, activity.javaClass.name, issues)
            synchronized(gate) {
                byActivity[activity] = issues
            }
            onFindings(snapshot())
        }
    }

    override fun onActivityDestroyed(activity: Activity) {
        val changed = synchronized(gate) { byActivity.remove(activity) != null }
        if (changed) {
            onFindings(snapshot())
        }
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit

    override fun onActivityStarted(activity: Activity) = Unit

    override fun onActivityPaused(activity: Activity) = Unit

    override fun onActivityStopped(activity: Activity) = Unit

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit

    private fun collect(view: View, owner: String, into: MutableList<SecurityIssue>) {
        if (view is WebView) {
            into += settingsIssues(view, owner)
        }
        if (view is ViewGroup) {
            for (index in 0 until view.childCount) {
                collect(view.getChildAt(index), owner, into)
            }
        }
    }

    private fun settingsIssues(webView: WebView, owner: String): List<SecurityIssue> {
        val settings = webView.settings
        val findings = ArrayList<SecurityIssue>(3)
        if (settings.javaScriptEnabled) {
            findings += webViewIssue(
                code = SecurityIssue.CODE_WEBVIEW_JAVASCRIPT,
                title = "WebView JavaScript enabled",
                description = "JavaScript is enabled in a WebView hosted by $owner.",
                severity = SecuritySeverity.HIGH,
                owner = owner
            )
        }
        if (settings.allowFileAccess) {
            findings += webViewIssue(
                code = SecurityIssue.CODE_WEBVIEW_FILE_ACCESS,
                title = "WebView file access enabled",
                description = "File access is enabled in a WebView hosted by $owner.",
                severity = SecuritySeverity.MEDIUM,
                owner = owner
            )
        }
        if (settings.allowUniversalAccessFromFileURLs) {
            findings += webViewIssue(
                code = SecurityIssue.CODE_WEBVIEW_UNIVERSAL_ACCESS,
                title = "WebView universal access enabled",
                description = "Universal access from file URLs is enabled in a WebView hosted by $owner.",
                severity = SecuritySeverity.CRITICAL,
                owner = owner
            )
        }
        return findings
    }

    private fun webViewIssue(
        code: String,
        title: String,
        description: String,
        severity: SecuritySeverity,
        owner: String
    ): SecurityIssue {
        return SecurityIssue(
            category = SecurityIssue.CATEGORY_WEBVIEW,
            code = code,
            title = title,
            description = description,
            severity = severity,
            target = "$owner#$code",
            timestamp = 0L
        )
    }
}
