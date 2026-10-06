package io.apppulse.sdk.performance

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.apppulse.sdk.R
import io.apppulse.sdk.core.AppPulseLogger
import io.apppulse.sdk.performance.model.AnrMetric
import io.apppulse.sdk.performance.model.FpsMetric
import io.apppulse.sdk.performance.model.NetworkMetric
import io.apppulse.sdk.performance.model.PerformanceReport
import io.apppulse.sdk.performance.model.ScreenMetric
import io.apppulse.sdk.performance.model.StartupMetric
import io.apppulse.sdk.storage.AppPulseDatabase
import io.apppulse.sdk.storage.repository.PerformanceRepository
import java.text.DateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Loads stored performance metrics into rows for [io.apppulse.sdk.ui.PerformanceDashboardActivity].
 *
 * @param application Host application. Used for strings and to open the database.
 * @param repository Performance storage.
 */
class PerformanceViewModel(
    application: Application,
    private val repository: PerformanceRepository
) : AndroidViewModel(application) {

    private val rowsState = MutableStateFlow<List<PerformanceRow>>(emptyList())

    /**
     * Dashboard rows, starting with the summary card.
     */
    val rows: StateFlow<List<PerformanceRow>> = rowsState.asStateFlow()

    /**
     * Reads every performance section and publishes [rows].
     */
    fun load() {
        viewModelScope.launch {
            try {
                val report = repository.loadReport()
                val startups = repository.loadStartupMetrics()
                val screens = repository.loadSlowScreens()
                val calls = repository.loadNetworkMetrics()
                val fps = repository.loadFpsMetrics()
                val anrs = repository.loadAnrMetrics()
                rowsState.value = buildRows(report, startups, screens, calls, fps, anrs)
                AppPulseLogger.i(TAG, "Performance dashboard loaded")
            } catch (throwable: Throwable) {
                AppPulseLogger.e(TAG, "Performance dashboard failed to load", throwable)
            }
        }
    }

    private fun buildRows(
        report: PerformanceReport,
        startups: List<StartupMetric>,
        screens: List<ScreenMetric>,
        calls: List<NetworkMetric>,
        fps: List<FpsMetric>,
        anrs: List<AnrMetric>
    ): List<PerformanceRow> {
        return buildList {
            add(PerformanceRow.Summary(report))
            add(PerformanceRow.Header(text(R.string.performance_section_startup)))
            if (startups.isEmpty()) {
                add(emptyRow())
            } else {
                startups.forEach { metric ->
                    add(
                        PerformanceRow.Metric(
                            title = metric.activityName,
                            subtitle = formatWhen(metric.firstActivityTimestamp),
                            value = milliseconds(metric.startupTimeMs),
                            flag = if (metric.slowStartup) text(R.string.performance_flag_slow) else null
                        )
                    )
                }
            }
            add(PerformanceRow.Header(text(R.string.performance_section_screens)))
            if (screens.isEmpty()) {
                add(emptyRow())
            } else {
                screens.forEach { metric ->
                    add(
                        PerformanceRow.Metric(
                            title = metric.activityName,
                            subtitle = formatWhen(metric.openTimestamp),
                            value = milliseconds(metric.renderTimeMs),
                            flag = text(R.string.performance_flag_slow)
                        )
                    )
                }
            }
            add(PerformanceRow.Header(text(R.string.performance_section_network)))
            if (calls.isEmpty()) {
                add(emptyRow())
            } else {
                calls.forEach { metric ->
                    val status = if (metric.responseCode == 0) "failed" else metric.responseCode.toString()
                    add(
                        PerformanceRow.Metric(
                            title = metric.apiLabel(),
                            subtitle = "${metric.method} · $status · " +
                                "${formatBytes(metric.requestSize)} → ${formatBytes(metric.responseSize)}",
                            value = milliseconds(metric.durationMs),
                            flag = if (metric.slowApi) text(R.string.performance_flag_slow) else null
                        )
                    )
                }
            }
            add(PerformanceRow.Header(text(R.string.performance_section_fps)))
            if (fps.isEmpty()) {
                add(emptyRow())
            } else {
                fps.forEach { metric ->
                    add(
                        PerformanceRow.Metric(
                            title = "${metric.fps.roundToInt()} FPS",
                            subtitle = "Dropped ${metric.droppedFrames} · Avg ${metric.averageFps.roundToInt()}",
                            value = formatWhen(metric.timestamp),
                            flag = if (metric.performanceIssue) text(R.string.performance_flag_fps) else null
                        )
                    )
                }
            }
            add(PerformanceRow.Header(text(R.string.performance_section_anr)))
            if (anrs.isEmpty()) {
                add(emptyRow())
            } else {
                anrs.forEach { metric ->
                    add(
                        PerformanceRow.Metric(
                            title = metric.threadName,
                            subtitle = metric.stackTrace.lineSequence().firstOrNull().orEmpty(),
                            value = milliseconds(metric.blockedDurationMs),
                            flag = "ANR"
                        )
                    )
                }
            }
        }
    }

    private fun emptyRow(): PerformanceRow.Metric {
        return PerformanceRow.Metric(
            title = text(R.string.performance_none),
            subtitle = "",
            value = "",
            flag = null
        )
    }

    private fun milliseconds(value: Long): String {
        return text(R.string.performance_milliseconds, value)
    }

    private fun text(resId: Int, vararg formatArgs: Any): String {
        return getApplication<Application>().getString(resId, *formatArgs)
    }

    /**
     * Creates a [PerformanceViewModel] bound to the process database.
     *
     * @param application Host application used to open [AppPulseDatabase].
     */
    class Factory(
        private val application: Application
    ) : ViewModelProvider.Factory {

        /**
         * Creates a [PerformanceViewModel].
         *
         * @throws IllegalArgumentException when [modelClass] is not a [PerformanceViewModel].
         */
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(PerformanceViewModel::class.java)) {
                "Factory can only create PerformanceViewModel"
            }
            val database = AppPulseDatabase.getInstance(application)
            val repository = PerformanceRepository(
                startupDao = database.startupDao(),
                screenDao = database.screenDao(),
                networkDao = database.networkDao(),
                fpsDao = database.fpsDao(),
                anrDao = database.anrDao()
            )
            @Suppress("UNCHECKED_CAST")
            return PerformanceViewModel(application, repository) as T
        }
    }

    private companion object {
        const val TAG = "AppPulsePerf"

        fun formatWhen(epochMillis: Long): String {
            val formatter = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
            return formatter.format(Date(epochMillis))
        }

        fun formatBytes(bytes: Long): String {
            if (bytes < 1024L) {
                return "$bytes B"
            }
            val kilobytes = bytes / 1024.0
            if (kilobytes < 1024.0) {
                return String.format(Locale.US, "%.1f KB", kilobytes)
            }
            return String.format(Locale.US, "%.1f MB", kilobytes / 1024.0)
        }
    }
}

/**
 * One row in the performance dashboard list.
 */
sealed interface PerformanceRow {

    /**
     * Top card with averages and the slowest samples.
     *
     * @property report Aggregated figures.
     */
    data class Summary(val report: PerformanceReport) : PerformanceRow

    /**
     * Section title.
     *
     * @property title Section name.
     */
    data class Header(val title: String) : PerformanceRow

    /**
     * One stored sample.
     *
     * @property title Primary label.
     * @property subtitle Supporting detail. Empty when there is nothing extra to show.
     * @property value Trailing measurement.
     * @property flag Short problem label, or null when the sample is healthy.
     */
    data class Metric(
        val title: String,
        val subtitle: String,
        val value: String,
        val flag: String?
    ) : PerformanceRow
}
