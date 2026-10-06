package io.apppulse.sample

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import io.apppulse.sdk.AppPulse

/**
 * Retains itself through the SDK, then finishes so the leak detector can observe it.
 */
class LeakTestActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppPulse.simulateActivityLeak(this)
        finish()
    }
}
