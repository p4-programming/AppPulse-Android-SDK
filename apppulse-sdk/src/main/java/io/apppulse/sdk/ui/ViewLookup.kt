package io.apppulse.sdk.ui

import android.app.Activity
import android.view.View
import androidx.annotation.IdRes

/**
 * Returns the view for [id], or throws if the layout does not contain it.
 *
 * [Activity.findViewById] is nullable. These helpers give the activities a
 * non-null view after [Activity.setContentView].
 */
internal fun <T : View> Activity.bind(@IdRes id: Int): T {
    return requireNotNull(findViewById(id)) {
        "Missing view ${resources.getResourceEntryName(id)}"
    }
}

/**
 * Returns the child for [id], or throws if this view does not contain it.
 */
internal fun <T : View> View.bind(@IdRes id: Int): T {
    return requireNotNull(findViewById(id)) {
        "Missing view ${resources.getResourceEntryName(id)}"
    }
}
