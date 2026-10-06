package io.apppulse.sdk.memory.model

/**
 * An activity or fragment that was still reachable after destruction and a GC attempt.
 *
 * @property id Stored row id. `0` before insert.
 * @property componentName Simple class name of the leaked component.
 * @property componentType `Activity` or `Fragment`.
 * @property destroyedTimestamp Wall-clock time when the component was destroyed.
 * @property detectedTimestamp Wall-clock time when the reference was still alive.
 */
data class LeakCandidate(
    val id: Long = 0,
    val componentName: String,
    val componentType: String,
    val destroyedTimestamp: Long,
    val detectedTimestamp: Long
) {
    companion object {
        const val TYPE_ACTIVITY = "Activity"
        const val TYPE_FRAGMENT = "Fragment"
    }
}
