package io.apppulse.sdk.memory

import android.app.Activity
import android.graphics.Bitmap
import androidx.fragment.app.Fragment
import io.apppulse.sdk.core.AppPulseLogger
import java.util.Collections

/**
 * Holds strong references on purpose so the leak detector and bitmap tracker
 * can be validated. Call [clear] when the check is finished so the process
 * can release the retained objects.
 *
 * @param bitmapTracker Tracker that records the validation bitmap.
 */
internal class MemorySamples(
    private val bitmapTracker: BitmapTracker
) {

    private val activities = Collections.synchronizedList(mutableListOf<Activity>())
    private val fragments = Collections.synchronizedList(mutableListOf<Fragment>())
    private val bitmaps = Collections.synchronizedList(mutableListOf<Bitmap>())

    /**
     * Retains [activity] after the caller destroys it.
     * The leak check then finds the activity still reachable.
     */
    fun leakActivity(activity: Activity) {
        activities.add(activity)
        AppPulseLogger.w(TAG, "Validation activity retained: ${activity.javaClass.simpleName}")
    }

    /**
     * Retains [fragment] after the caller removes it.
     * The leak check then finds the fragment still reachable.
     */
    fun leakFragment(fragment: Fragment) {
        fragments.add(fragment)
        AppPulseLogger.w(TAG, "Validation fragment retained: ${fragment.javaClass.simpleName}")
    }

    /**
     * Allocates a bitmap larger than 5 MB and records it.
     * The bitmap stays reachable until [clear].
     */
    fun createLargeBitmap(): Bitmap {
        val bitmap = Bitmap.createBitmap(LARGE_BITMAP_WIDTH, LARGE_BITMAP_HEIGHT, Bitmap.Config.ARGB_8888)
        bitmaps.add(bitmap)
        bitmapTracker.track(bitmap)
        AppPulseLogger.w(TAG, "Validation bitmap created")
        return bitmap
    }

    /**
     * Drops validation references and recycles validation bitmaps.
     */
    fun clear() {
        activities.clear()
        fragments.clear()
        val held = bitmaps.toList()
        bitmaps.clear()
        held.forEach { bitmap ->
            bitmapTracker.release(bitmap)
            if (!bitmap.isRecycled) {
                bitmap.recycle()
            }
        }
        AppPulseLogger.i(TAG, "Validation memory references cleared")
    }

    private companion object {
        const val TAG = "AppPulseMem"
        const val LARGE_BITMAP_WIDTH = 1400
        const val LARGE_BITMAP_HEIGHT = 1400
    }
}
