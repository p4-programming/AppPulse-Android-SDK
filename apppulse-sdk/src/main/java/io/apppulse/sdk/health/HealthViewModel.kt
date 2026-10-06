package io.apppulse.sdk.health

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.apppulse.sdk.AppPulse
import io.apppulse.sdk.core.AppPulseLogger
import io.apppulse.sdk.health.model.HealthReport
import io.apppulse.sdk.health.model.HealthSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Loads the unified health report for the health screens.
 *
 * @param application Host application.
 * @param exporter Writes JSON and CSV into Downloads.
 */
class HealthViewModel(
    application: Application,
    private val exporter: HealthReportExporter = HealthReportExporter(application)
) : AndroidViewModel(application) {

    private val stateHolder = MutableStateFlow(HealthDashboardState())

    /**
     * Latest report, history, and export status.
     */
    val state: StateFlow<HealthDashboardState> = stateHolder.asStateFlow()

    /**
     * Shows the cached report, generating one when none exists yet.
     */
    fun load() {
        viewModelScope.launch {
            publish(generateIfMissing = true)
        }
    }

    /**
     * Builds a new report from the current module data.
     */
    fun refresh() {
        viewModelScope.launch {
            stateHolder.value = stateHolder.value.copy(busy = true)
            try {
                AppPulse.generateHealthReportAwait()
            } catch (throwable: Throwable) {
                AppPulseLogger.e(TAG, "Health report failed", throwable)
            }
            publish(generateIfMissing = false)
        }
    }

    /**
     * Writes the current report to Downloads.
     */
    fun export(format: HealthExportFormat) {
        viewModelScope.launch {
            stateHolder.value = stateHolder.value.copy(busy = true, exportMessage = "")
            try {
                val manager = AppPulse.healthManager()
                val report = manager?.cached() ?: AppPulse.generateHealthReportAwait()
                val result = withContext(Dispatchers.IO) {
                    exporter.export(report, format)
                }
                publish(generateIfMissing = false)
                stateHolder.value = stateHolder.value.copy(
                    exportMessage = result.displayName
                )
            } catch (throwable: Throwable) {
                AppPulseLogger.e(TAG, "Health export failed", throwable)
                stateHolder.value = stateHolder.value.copy(
                    busy = false,
                    exportMessage = ""
                )
            }
        }
    }

    private suspend fun publish(generateIfMissing: Boolean) {
        try {
            val manager = AppPulse.healthManager()
            if (manager == null) {
                stateHolder.value = HealthDashboardState()
                return
            }
            if (generateIfMissing && manager.cached() == null) {
                manager.generate()
            }
            stateHolder.value = HealthDashboardState(
                report = manager.cached(),
                history = manager.history(),
                busy = false,
                exportMessage = stateHolder.value.exportMessage
            )
            AppPulseLogger.i(TAG, "Health dashboard loaded")
        } catch (throwable: Throwable) {
            AppPulseLogger.e(TAG, "Health dashboard failed to load", throwable)
            stateHolder.value = stateHolder.value.copy(busy = false)
        }
    }

    /**
     * Creates a [HealthViewModel].
     */
    class Factory(
        private val application: Application
    ) : ViewModelProvider.Factory {

        /**
         * Creates a [HealthViewModel].
         *
         * @throws IllegalArgumentException when [modelClass] is not a [HealthViewModel].
         */
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(HealthViewModel::class.java)) {
                "Factory can only create HealthViewModel"
            }
            @Suppress("UNCHECKED_CAST")
            return HealthViewModel(application) as T
        }
    }

    private companion object {
        const val TAG = "AppPulseHealth"
    }
}

/**
 * Figures shown by the health dashboard and detail screen.
 *
 * @property report Latest report, or null before the first successful load.
 * @property history Stored reports, oldest first.
 * @property busy True while a refresh or export is running.
 * @property exportMessage File name of the last successful export.
 */
data class HealthDashboardState(
    val report: HealthReport? = null,
    val history: List<HealthSnapshot> = emptyList(),
    val busy: Boolean = false,
    val exportMessage: String = ""
)
