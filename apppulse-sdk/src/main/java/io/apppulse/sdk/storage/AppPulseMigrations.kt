package io.apppulse.sdk.storage

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/**
 * Opens `apppulse.db` version 1 (crash reports only) as version 2 by adding
 * the performance tables. Existing crash rows are left in place.
 */
internal val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS `startup_metrics` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`launchTimestamp` INTEGER NOT NULL, " +
                "`firstActivityTimestamp` INTEGER NOT NULL, " +
                "`startupTimeMs` INTEGER NOT NULL, " +
                "`slowStartup` INTEGER NOT NULL, " +
                "`activityName` TEXT NOT NULL)"
        )
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS `screen_metrics` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`activityName` TEXT NOT NULL, " +
                "`openTimestamp` INTEGER NOT NULL, " +
                "`fullyDrawnTimestamp` INTEGER NOT NULL, " +
                "`renderTimeMs` INTEGER NOT NULL, " +
                "`slowScreen` INTEGER NOT NULL)"
        )
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS `network_metrics` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`url` TEXT NOT NULL, " +
                "`method` TEXT NOT NULL, " +
                "`responseCode` INTEGER NOT NULL, " +
                "`requestSize` INTEGER NOT NULL, " +
                "`responseSize` INTEGER NOT NULL, " +
                "`durationMs` INTEGER NOT NULL, " +
                "`slowApi` INTEGER NOT NULL, " +
                "`timestamp` INTEGER NOT NULL)"
        )
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS `fps_metrics` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`timestamp` INTEGER NOT NULL, " +
                "`fps` REAL NOT NULL, " +
                "`droppedFrames` INTEGER NOT NULL, " +
                "`averageFps` REAL NOT NULL, " +
                "`performanceIssue` INTEGER NOT NULL)"
        )
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS `anr_metrics` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`timestamp` INTEGER NOT NULL, " +
                "`blockedDurationMs` INTEGER NOT NULL, " +
                "`stackTrace` TEXT NOT NULL, " +
                "`threadName` TEXT NOT NULL)"
        )
    }
}

/**
 * Opens version 2 as version 3 by adding memory, leak, and bitmap tables.
 * Crash and performance rows are left in place.
 */
internal val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS `memory_metrics` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`timestamp` INTEGER NOT NULL, " +
                "`usedMemoryMb` REAL NOT NULL, " +
                "`freeMemoryMb` REAL NOT NULL, " +
                "`maxMemoryMb` REAL NOT NULL, " +
                "`totalMemoryMb` REAL NOT NULL, " +
                "`nativeMemoryMb` REAL NOT NULL)"
        )
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS `leak_candidates` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`componentName` TEXT NOT NULL, " +
                "`componentType` TEXT NOT NULL, " +
                "`destroyedTimestamp` INTEGER NOT NULL, " +
                "`detectedTimestamp` INTEGER NOT NULL)"
        )
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS `bitmap_metrics` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`timestamp` INTEGER NOT NULL, " +
                "`sizeMb` REAL NOT NULL, " +
                "`bitmapCount` INTEGER NOT NULL, " +
                "`largeCount` INTEGER NOT NULL, " +
                "`largeBitmap` INTEGER NOT NULL)"
        )
    }
}

/**
 * Opens version 3 as version 4 by adding the security findings table.
 * Crash, performance, and memory rows are left in place.
 */
internal val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS `security_issues` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`timestamp` INTEGER NOT NULL, " +
                "`category` TEXT NOT NULL, " +
                "`code` TEXT NOT NULL, " +
                "`title` TEXT NOT NULL, " +
                "`description` TEXT NOT NULL, " +
                "`severity` TEXT NOT NULL, " +
                "`target` TEXT NOT NULL)"
        )
    }
}

/**
 * Opens version 4 as version 5 by adding health-report history.
 * Existing module rows are left in place.
 */
internal val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS `health_snapshots` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`generatedAt` INTEGER NOT NULL, " +
                "`overallScore` INTEGER NOT NULL, " +
                "`overallStatus` TEXT NOT NULL, " +
                "`crashScore` INTEGER NOT NULL, " +
                "`performanceScore` INTEGER NOT NULL, " +
                "`memoryScore` INTEGER NOT NULL, " +
                "`securityScore` INTEGER NOT NULL)"
        )
    }
}

/**
 * Opens version 5 as version 6 by adding [isSynced] to every stored table.
 * Existing rows are kept and treated as not yet uploaded.
 */
internal val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(connection: SQLiteConnection) {
        val tables = listOf(
            "crashes",
            "startup_metrics",
            "screen_metrics",
            "network_metrics",
            "fps_metrics",
            "anr_metrics",
            "memory_metrics",
            "leak_candidates",
            "bitmap_metrics",
            "security_issues",
            "health_snapshots"
        )
        tables.forEach { table ->
            connection.execSQL(
                "ALTER TABLE `$table` ADD COLUMN `isSynced` INTEGER NOT NULL DEFAULT 0"
            )
        }
    }
}
