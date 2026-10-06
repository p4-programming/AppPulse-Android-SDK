package io.apppulse.sdk.ui

import android.os.Bundle
import android.view.MenuItem
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
import io.apppulse.sdk.R
import io.apppulse.sdk.memory.MemoryDashboardState
import io.apppulse.sdk.memory.MemoryViewModel
import io.apppulse.sdk.memory.model.MemoryHealthLevel
import kotlinx.coroutines.launch

/**
 * Scrollable memory detail screen.
 *
 * Shows the health score, heap timeline, leak candidates, and bitmap statistics.
 */
class MemoryDetailsActivity : AppCompatActivity() {

    private val viewModel: MemoryViewModel by viewModels {
        MemoryViewModel.Factory(application)
    }

    private val memoryAdapter = MemoryAdapter()
    private val leakAdapter = LeakAdapter()

    /**
     * Inflates the timeline and leak lists.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_memory_details)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = getString(R.string.memory_details_title)

        bindList(R.id.memoryTimelineList, memoryAdapter)
        bindList(R.id.memoryDetailLeakList, leakAdapter)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect(::render)
            }
        }
    }

    /**
     * Reloads memory data when the screen becomes visible.
     */
    override fun onStart() {
        super.onStart()
        viewModel.load()
    }

    /**
     * Closes the screen from the action bar up control.
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun render(state: MemoryDashboardState) {
        val unavailable = getString(R.string.memory_unavailable)
        val health = state.health
        val scoreView = bind<TextView>(R.id.memoryDetailScore)
        val levelView = bind<TextView>(R.id.memoryDetailLevel)
        if (health == null) {
            scoreView.text = unavailable
            levelView.text = ""
        } else {
            scoreView.text = health.score.toString()
            scoreView.setTextColor(healthColor(health.level))
            levelView.text = buildString {
                append(
                    getString(
                        R.string.memory_level,
                        health.level.name,
                        health.leakPenalty + health.memoryPenalty + health.bitmapPenalty
                    )
                )
                append('\n')
                append(getString(R.string.memory_trend, health.trend.name))
                append("\nLeaks -")
                append(health.leakPenalty)
                append(" · Heap -")
                append(health.memoryPenalty)
                append(" · Bitmaps -")
                append(health.bitmapPenalty)
            }
        }

        memoryAdapter.submitList(state.timeline)
        bind<TextView>(R.id.memoryTimelineEmpty).visibility =
            if (state.timeline.isEmpty()) View.VISIBLE else View.GONE

        leakAdapter.submitList(state.leaks)
        bind<TextView>(R.id.memoryDetailLeakEmpty).visibility =
            if (state.leaks.isEmpty()) View.VISIBLE else View.GONE

        val bitmap = state.bitmaps.firstOrNull()
        val largeEvents = state.bitmaps.count { it.largeBitmap }
        bind<TextView>(R.id.memoryBitmapStats).text = if (bitmap == null) {
            unavailable
        } else {
            getString(
                R.string.memory_bitmap_summary,
                bitmap.bitmapCount,
                bitmap.sizeMb,
                bitmap.largeCount
            ) + "\nLarge samples: $largeEvents"
        }
    }

    private fun bindList(viewId: Int, adapter: RecyclerView.Adapter<*>) {
        val list = bind<RecyclerView>(viewId)
        list.layoutManager = LinearLayoutManager(this)
        list.adapter = adapter
        list.isNestedScrollingEnabled = false
    }

    private fun healthColor(level: MemoryHealthLevel): Int {
        val color = when (level) {
            MemoryHealthLevel.EXCELLENT, MemoryHealthLevel.GOOD -> R.color.purple_500
            MemoryHealthLevel.WARNING -> R.color.memory_warning
            MemoryHealthLevel.CRITICAL -> R.color.memory_critical
        }
        return ContextCompat.getColor(this, color)
    }
}
