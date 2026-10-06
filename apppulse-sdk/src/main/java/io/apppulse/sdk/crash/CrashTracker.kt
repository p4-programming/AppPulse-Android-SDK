package io.apppulse.sdk.crash

import android.app.Application
import io.apppulse.sdk.core.AppPulseLogger
import io.apppulse.sdk.storage.AppPulseDatabase
import io.apppulse.sdk.storage.repository.CrashRepository

/**
 * Installs AppPulse as the process default uncaught-exception handler and
 * stores each crash in Room before the process exits.
 *
 * The tracker is constructed with its [Application] dependency so a future
 * Hilt binding can provide the same type without a service locator.
 *
 * @param application Host application stored for crash reports.
 */
class CrashTracker(
    private val application: Application
) {

    /**
     * Opens `apppulse.db` and registers [AppPulseExceptionHandler].
     *
     * The handler that is already installed is retained and invoked after
     * AppPulse records the crash. A database failure is logged and does not
     * block handler installation; crashes are still written to logcat.
     * Calling this method more than once does not wrap the AppPulse handler
     * in itself.
     */
    fun initialize() {
        val repository = openRepository()
        val current = Thread.getDefaultUncaughtExceptionHandler()
        if (current is AppPulseExceptionHandler) {
            AppPulseLogger.i(TAG, "Crash tracking is already installed")
            return
        }

        val handler = AppPulseExceptionHandler(
            application = application,
            previousHandler = current,
            crashRepository = repository
        )
        Thread.setDefaultUncaughtExceptionHandler(handler)
        AppPulseLogger.i(TAG, "Crash tracking initialized")
    }

    private fun openRepository(): CrashRepository? {
        return try {
            val database = AppPulseDatabase.getInstance(application)
            CrashRepository(database.crashDao())
        } catch (throwable: Throwable) {
            AppPulseLogger.e(TAG, "Crash database failed to open", throwable)
            null
        }
    }

    private companion object {
        const val TAG = "AppPulseCrash"
    }
}
