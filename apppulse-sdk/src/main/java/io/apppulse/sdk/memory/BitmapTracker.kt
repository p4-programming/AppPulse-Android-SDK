package io.apppulse.sdk.memory

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import io.apppulse.sdk.core.AppPulseLogger
import io.apppulse.sdk.memory.model.BitmapMetric
import io.apppulse.sdk.storage.repository.MemoryRepository
import java.util.Collections
import java.util.IdentityHashMap
import kotlin.coroutines.resume
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Records bitmap allocations and bitmaps currently attached to started activities.
 *
 * A bitmap larger than 5 MB is flagged. View walks run on the main thread.
 *
 * @param repository Destination for bitmap rows.
 * @param scope Scope used to persist rows.
 */
internal class BitmapTracker(
    private val repository: MemoryRepository,
    private val scope: CoroutineScope
) {

    private val gate = Any()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val startedActivities = Collections.newSetFromMap(IdentityHashMap<Activity, Boolean>())
    private val explicitBitmaps = Collections.newSetFromMap(IdentityHashMap<Bitmap, Boolean>())
    private val explicitLarge = Collections.newSetFromMap(IdentityHashMap<Bitmap, Boolean>())

    /**
     * Number of explicitly tracked bitmaps that are still larger than 5 MB.
     */
    val explicitLargeCount: Int
        get() = synchronized(gate) { explicitLarge.size }

    /**
     * Remembers [activity] so the next scan can walk its view hierarchy.
     */
    fun addActivity(activity: Activity) {
        synchronized(gate) {
            startedActivities.add(activity)
        }
    }

    /**
     * Drops [activity] from future scans.
     */
    fun removeActivity(activity: Activity) {
        synchronized(gate) {
            startedActivities.remove(activity)
        }
    }

    /**
     * Records [bitmap] immediately. Repeated calls for the same instance are ignored.
     */
    fun track(bitmap: Bitmap) {
        val bytes = synchronized(gate) {
            if (bitmap.isRecycled || !explicitBitmaps.add(bitmap)) {
                return
            }
            val size = bitmap.allocationByteCount.toLong().coerceAtLeast(0L)
            if (size > LARGE_BITMAP_BYTES) {
                explicitLarge.add(bitmap)
            }
            size
        }
        val large = bytes > LARGE_BITMAP_BYTES
        if (large) {
            AppPulseLogger.w(TAG, "Bitmap Exceeded Threshold")
        }
        persist(
            BitmapMetric(
                timestamp = System.currentTimeMillis(),
                sizeMb = bytesToMb(bytes),
                bitmapCount = 1,
                largeCount = if (large) 1 else 0,
                largeBitmap = large
            )
        )
    }

    /**
     * Drops an explicitly tracked bitmap, for example after a validation bitmap is recycled.
     */
    fun release(bitmap: Bitmap) {
        synchronized(gate) {
            explicitBitmaps.remove(bitmap)
            explicitLarge.remove(bitmap)
        }
    }

    /**
     * Walks started activities and stores one snapshot of the bitmaps it finds.
     * Bitmaps already passed to [track] are not counted twice.
     *
     * @param onSnapshot Receives the snapshot even when nothing was found.
     */
    fun scan(onSnapshot: (BitmapMetric) -> Unit = {}) {
        scope.launch {
            val snapshot = withTimeoutOrNull(SCAN_TIMEOUT_MS) { captureOnMain() } ?: return@launch
            onSnapshot(snapshot)
            if (snapshot.bitmapCount == 0 && snapshot.largeCount == 0) {
                return@launch
            }
            if (snapshot.largeCount > 0) {
                AppPulseLogger.w(TAG, "Bitmap Exceeded Threshold")
            }
            try {
                repository.saveBitmapMetric(snapshot)
            } catch (throwable: Throwable) {
                AppPulseLogger.e(TAG, "Failed to store bitmap metric", throwable)
            }
        }
    }

    private suspend fun captureOnMain(): BitmapMetric {
        return suspendCancellableCoroutine { continuation ->
            mainHandler.post {
                if (!continuation.isActive) {
                    return@post
                }
                continuation.resume(scanHierarchy())
            }
        }
    }

    private fun scanHierarchy(): BitmapMetric {
        val activities: List<Activity>
        val alreadyTracked = IdentityHashMap<Bitmap, Boolean>()
        synchronized(gate) {
            activities = startedActivities.toList()
            explicitBitmaps.forEach { bitmap -> alreadyTracked[bitmap] = true }
        }
        var totalBytes = 0L
        var count = 0
        var largeCount = 0
        val seen = IdentityHashMap<Bitmap, Boolean>()
        activities.forEach { activity ->
            val root = activity.window?.decorView ?: return@forEach
            walk(root) { bitmap ->
                if (bitmap.isRecycled || seen.containsKey(bitmap) || alreadyTracked.contains(bitmap)) {
                    return@walk
                }
                seen[bitmap] = true
                val bytes = bitmap.allocationByteCount.toLong().coerceAtLeast(0L)
                totalBytes += bytes
                count += 1
                if (bytes > LARGE_BITMAP_BYTES) {
                    largeCount += 1
                }
            }
        }
        return BitmapMetric(
            timestamp = System.currentTimeMillis(),
            sizeMb = bytesToMb(totalBytes),
            bitmapCount = count,
            largeCount = largeCount,
            largeBitmap = largeCount > 0
        )
    }

    private fun walk(view: View, visit: (Bitmap) -> Unit) {
        collect(view.background, visit)
        if (view is ImageView) {
            collect(view.drawable, visit)
        }
        if (view is ViewGroup) {
            for (index in 0 until view.childCount) {
                walk(view.getChildAt(index), visit)
            }
        }
    }

    private fun collect(drawable: Drawable?, visit: (Bitmap) -> Unit) {
        when (drawable) {
            is BitmapDrawable -> drawable.bitmap?.let(visit)
            is LayerDrawable -> {
                for (index in 0 until drawable.numberOfLayers) {
                    collect(drawable.getDrawable(index), visit)
                }
            }
        }
    }

    private fun persist(metric: BitmapMetric) {
        scope.launch {
            try {
                repository.saveBitmapMetric(metric)
            } catch (throwable: Throwable) {
                AppPulseLogger.e(TAG, "Failed to store bitmap metric", throwable)
            }
        }
    }

    private companion object {
        const val TAG = "AppPulseMem"
        const val SCAN_TIMEOUT_MS = 2_000L
    }
}
