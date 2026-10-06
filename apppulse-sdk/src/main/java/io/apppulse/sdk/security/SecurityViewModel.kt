package io.apppulse.sdk.security

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.apppulse.sdk.AppPulse
import io.apppulse.sdk.core.AppPulseLogger
import io.apppulse.sdk.security.model.SecurityReport
import io.apppulse.sdk.storage.AppPulseDatabase
import io.apppulse.sdk.storage.repository.SecurityRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Loads the latest security scan for [io.apppulse.sdk.ui.SecurityDashboardActivity].
 *
 * @param application Host application.
 * @param repository Security finding storage.
 * @param calculator Score calculator. A future Hilt binding can replace it.
 */
class SecurityViewModel(
    application: Application,
    private val repository: SecurityRepository,
    private val calculator: SecurityHealthCalculator = SecurityHealthCalculator()
) : AndroidViewModel(application) {

    private val stateHolder = MutableStateFlow(SecurityDashboardState())

    /**
     * Latest scan. Empty until [load] succeeds.
     */
    val state: StateFlow<SecurityDashboardState> = stateHolder.asStateFlow()

    /**
     * Reads stored findings and publishes [state].
     */
    fun load() {
        viewModelScope.launch {
            publish(scanning = stateHolder.value.scanning)
        }
    }

    /**
     * Runs a new scan, then reloads the dashboard.
     */
    fun rescan() {
        viewModelScope.launch {
            stateHolder.value = stateHolder.value.copy(scanning = true)
            try {
                AppPulse.awaitSecurityScan()
            } catch (throwable: Throwable) {
                AppPulseLogger.e(TAG, "Security scan failed", throwable)
            }
            publish(scanning = false)
        }
    }

    private suspend fun publish(scanning: Boolean) {
        try {
            val issues = repository.getIssues()
            val score = calculator.calculate(issues)
            stateHolder.value = SecurityDashboardState(
                report = SecurityReport(
                    scannedAt = issues.maxOfOrNull { it.timestamp } ?: 0L,
                    issues = issues,
                    score = score
                ),
                scanning = scanning
            )
            AppPulseLogger.i(TAG, "Security dashboard loaded. Security Score = ${score.score}")
        } catch (throwable: Throwable) {
            AppPulseLogger.e(TAG, "Security dashboard failed to load", throwable)
            stateHolder.value = stateHolder.value.copy(scanning = scanning)
        }
    }

    /**
     * Creates a [SecurityViewModel] bound to the process database.
     */
    class Factory(
        private val application: Application
    ) : ViewModelProvider.Factory {

        /**
         * Creates a [SecurityViewModel].
         *
         * @throws IllegalArgumentException when [modelClass] is not a [SecurityViewModel].
         */
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(SecurityViewModel::class.java)) {
                "Factory can only create SecurityViewModel"
            }
            val database = AppPulseDatabase.getInstance(application)
            val repository = SecurityRepository(database.securityIssueDao())
            @Suppress("UNCHECKED_CAST")
            return SecurityViewModel(application, repository) as T
        }
    }

    private companion object {
        const val TAG = "AppPulseSec"
    }
}

/**
 * Figures shown by the security dashboard.
 *
 * @property report Latest stored scan, or null before the first successful load.
 * @property scanning True while [SecurityViewModel.rescan] is running.
 */
data class SecurityDashboardState(
    val report: SecurityReport? = null,
    val scanning: Boolean = false
)
