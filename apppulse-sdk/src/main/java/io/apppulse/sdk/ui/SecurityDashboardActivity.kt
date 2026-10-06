package io.apppulse.sdk.ui

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import io.apppulse.sdk.R
import io.apppulse.sdk.core.AppPulseLogger
import io.apppulse.sdk.security.SecurityDashboardState
import io.apppulse.sdk.security.SecurityViewModel
import io.apppulse.sdk.security.model.SecurityLevel
import kotlinx.coroutines.launch

/**
 * Debug screen for the latest security score and findings.
 */
class SecurityDashboardActivity : AppCompatActivity() {

    private val viewModel: SecurityViewModel by viewModels {
        SecurityViewModel.Factory(application)
    }

    private val issueAdapter = SecurityIssueAdapter()

    /**
     * Inflates the score, counts, and finding list.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_security_dashboard)
        title = getString(R.string.security_dashboard_title)

        val issues = bind<RecyclerView>(R.id.securityIssueList)
        issues.layoutManager = LinearLayoutManager(this)
        issues.adapter = issueAdapter
        issues.isNestedScrollingEnabled = false

        bind<MaterialButton>(R.id.securityRescan).setOnClickListener {
            viewModel.rescan()
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect(::render)
            }
        }
        AppPulseLogger.i(TAG, "Security dashboard opened")
    }

    /**
     * Reloads stored findings whenever the screen becomes visible.
     */
    override fun onStart() {
        super.onStart()
        viewModel.load()
    }

    private fun render(state: SecurityDashboardState) {
        val report = state.report
        val scoreView = bind<TextView>(R.id.securityScore)
        val levelView = bind<TextView>(R.id.securityLevel)
        val empty = bind<TextView>(R.id.securityEmpty)
        val rescan = bind<MaterialButton>(R.id.securityRescan)
        rescan.isEnabled = !state.scanning
        rescan.text = getString(if (state.scanning) R.string.security_scanning else R.string.security_rescan)

        if (report == null) {
            scoreView.text = getString(R.string.security_unavailable)
            levelView.text = ""
            issueAdapter.submitList(emptyList())
            empty.visibility = View.VISIBLE
            return
        }

        val score = report.score
        scoreView.text = score.score.toString()
        scoreView.setTextColor(ContextCompat.getColor(this, colorFor(score.level)))
        levelView.text = getString(R.string.security_level, score.level.name)
        bind<TextView>(R.id.securityCriticalCount).text =
            getString(R.string.security_critical_count, score.criticalCount)
        bind<TextView>(R.id.securityHighCount).text =
            getString(R.string.security_high_count, score.highCount)
        bind<TextView>(R.id.securityMediumCount).text =
            getString(R.string.security_medium_count, score.mediumCount)
        bind<TextView>(R.id.securityLowCount).text =
            getString(R.string.security_low_count, score.lowCount)
        issueAdapter.submitList(report.issues)
        empty.visibility = if (report.issues.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun colorFor(level: SecurityLevel): Int {
        return when (level) {
            SecurityLevel.EXCELLENT, SecurityLevel.GOOD -> R.color.purple_500
            SecurityLevel.WARNING -> R.color.memory_warning
            SecurityLevel.CRITICAL -> R.color.memory_critical
        }
    }

    private companion object {
        const val TAG = "AppPulseSecUi"
    }
}
