package io.apppulse.sdk.ui

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.apppulse.sdk.R
import io.apppulse.sdk.core.AppPulseLogger
import io.apppulse.sdk.performance.PerformanceViewModel
import kotlinx.coroutines.launch

/**
 * Debug screen for startup, screen, network, FPS, and ANR metrics stored in `apppulse.db`.
 *
 * The first row is the summary card. The sections below it are startup metrics,
 * slow screens, network calls, FPS samples, and ANRs.
 */
class PerformanceDashboardActivity : AppCompatActivity() {

    private val viewModel: PerformanceViewModel by viewModels {
        PerformanceViewModel.Factory(application)
    }

    private val adapter = PerformanceDashboardAdapter()

    /**
     * Inflates the list and starts collecting [PerformanceViewModel.rows].
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_performance_dashboard)
        title = getString(R.string.performance_dashboard_title)

        val recyclerView = bind<RecyclerView>(R.id.performanceRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.rows.collect { rows ->
                    adapter.submitList(rows)
                }
            }
        }
        AppPulseLogger.i(TAG, "Performance dashboard opened")
    }

    /**
     * Reloads metrics whenever the dashboard becomes visible.
     */
    override fun onStart() {
        super.onStart()
        viewModel.load()
    }

    private companion object {
        const val TAG = "AppPulsePerf"
    }
}
