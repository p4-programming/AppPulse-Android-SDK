package io.apppulse.sdk.crash

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.apppulse.sdk.core.AppPulseLogger
import io.apppulse.sdk.storage.AppPulseDatabase
import io.apppulse.sdk.storage.repository.CrashRepository
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Dashboard state for crash reports stored on the device.
 *
 * [crashes] is the list shown by the crash screen. [selectedCrash] is the row
 * opened on the detail screen. Storage failures are logged and leave the last
 * successful state in place.
 *
 * @param application Host application. Required by [AndroidViewModel].
 * @param repository Crash storage. Passed in so a future Hilt binding can
 * replace [Factory].
 */
class CrashViewModel(
    application: Application,
    private val repository: CrashRepository
) : AndroidViewModel(application) {

    private val crashesState = MutableStateFlow<List<CrashEntity>>(emptyList())

    /**
     * Newest-first crash reports from the last successful [loadCrashes].
     */
    val crashes: StateFlow<List<CrashEntity>> = crashesState.asStateFlow()

    private val selectedCrashState = MutableStateFlow<SelectedCrash>(SelectedCrash.Idle)

    /**
     * Detail-screen state. [SelectedCrash.Idle] until [loadCrash] finishes.
     */
    val selectedCrash: StateFlow<SelectedCrash> = selectedCrashState.asStateFlow()

    private val deletedEvents = MutableSharedFlow<Long>(extraBufferCapacity = 1)

    /**
     * Emits the id of a crash after [deleteCrash] has removed it from Room.
     * The detail screen uses this to close only after the delete commits.
     */
    val crashDeleted: SharedFlow<Long> = deletedEvents.asSharedFlow()

    /**
     * Reads every stored crash into [crashes].
     */
    fun loadCrashes() {
        viewModelScope.launch {
            try {
                crashesState.value = repository.getAllCrashes()
            } catch (throwable: Throwable) {
                AppPulseLogger.e(TAG, "Crash loaded failed", throwable)
            }
        }
    }

    /**
     * Reads one crash into [selectedCrash].
     *
     * @param id Primary key passed from the crash list.
     */
    fun loadCrash(id: Long) {
        viewModelScope.launch {
            try {
                val crash = repository.getCrashById(id)
                selectedCrashState.value = if (crash == null) {
                    SelectedCrash.Missing
                } else {
                    SelectedCrash.Found(crash)
                }
            } catch (throwable: Throwable) {
                AppPulseLogger.e(TAG, "Crash loaded failed id=$id", throwable)
                selectedCrashState.value = SelectedCrash.Missing
            }
        }
    }

    /**
     * Deletes [crash] from Room and from [crashes].
     *
     * The database call uses [NonCancellable] so the row is still removed if
     * the screen closes while the write is running.
     *
     * @param crash Report to remove.
     */
    fun deleteCrash(crash: CrashEntity) {
        viewModelScope.launch {
            try {
                withContext(NonCancellable) {
                    repository.deleteCrash(crash)
                }
                crashesState.value = crashesState.value.filterNot { it.id == crash.id }
                deletedEvents.emit(crash.id)
            } catch (throwable: Throwable) {
                AppPulseLogger.e(TAG, "Crash deleted failed id=${crash.id}", throwable)
            }
        }
    }

    /**
     * Deletes every stored crash and clears [crashes].
     */
    fun clearAllCrashes() {
        viewModelScope.launch {
            try {
                withContext(NonCancellable) {
                    repository.deleteAllCrashes()
                }
                crashesState.value = emptyList()
            } catch (throwable: Throwable) {
                AppPulseLogger.e(TAG, "Crash deleted all failed", throwable)
            }
        }
    }

    /**
     * Result of [loadCrash].
     */
    sealed class SelectedCrash {
        /**
         * [loadCrash] has not finished for this screen.
         */
        data object Idle : SelectedCrash()

        /**
         * The requested crash is stored.
         *
         * @property crash Row loaded from Room.
         */
        data class Found(val crash: CrashEntity) : SelectedCrash()

        /**
         * The requested id is not in the database.
         */
        data object Missing : SelectedCrash()
    }

    /**
     * Creates a [CrashViewModel] bound to the process database.
     *
     * @param application Host application used to open [AppPulseDatabase].
     */
    class Factory(
        private val application: Application
    ) : ViewModelProvider.Factory {

        /**
         * Creates a [CrashViewModel].
         *
         * @throws IllegalArgumentException when [modelClass] is not a [CrashViewModel].
         */
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(CrashViewModel::class.java)) {
                "Factory can only create CrashViewModel"
            }
            val repository = CrashRepository(
                AppPulseDatabase.getInstance(application).crashDao()
            )
            @Suppress("UNCHECKED_CAST")
            return CrashViewModel(application, repository) as T
        }
    }

    private companion object {
        const val TAG = "AppPulseCrash"
    }
}
