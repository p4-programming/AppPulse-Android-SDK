package io.apppulse.sdk.ui

import android.content.Intent
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
import io.apppulse.sdk.AppPulse
import io.apppulse.sdk.R
import io.apppulse.sdk.core.AppPulseLogger
import io.apppulse.sdk.health.HealthDashboardState
import io.apppulse.sdk.health.HealthExportFormat
import io.apppulse.sdk.health.HealthViewModel
import io.apppulse.sdk.health.model.HealthCategory
import io.apppulse.sdk.health.model.HealthLevel
import kotlinx.coroutines.launch

/**
 * Debug screen for the overall health score, category scores, and recommendations.
 *
 * A category row opens that module's dashboard. The detail button opens
 * [HealthDetailsActivity].
 */
class HealthDashboardActivity : AppCompatActivity() {

    private val viewModel: HealthViewModel by viewModels {
        HealthViewModel.Factory(application)
    }

    private val summaryAdapter = HealthSummaryAdapter { score ->
        when (score.category) {
            HealthCategory.CRASH -> AppPulse.openCrashDashboard(this)
            HealthCategory.PERFORMANCE -> AppPulse.openPerformanceDashboard(this)
            HealthCategory.MEMORY -> AppPulse.openMemoryDashboard(this)
            HealthCategory.SECURITY -> AppPulse.openSecurityDashboard(this)
        }
    }

    /**
     * Inflates the overall score, category list, and export actions.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_health_dashboard)
        title = getString(R.string.health_dashboard_title)

        val list = bind<RecyclerView>(R.id.healthSummaryList)
        list.layoutManager = LinearLayoutManager(this)
        list.adapter = summaryAdapter
        list.isNestedScrollingEnabled = false

        bind<MaterialButton>(R.id.healthRefresh).setOnClickListener {
            viewModel.refresh()
        }
        bind<MaterialButton>(R.id.healthViewDetails).setOnClickListener {
            startActivity(Intent(this, HealthDetailsActivity::class.java))
        }
        bind<MaterialButton>(R.id.healthExportJson).setOnClickListener {
            viewModel.export(HealthExportFormat.JSON)
        }
        bind<MaterialButton>(R.id.healthExportCsv).setOnClickListener {
            viewModel.export(HealthExportFormat.CSV)
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect(::render)
            }
        }
        AppPulseLogger.i(TAG, "Health dashboard opened")
    }

    /**
     * Reloads the cached report whenever the screen becomes visible.
     */
    override fun onStart() {
        super.onStart()
        viewModel.load()
    }

    private fun render(state: HealthDashboardState) {
        val report = state.report
        val scoreView = bind<TextView>(R.id.healthScore)
        val levelView = bind<TextView>(R.id.healthLevel)
        val recommendations = bind<TextView>(R.id.healthRecommendations)
        val exportResult = bind<TextView>(R.id.healthExportResult)
        bind<MaterialButton>(R.id.healthRefresh).isEnabled = !state.busy
        bind<MaterialButton>(R.id.healthExportJson).isEnabled = !state.busy
        bind<MaterialButton>(R.id.healthExportCsv).isEnabled = !state.busy

        if (report == null) {
            scoreView.text = getString(R.string.health_unavailable)
            levelView.text = ""
            recommendations.text = getString(R.string.health_none)
            summaryAdapter.submitList(emptyList())
        } else {
            scoreView.text = report.overallScore.toString()
            scoreView.setTextColor(ContextCompat.getColor(this, colorFor(report.overallStatus)))
            levelView.text = getString(
                R.string.health_status_trend,
                report.overallStatus.name,
                report.trend.name
            )
            recommendations.text = report.recommendations.joinToString(separator = "\n") { line ->
                getString(R.string.health_recommendation_line, line)
            }
            summaryAdapter.submitList(report.categories)
        }
        exportResult.visibility = if (state.exportMessage.isBlank()) View.GONE else View.VISIBLE
        exportResult.text = getString(R.string.health_exported, state.exportMessage)
    }

    private fun colorFor(level: HealthLevel): Int {
        return when (level) {
            HealthLevel.EXCELLENT, HealthLevel.GOOD -> R.color.purple_500
            HealthLevel.WARNING -> R.color.memory_warning
            HealthLevel.CRITICAL -> R.color.memory_critical
        }
    }

    private companion object {
        const val TAG = "AppPulseHealthUi"
    }
}
