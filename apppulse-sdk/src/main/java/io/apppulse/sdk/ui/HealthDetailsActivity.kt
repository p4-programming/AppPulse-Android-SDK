package io.apppulse.sdk.ui

import android.os.Bundle
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.apppulse.sdk.R
import io.apppulse.sdk.core.AppPulseLogger
import io.apppulse.sdk.health.HealthDashboardState
import io.apppulse.sdk.health.HealthViewModel
import io.apppulse.sdk.health.model.HealthLevel
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

/**
 * Score breakdown, recommendations, and health-score trend.
 */
class HealthDetailsActivity : AppCompatActivity() {

    private val viewModel: HealthViewModel by viewModels {
        HealthViewModel.Factory(application)
    }

    private val summaryAdapter = HealthSummaryAdapter { }

    /**
     * Inflates the breakdown, recommendations, and trend.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_health_details)
        title = getString(R.string.health_details_title)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val breakdown = bind<RecyclerView>(R.id.healthBreakdownList)
        breakdown.layoutManager = LinearLayoutManager(this)
        breakdown.adapter = summaryAdapter
        breakdown.isNestedScrollingEnabled = false

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect(::render)
            }
        }
        AppPulseLogger.i(TAG, "Health details opened")
    }

    /**
     * Reloads the cached report whenever the screen becomes visible.
     */
    override fun onStart() {
        super.onStart()
        viewModel.load()
    }

    /**
     * Closes the detail screen.
     */
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun render(state: HealthDashboardState) {
        val report = state.report
        val scoreView = bind<TextView>(R.id.healthDetailScore)
        val levelView = bind<TextView>(R.id.healthDetailLevel)
        val recommendations = bind<TextView>(R.id.healthDetailRecommendations)
        val trend = bind<TextView>(R.id.healthTrend)
        if (report == null) {
            scoreView.text = getString(R.string.health_unavailable)
            levelView.text = ""
            recommendations.text = getString(R.string.health_none)
            trend.text = getString(R.string.health_none)
            summaryAdapter.submitList(emptyList())
            return
        }
        scoreView.text = report.overallScore.toString()
        scoreView.setTextColor(ContextCompat.getColor(this, colorFor(report.overallStatus)))
        levelView.text = getString(
            R.string.health_status_trend,
            report.overallStatus.name,
            report.trend.name
        )
        summaryAdapter.submitList(report.categories)
        recommendations.text = report.recommendations.joinToString(separator = "\n") { line ->
            getString(R.string.health_recommendation_line, line)
        }
        trend.text = if (state.history.isEmpty()) {
            getString(R.string.health_none)
        } else {
            state.history.joinToString(separator = "\n") { snapshot ->
                getString(
                    R.string.health_trend_line,
                    formatWhen(snapshot.generatedAt),
                    snapshot.overallScore,
                    snapshot.overallStatus.name
                )
            }
        }
    }

    private fun colorFor(level: HealthLevel): Int {
        return when (level) {
            HealthLevel.EXCELLENT, HealthLevel.GOOD -> R.color.purple_500
            HealthLevel.WARNING -> R.color.memory_warning
            HealthLevel.CRITICAL -> R.color.memory_critical
        }
    }

    private fun formatWhen(epochMillis: Long): String {
        return DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
            .format(Date(epochMillis))
    }

    private companion object {
        const val TAG = "AppPulseHealthUi"
    }
}
