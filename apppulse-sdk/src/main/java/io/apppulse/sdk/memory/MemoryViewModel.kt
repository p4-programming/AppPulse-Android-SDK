package io.apppulse.sdk.memory

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.apppulse.sdk.core.AppPulseLogger
import io.apppulse.sdk.memory.model.BitmapMetric
import io.apppulse.sdk.memory.model.LeakCandidate
import io.apppulse.sdk.memory.model.MemoryHealthReport
import io.apppulse.sdk.memory.model.MemoryMetric
import io.apppulse.sdk.storage.AppPulseDatabase
import io.apppulse.sdk.storage.repository.MemoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Loads heap samples, leaks, bitmaps, and the health score for the memory screens.
 *
 * @param application Host application.
 * @param repository Memory storage.
 * @param calculator Score calculator. A future Hilt binding can replace it.
 */
class MemoryViewModel(
    application: Application,
    private val repository: MemoryRepository,
    private val calculator: MemoryHealthCalculator = MemoryHealthCalculator()
) : AndroidViewModel(application) {

    private val stateHolder = MutableStateFlow(MemoryDashboardState())

    /**
     * Latest dashboard figures. Empty until [load] succeeds.
     */
    val state: StateFlow<MemoryDashboardState> = stateHolder.asStateFlow()

    /**
     * Reads stored memory data and publishes [state].
     */
    fun load() {
        viewModelScope.launch {
            try {
                val metrics = repository.getMemoryMetrics()
                val leaks = repository.getLeakCandidates()
                val bitmaps = repository.getBitmapMetrics()
                val current = metrics.firstOrNull()
                val latestBitmap = bitmaps.firstOrNull()
                val health = current?.let { metric ->
                    calculator.calculate(
                        usedMemoryMb = metric.usedMemoryMb,
                        maxMemoryMb = metric.maxMemoryMb,
                        leakCount = leaks.size,
                        largeBitmapCount = latestBitmap?.largeCount ?: 0,
                        recentUsedMb = metrics.take(TREND_WINDOW).map { it.usedMemoryMb }.asReversed()
                    )
                }
                stateHolder.value = MemoryDashboardState(
                    current = current,
                    peakMb = repository.peakUsedMemoryMb(),
                    averageMb = repository.averageUsedMemoryMb(),
                    leaks = leaks,
                    timeline = metrics.asReversed(),
                    bitmaps = bitmaps,
                    health = health
                )
                AppPulseLogger.i(TAG, "Memory dashboard loaded")
            } catch (throwable: Throwable) {
                AppPulseLogger.e(TAG, "Memory dashboard failed to load", throwable)
            }
        }
    }

    /**
     * Creates a [MemoryViewModel] bound to the process database.
     */
    class Factory(
        private val application: Application
    ) : ViewModelProvider.Factory {

        /**
         * Creates a [MemoryViewModel].
         *
         * @throws IllegalArgumentException when [modelClass] is not a [MemoryViewModel].
         */
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(MemoryViewModel::class.java)) {
                "Factory can only create MemoryViewModel"
            }
            val database = AppPulseDatabase.getInstance(application)
            val repository = MemoryRepository(
                memoryDao = database.memoryDao(),
                leakDao = database.leakDao(),
                bitmapDao = database.bitmapDao()
            )
            @Suppress("UNCHECKED_CAST")
            return MemoryViewModel(application, repository) as T
        }
    }

    private companion object {
        const val TAG = "AppPulseMem"
        const val TREND_WINDOW = 6
    }
}

/**
 * Figures shown by the memory dashboard and detail screen.
 *
 * @property current Newest heap sample.
 * @property peakMb Largest used heap stored so far.
 * @property averageMb Mean used heap.
 * @property leaks Stored leak candidates, newest first.
 * @property timeline Heap samples from oldest to newest.
 * @property bitmaps Bitmap rows, newest first.
 * @property health Score for [current], or null before the first sample.
 */
data class MemoryDashboardState(
    val current: MemoryMetric? = null,
    val peakMb: Double? = null,
    val averageMb: Double? = null,
    val leaks: List<LeakCandidate> = emptyList(),
    val timeline: List<MemoryMetric> = emptyList(),
    val bitmaps: List<BitmapMetric> = emptyList(),
    val health: MemoryHealthReport? = null
)
