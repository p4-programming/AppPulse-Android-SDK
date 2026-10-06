package io.apppulse.sdk.crash

import android.app.Application
import io.apppulse.sdk.core.AppPulseLogger
import io.apppulse.sdk.model.CrashEvent
import io.apppulse.sdk.storage.repository.CrashRepository
import io.apppulse.sdk.utils.DeviceUtils
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.system.exitProcess
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout

/**
 * Uncaught-exception handler that records an AppPulse [CrashEvent], writes a
 * [CrashEntity] to Room, and then forwards the failure to the previous handler.
 *
 * The Room write blocks the crashing thread until the transaction commits, or
 * until [PERSIST_TIMEOUT_MILLIS] elapses, so the row is on disk before the
 * process is killed. A storage failure is logged and does not stop the
 * default crash flow.
 *
 * @param application Host application used to read the app version.
 * @param previousHandler Handler that was current when this one was installed.
 * @param crashRepository Storage used to persist the report. Null when the
 * database could not be opened.
 */
class AppPulseExceptionHandler(
    private val application: Application,
    private val previousHandler: Thread.UncaughtExceptionHandler?,
    private val crashRepository: CrashRepository?
) : Thread.UncaughtExceptionHandler {

    private val reporting = AtomicBoolean(false)

    /**
     * Records [throwable], stores it, and delegates to the previous handler.
     *
     * A second entry on the same process skips reporting and only delegates,
     * so a failure inside the reporter cannot recurse.
     *
     * @param thread Thread that threw [throwable].
     * @param throwable Exception that escaped [thread].
     */
    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        if (reporting.compareAndSet(false, true)) {
            report(thread, throwable)
        } else {
            AppPulseLogger.w(TAG, "Dropping re-entrant uncaught exception on ${thread.name}")
        }
        delegate(thread, throwable)
    }

    private fun report(thread: Thread, throwable: Throwable) {
        try {
            val event = CrashEvent.capture(
                thread = thread,
                throwable = throwable,
                deviceInfo = DeviceUtils.deviceInfo(application)
            )
            AppPulseLogger.e(TAG, event.summary(), throwable)
            AppPulseLogger.e(TAG, event.stackTrace)
            persist(CrashEntity.from(event))
        } catch (reportingFailure: Throwable) {
            AppPulseLogger.e(TAG, "Failed to record uncaught exception", reportingFailure)
        }
    }

    private fun persist(crash: CrashEntity) {
        val repository = crashRepository
        if (repository == null) {
            AppPulseLogger.e(TAG, "Crash was not saved because the database is unavailable")
            return
        }
        runBlocking {
            withTimeout(PERSIST_TIMEOUT_MILLIS) {
                repository.insertCrash(crash)
            }
        }
    }

    private fun delegate(thread: Thread, throwable: Throwable) {
        val next = previousHandler
        if (next != null && next !== this) {
            next.uncaughtException(thread, throwable)
            return
        }
        android.os.Process.killProcess(android.os.Process.myPid())
        exitProcess(PROCESS_EXIT_CODE)
    }

    private companion object {
        const val TAG = "AppPulseCrash"
        const val PROCESS_EXIT_CODE = 10
        const val PERSIST_TIMEOUT_MILLIS = 2_500L
    }
}
