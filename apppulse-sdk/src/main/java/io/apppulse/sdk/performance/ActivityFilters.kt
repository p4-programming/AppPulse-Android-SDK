package io.apppulse.sdk.performance

import android.app.Activity

/**
 * Host screens are measured. AppPulse's own debug activities are not, so the
 * dashboards do not record themselves.
 */
internal fun Activity.shouldMeasure(): Boolean {
    return !javaClass.name.startsWith(SDK_UI_PREFIX)
}

private const val SDK_UI_PREFIX = "io.apppulse.sdk.ui."
