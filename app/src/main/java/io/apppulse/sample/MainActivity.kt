package io.apppulse.sample

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.annotation.IdRes
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import io.apppulse.sdk.AppPulse

/**
 * Exercises the public AppPulse APIs from a host application.
 */
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        title = getString(R.string.app_name)

        bind<MaterialButton>(R.id.crashTest).setOnClickListener {
            throw RuntimeException("AppPulse sample crash")
        }
        bind<MaterialButton>(R.id.anrTest).setOnClickListener {
            Thread.sleep(6_000)
            Toast.makeText(this, R.string.sample_anr_finished, Toast.LENGTH_SHORT).show()
        }
        bind<MaterialButton>(R.id.memoryLeakTest).setOnClickListener {
            startActivity(android.content.Intent(this, LeakTestActivity::class.java))
            Toast.makeText(this, R.string.sample_leak_started, Toast.LENGTH_LONG).show()
        }
        bind<MaterialButton>(R.id.securityScanTest).setOnClickListener {
            AppPulse.runSecurityScan()
            AppPulse.openSecurityDashboard(this)
        }
        bind<MaterialButton>(R.id.openHealthDashboard).setOnClickListener {
            AppPulse.getHealthScore()
            AppPulse.generateHealthReport()
            AppPulse.openHealthDashboard(this)
        }
    }

    private fun <T : View> bind(@IdRes id: Int): T {
        return requireNotNull(findViewById(id))
    }
}
