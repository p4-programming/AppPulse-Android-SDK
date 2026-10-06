package io.apppulse.sdk.memory

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import io.apppulse.sdk.memory.model.LeakCandidate
import java.lang.ref.WeakReference

/**
 * Watches fragments destroyed by a [FragmentManager].
 *
 * Fragments destroyed because the host activity is changing configuration are
 * ignored, because the framework replaces them immediately.
 *
 * @param detector Checker that decides whether the fragment is still reachable.
 */
internal class FragmentLeakTracker(
    private val detector: LeakDetector
) : FragmentManager.FragmentLifecycleCallbacks() {

    /**
     * Starts a leak check for [fragment] when it is destroyed.
     */
    override fun onFragmentDestroyed(fm: FragmentManager, fragment: Fragment) {
        if (fragment.activity?.isChangingConfigurations == true) {
            return
        }
        val name = fragment.javaClass.simpleName.ifBlank { LeakCandidate.TYPE_FRAGMENT }
        val reference = WeakReference<Any>(fragment)
        detector.watch(
            componentName = name,
            componentType = LeakCandidate.TYPE_FRAGMENT,
            reference = reference
        )
    }
}
