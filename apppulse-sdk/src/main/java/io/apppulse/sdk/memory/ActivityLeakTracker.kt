package io.apppulse.sdk.memory

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import io.apppulse.sdk.memory.model.LeakCandidate
import java.lang.ref.WeakReference
import java.util.IdentityHashMap

/**
 * Registers activity and fragment destroy hooks for leak checks.
 *
 * Activities destroyed for a configuration change are not watched.
 *
 * @param application Host application.
 * @param detector Checker used after an activity is destroyed.
 * @param fragmentTracker Callbacks installed on each [FragmentActivity].
 * @param onActivityStarted Called when a host activity starts, for bitmap scans.
 * @param onActivityStopped Called when that activity stops.
 */
internal class ActivityLeakTracker(
    private val application: Application,
    private val detector: LeakDetector,
    private val fragmentTracker: FragmentLeakTracker,
    private val activityStarted: (Activity) -> Unit,
    private val activityStopped: (Activity) -> Unit
) : Application.ActivityLifecycleCallbacks {

    private val fragmentManagers = IdentityHashMap<Activity, androidx.fragment.app.FragmentManager>()
    private var registered = false

    /**
     * Installs the process-wide activity callbacks.
     */
    fun start() {
        if (registered) {
            return
        }
        registered = true
        application.registerActivityLifecycleCallbacks(this)
    }

    /**
     * Removes callbacks and fragment watches that were installed by [start].
     */
    fun stop() {
        if (!registered) {
            return
        }
        registered = false
        application.unregisterActivityLifecycleCallbacks(this)
        fragmentManagers.entries.toList().forEach { (activity, manager) ->
            manager.unregisterFragmentLifecycleCallbacks(fragmentTracker)
            activityStopped(activity)
        }
        fragmentManagers.clear()
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        if (activity !is FragmentActivity) {
            return
        }
        val manager = activity.supportFragmentManager
        manager.registerFragmentLifecycleCallbacks(fragmentTracker, true)
        fragmentManagers[activity] = manager
    }

    override fun onActivityStarted(activity: Activity) {
        activityStarted(activity)
    }

    override fun onActivityStopped(activity: Activity) {
        activityStopped(activity)
    }

    override fun onActivityDestroyed(activity: Activity) {
        fragmentManagers.remove(activity)?.unregisterFragmentLifecycleCallbacks(fragmentTracker)
        activityStopped(activity)
        if (activity.isChangingConfigurations) {
            return
        }
        val name = activity.javaClass.simpleName.ifBlank { LeakCandidate.TYPE_ACTIVITY }
        val reference = WeakReference<Any>(activity)
        detector.watch(
            componentName = name,
            componentType = LeakCandidate.TYPE_ACTIVITY,
            reference = reference
        )
    }

    override fun onActivityResumed(activity: Activity) = Unit

    override fun onActivityPaused(activity: Activity) = Unit

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
}
