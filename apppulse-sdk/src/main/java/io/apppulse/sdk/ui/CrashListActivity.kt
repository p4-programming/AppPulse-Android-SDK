package io.apppulse.sdk.ui

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.apppulse.sdk.R
import io.apppulse.sdk.core.AppPulseLogger
import io.apppulse.sdk.crash.CrashViewModel
import kotlinx.coroutines.launch

/**
 * Debug screen that lists crashes stored in `apppulse.db`.
 *
 * A row shows the exception type, message, and date. Tapping a row opens
 * [CrashDetailsActivity]. The options menu can delete every stored crash.
 */
class CrashListActivity : AppCompatActivity() {

    private val viewModel: CrashViewModel by viewModels {
        CrashViewModel.Factory(application)
    }

    private val adapter = CrashListAdapter { crash ->
        startActivity(CrashDetailsActivity.createIntent(this, crash.id))
    }

    private lateinit var emptyView: View

    /**
     * Inflates the list, starts collecting [CrashViewModel.crashes], and loads
     * the current rows.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_crash_list)
        title = getString(R.string.crash_list_title)

        emptyView = bind(R.id.crashEmptyView)
        val recyclerView = bind<RecyclerView>(R.id.crashRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.crashes.collect { crashes ->
                    adapter.submitList(crashes)
                    emptyView.visibility = if (crashes.isEmpty()) View.VISIBLE else View.GONE
                    invalidateOptionsMenu()
                }
            }
        }
        AppPulseLogger.i(TAG, "Crash dashboard opened")
    }

    /**
     * Reloads crashes whenever the list becomes visible again, including after
     * a report is deleted on the detail screen.
     */
    override fun onStart() {
        super.onStart()
        viewModel.loadCrashes()
    }

    /**
     * Adds the clear-all action.
     */
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_crash_list, menu)
        return true
    }

    /**
     * Disables clear-all while the list is empty.
     */
    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        menu.findItem(R.id.action_clear_all)?.isEnabled = viewModel.crashes.value.isNotEmpty()
        return super.onPrepareOptionsMenu(menu)
    }

    /**
     * Confirms, then deletes every stored crash.
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId != R.id.action_clear_all) {
            return super.onOptionsItemSelected(item)
        }
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.crash_clear_confirm_title)
            .setMessage(R.string.crash_clear_confirm_message)
            .setNegativeButton(R.string.crash_cancel, null)
            .setPositiveButton(R.string.crash_clear_all) { _, _ ->
                viewModel.clearAllCrashes()
            }
            .show()
        return true
    }

    private companion object {
        const val TAG = "AppPulseCrash"
    }
}
