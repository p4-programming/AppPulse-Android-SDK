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
import io.apppulse.sdk.R
import io.apppulse.sdk.core.AppPulseLogger
import io.apppulse.sdk.memory.MemoryDashboardState
import io.apppulse.sdk.memory.MemoryViewModel
import io.apppulse.sdk.memory.model.MemoryHealthLevel
import kotlinx.coroutines.launch

/**
 * Debug screen for the current heap, peaks, leaks, bitmaps, and health score.
 *
 * The health card opens [MemoryDetailsActivity].
 */
class MemoryDashboardActivity : AppCompatActivity() {

    private val viewModel: MemoryViewModel by viewModels {
        MemoryViewModel.Factory(application)
    }

    private val leakAdapter = LeakAdapter()

    /**
     * Inflates the summary and starts collecting [MemoryViewModel.state].
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_memory_dashboard)
        title = getString(R.string.memory_dashboard_title)

        val leaks = bind<RecyclerView>(R.id.memoryLeakList)
        leaks.layoutManager = LinearLayoutManager(this)
        leaks.adapter = leakAdapter
        leaks.isNestedScrollingEnabled = false

        bind<View>(R.id.memoryHealthCard).setOnClickListener {
            openDetails()
        }
        bind<MaterialButton>(R.id.memoryViewDetails).setOnClickListener {
            openDetails()
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect(::render)
            }
        }
        AppPulseLogger.i(TAG, "Memory dashboard opened")
    }

    /**
     * Reloads stored memory data whenever the screen becomes visible.
     */
    override fun onStart() {
        super.onStart()
        viewModel.load()
    }

    private fun render(state: MemoryDashboardState) {
        val unavailable = getString(R.string.memory_unavailable)
        bind<TextView>(R.id.memoryCurrentValue).text = state.current?.let {
            getString(R.string.memory_megabytes, it.usedMemoryMb)
        } ?: unavailable
        bind<TextView>(R.id.memoryCurrentDetail).text = state.current?.let {
            getString(R.string.memory_native, it.nativeMemoryMb)
        } ?: ""
        bind<TextView>(R.id.memoryPeakValue).text = state.peakMb?.let {
            getString(R.string.memory_megabytes, it)
        } ?: unavailable
        bind<TextView>(R.id.memoryAverageValue).text = state.averageMb?.let {
            getString(R.string.memory_megabytes, it)
        } ?: unavailable

        val health = state.health
        val scoreView = bind<TextView>(R.id.memoryHealthScore)
        val levelView = bind<TextView>(R.id.memoryHealthLevel)
        if (health == null) {
            scoreView.text = unavailable
            levelView.text = ""
        } else {
            scoreView.text = health.score.toString()
            scoreView.setTextColor(healthColor(health.level))
            levelView.text = getString(
                R.string.memory_level,
                health.level.name,
                health.leakPenalty + health.memoryPenalty + health.bitmapPenalty
            ) + "\n" + getString(R.string.memory_trend, health.trend.name)
        }

        val bitmap = state.bitmaps.firstOrNull()
        bind<TextView>(R.id.memoryBitmapValue).text = if (bitmap == null) {
            unavailable
        } else {
            getString(
                R.string.memory_bitmap_summary,
                bitmap.bitmapCount,
                bitmap.sizeMb,
                bitmap.largeCount
            )
        }

        leakAdapter.submitList(state.leaks)
        bind<TextView>(R.id.memoryLeakEmpty).visibility =
            if (state.leaks.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun healthColor(level: MemoryHealthLevel): Int {
        val color = when (level) {
            MemoryHealthLevel.EXCELLENT, MemoryHealthLevel.GOOD -> R.color.purple_500
            MemoryHealthLevel.WARNING -> R.color.memory_warning
            MemoryHealthLevel.CRITICAL -> R.color.memory_critical
        }
        return ContextCompat.getColor(this, color)
    }

    private fun openDetails() {
        startActivity(Intent(this, MemoryDetailsActivity::class.java))
    }

    private companion object {
        const val TAG = "AppPulseMem"
    }
}
