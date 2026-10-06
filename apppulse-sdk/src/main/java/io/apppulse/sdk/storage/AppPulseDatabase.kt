package io.apppulse.sdk.storage

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import io.apppulse.sdk.core.AppPulseLogger
import io.apppulse.sdk.crash.CrashEntity
import io.apppulse.sdk.storage.dao.AnrDao
import io.apppulse.sdk.storage.dao.BitmapDao
import io.apppulse.sdk.storage.dao.CrashDao
import io.apppulse.sdk.storage.dao.FpsDao
import io.apppulse.sdk.storage.dao.HealthSnapshotDao
import io.apppulse.sdk.storage.dao.LeakDao
import io.apppulse.sdk.storage.dao.MemoryDao
import io.apppulse.sdk.storage.dao.NetworkDao
import io.apppulse.sdk.storage.dao.ScreenDao
import io.apppulse.sdk.storage.dao.SecurityIssueDao
import io.apppulse.sdk.storage.dao.StartupDao
import io.apppulse.sdk.storage.entity.AnrEntity
import io.apppulse.sdk.storage.entity.BitmapEntity
import io.apppulse.sdk.storage.entity.FpsEntity
import io.apppulse.sdk.storage.entity.HealthSnapshotEntity
import io.apppulse.sdk.storage.entity.LeakEntity
import io.apppulse.sdk.storage.entity.MemoryEntity
import io.apppulse.sdk.storage.entity.NetworkEntity
import io.apppulse.sdk.storage.entity.ScreenEntity
import io.apppulse.sdk.storage.entity.SecurityIssueEntity
import io.apppulse.sdk.storage.entity.StartupEntity

/**
 * Process-wide Room database for AppPulse.
 *
 * The file name is [DATABASE_NAME]. [getInstance] is safe to call from more
 * than one thread and always returns the same open database.
 */
@Database(
    entities = [
        CrashEntity::class,
        StartupEntity::class,
        ScreenEntity::class,
        NetworkEntity::class,
        FpsEntity::class,
        AnrEntity::class,
        MemoryEntity::class,
        LeakEntity::class,
        BitmapEntity::class,
        SecurityIssueEntity::class,
        HealthSnapshotEntity::class
    ],
    version = 6,
    exportSchema = false
)
abstract class AppPulseDatabase : RoomDatabase() {

    /**
     * Returns the DAO used to read and write crash reports.
     */
    abstract fun crashDao(): CrashDao

    /**
     * Returns the DAO used to read and write cold-start metrics.
     */
    abstract fun startupDao(): StartupDao

    /**
     * Returns the DAO used to read and write screen render metrics.
     */
    abstract fun screenDao(): ScreenDao

    /**
     * Returns the DAO used to read and write HTTP metrics.
     */
    abstract fun networkDao(): NetworkDao

    /**
     * Returns the DAO used to read and write FPS samples.
     */
    abstract fun fpsDao(): FpsDao

    /**
     * Returns the DAO used to read and write ANR records.
     */
    abstract fun anrDao(): AnrDao

    /**
     * Returns the DAO used to read and write heap samples.
     */
    abstract fun memoryDao(): MemoryDao

    /**
     * Returns the DAO used to read and write leak candidates.
     */
    abstract fun leakDao(): LeakDao

    /**
     * Returns the DAO used to read and write bitmap measurements.
     */
    abstract fun bitmapDao(): BitmapDao

    /**
     * Returns the DAO used to read and write security findings.
     */
    abstract fun securityIssueDao(): SecurityIssueDao

    /**
     * Returns the DAO used to read and write health-report history.
     */
    abstract fun healthSnapshotDao(): HealthSnapshotDao

    companion object {
        /**
         * SQLite file name created in the host application's database directory.
         */
        const val DATABASE_NAME = "apppulse.db"

        private const val TAG = "AppPulseDb"

        @Volatile
        private var instance: AppPulseDatabase? = null

        /**
         * Returns the shared database, opening it on the first call.
         *
         * @param context Any context. The application context is retained so an
         * activity cannot be leaked by the singleton.
         */
        fun getInstance(context: Context): AppPulseDatabase {
            instance?.let { return it }
            return synchronized(this) {
                instance ?: build(context.applicationContext).also { created ->
                    instance = created
                }
            }
        }

        private fun build(context: Context): AppPulseDatabase {
            AppPulseLogger.i(TAG, "Opening $DATABASE_NAME")
            return Room.databaseBuilder(
                context,
                AppPulseDatabase::class.java,
                DATABASE_NAME
            )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                .build()
        }
    }
}
