package io.apppulse.sdk.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import io.apppulse.sdk.AppPulse
import io.apppulse.sdk.core.AppPulseLogger

/**
 * Periodic uploader for unsynced AppPulse rows.
 *
 * WorkManager runs this about every 30 minutes when auto sync is enabled.
 * The request requires an internet connection and a battery that is not low.
 * [AppPulse.disableAutoSync] cancels the unique work. A run that starts after
 * auto sync is turned off returns success without uploading.
 */
class SyncWorker(
    context: Context,
    parameters: WorkerParameters
) : CoroutineWorker(context, parameters) {

    /**
     * Uploads unsynced rows and maps the outcome to a WorkManager result.
     */
    override suspend fun doWork(): Result {
        val manager = AppPulse.syncManager()
        if (manager == null) {
            AppPulseLogger.w(TAG, "Sync worker ignored because the SDK is not initialized")
            return Result.failure()
        }
        if (!manager.isAutoSyncEnabled()) {
            AppPulseLogger.i(TAG, "Sync worker skipped because auto sync is disabled")
            return Result.success()
        }
        return when (manager.sync()) {
            SyncOutcome.SUCCESS -> Result.success()
            SyncOutcome.RETRY -> Result.retry()
            SyncOutcome.FAILURE -> Result.failure()
        }
    }

    private companion object {
        const val TAG = "AppPulseSync"
    }
}
