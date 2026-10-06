package io.apppulse.sdk.memory

import io.apppulse.sdk.core.AppPulseLogger
import io.apppulse.sdk.memory.model.LeakCandidate
import io.apppulse.sdk.storage.repository.MemoryRepository
import java.lang.ref.WeakReference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield

/**
 * Checks whether a destroyed component is still reachable after a GC and a wait.
 *
 * The caller passes a [WeakReference] only. This detector does not keep a
 * strong reference to the component.
 *
 * @param repository Destination for confirmed candidates.
 * @param scope Scope used for the wait and the database write.
 * @param onLeak Called after a candidate is stored.
 */
internal class LeakDetector(
    private val repository: MemoryRepository,
    private val scope: CoroutineScope,
    private val onLeak: () -> Unit
) {

    /**
     * Watches [reference] after the component named [componentName] is destroyed.
     *
     * The check forces a GC, waits five seconds, forces GC again, and stores a
     * candidate when the reference is still alive.
     *
     * @param componentType [LeakCandidate.TYPE_ACTIVITY] or [LeakCandidate.TYPE_FRAGMENT].
     */
    fun watch(
        componentName: String,
        componentType: String,
        reference: WeakReference<Any>
    ) {
        val destroyedAt = System.currentTimeMillis()
        scope.launch {
            yield()
            Runtime.getRuntime().gc()
            System.runFinalization()
            delay(LEAK_CHECK_DELAY_MS)
            Runtime.getRuntime().gc()
            if (reference.get() == null) {
                return@launch
            }
            val candidate = LeakCandidate(
                componentName = componentName.ifBlank { componentType },
                componentType = componentType,
                destroyedTimestamp = destroyedAt,
                detectedTimestamp = System.currentTimeMillis()
            )
            try {
                repository.saveLeak(candidate)
                AppPulseLogger.w(TAG, "Leak Candidate Found: $componentType ${candidate.componentName}")
                onLeak()
            } catch (throwable: Throwable) {
                AppPulseLogger.e(TAG, "Failed to store leak candidate", throwable)
            }
        }
    }

    private companion object {
        const val TAG = "AppPulseMem"
    }
}
