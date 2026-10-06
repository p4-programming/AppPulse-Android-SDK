package io.apppulse.sdk.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.apppulse.sdk.R
import io.apppulse.sdk.crash.CrashEntity
import io.apppulse.sdk.crash.CrashViewModel
import kotlinx.coroutines.launch

/**
 * Scrollable debug screen for one stored crash.
 *
 * Shows the exception type, message, stack trace, device, Android version,
 * and app version. The report is loaded by id so a large stack trace is not
 * copied through the launching [Intent].
 */
class CrashDetailsActivity : AppCompatActivity() {

    private val viewModel: CrashViewModel by viewModels {
        CrashViewModel.Factory(application)
    }

    private val crashId: Long by lazy {
        intent.getLongExtra(EXTRA_CRASH_ID, MISSING_CRASH_ID)
    }

    /**
     * Binds the detail views and loads the crash identified by the intent extra.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_crash_details)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = getString(R.string.crash_details_title)

        if (crashId == MISSING_CRASH_ID) {
            showMissing()
            return
        }

        val content = bind<View>(R.id.crashDetailContent)
        val missing = bind<TextView>(R.id.crashDetailMissing)
        val deleteButton = bind<MaterialButton>(R.id.crashDetailDelete)
        var displayed: CrashEntity? = null

        deleteButton.setOnClickListener {
            val crash = displayed ?: return@setOnClickListener
            confirmDelete(crash)
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.selectedCrash.collect { selection ->
                        when (selection) {
                            CrashViewModel.SelectedCrash.Idle -> Unit
                            CrashViewModel.SelectedCrash.Missing -> {
                                displayed = null
                                content.visibility = View.GONE
                                missing.visibility = View.VISIBLE
                            }
                            is CrashViewModel.SelectedCrash.Found -> {
                                displayed = selection.crash
                                bind(selection.crash)
                                content.visibility = View.VISIBLE
                                missing.visibility = View.GONE
                            }
                        }
                    }
                }
                launch {
                    viewModel.crashDeleted.collect { deletedId ->
                        if (deletedId == crashId) {
                            finish()
                        }
                    }
                }
            }
        }
        viewModel.loadCrash(crashId)
    }

    /**
     * Closes the detail screen when the action bar up control is pressed.
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun bind(crash: CrashEntity) {
        bind<TextView>(R.id.crashDetailException).text = crash.exceptionType
        bind<TextView>(R.id.crashDetailMessage).text = crash.displayMessage()
        bind<TextView>(R.id.crashDetailStacktrace).text = crash.stackTrace.ifBlank {
            getString(R.string.crash_message_empty)
        }
        bind<TextView>(R.id.crashDetailDevice).text = crash.deviceLabel()
        bind<TextView>(R.id.crashDetailAndroidVersion).text = crash.androidVersion
        bind<TextView>(R.id.crashDetailAppVersion).text = crash.appVersion
        bind<TextView>(R.id.crashDetailTime).text = crash.formattedTimestamp()
    }

    private fun confirmDelete(crash: CrashEntity) {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.crash_delete_confirm_title)
            .setMessage(R.string.crash_delete_confirm_message)
            .setNegativeButton(R.string.crash_cancel, null)
            .setPositiveButton(R.string.crash_delete) { _, _ ->
                viewModel.deleteCrash(crash)
            }
            .show()
    }

    private fun showMissing() {
        bind<View>(R.id.crashDetailContent).visibility = View.GONE
        bind<TextView>(R.id.crashDetailMissing).visibility = View.VISIBLE
    }

    companion object {
        private const val EXTRA_CRASH_ID = "io.apppulse.sdk.extra.CRASH_ID"
        private const val MISSING_CRASH_ID = -1L

        /**
         * Creates an intent that opens the stored crash with [crashId].
         *
         * @param context Context used to build the intent.
         * @param crashId Primary key of the report to display.
         */
        fun createIntent(context: Context, crashId: Long): Intent {
            return Intent(context, CrashDetailsActivity::class.java)
                .putExtra(EXTRA_CRASH_ID, crashId)
        }
    }
}
