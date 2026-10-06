package io.apppulse.sample

import android.app.Application
import io.apppulse.sdk.AppPulse

/**
 * Host application for the AppPulse sample.
 */
class SampleApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        AppPulse.init(this)
    }
}
