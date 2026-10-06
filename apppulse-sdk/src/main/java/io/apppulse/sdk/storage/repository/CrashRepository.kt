package io.apppulse.sdk.storage.repository

import io.apppulse.sdk.core.AppPulseLogger
import io.apppulse.sdk.crash.CrashEntity
import io.apppulse.sdk.storage.dao.CrashDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Crash storage API used by the tracker and the debug dashboard.
 *
 * Database work runs on [Dispatchers.IO]. Each successful operation is written
 * through [AppPulseLogger].
 *
 * @param crashDao Room DAO that owns the `crashes` table.
 */
class CrashRepository(
    private val crashDao: CrashDao
) {

    /**
     * Stores [crash] and returns the generated row id.
     *
     * @param crash Report captured from an uncaught exception.
     */
    suspend fun insertCrash(crash: CrashEntity): Long {
        val id = withContext(Dispatchers.IO) {
            crashDao.insertCrash(crash)
        }
        AppPulseLogger.i(TAG, "Crash saved id=$id")
        return id
    }

    /**
     * Returns every stored crash, newest first.
     */
    suspend fun getAllCrashes(): List<CrashEntity> {
        val crashes = withContext(Dispatchers.IO) {
            crashDao.getAllCrashes()
        }
        AppPulseLogger.i(TAG, "Crash loaded count=${crashes.size}")
        return crashes
    }

    /**
     * Returns one crash, or null when [id] is not stored.
     *
     * @param id Primary key of the report.
     */
    suspend fun getCrashById(id: Long): CrashEntity? {
        val crash = withContext(Dispatchers.IO) {
            crashDao.getCrashById(id)
        }
        if (crash == null) {
            AppPulseLogger.w(TAG, "Crash loaded id=$id was not found")
        } else {
            AppPulseLogger.i(TAG, "Crash loaded id=$id")
        }
        return crash
    }

    /**
     * Removes one stored crash.
     *
     * @param crash Report to delete.
     */
    suspend fun deleteCrash(crash: CrashEntity) {
        withContext(Dispatchers.IO) {
            crashDao.deleteCrash(crash)
        }
        AppPulseLogger.i(TAG, "Crash deleted id=${crash.id}")
    }

    /**
     * Removes every stored crash.
     */
    suspend fun deleteAllCrashes() {
        withContext(Dispatchers.IO) {
            crashDao.deleteAllCrashes()
        }
        AppPulseLogger.i(TAG, "Crash deleted all")
    }

    private companion object {
        const val TAG = "AppPulseStore"
    }
}
