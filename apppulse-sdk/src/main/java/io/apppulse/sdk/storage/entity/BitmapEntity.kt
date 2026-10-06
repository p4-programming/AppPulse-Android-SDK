package io.apppulse.sdk.storage.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room row for bitmap usage.
 *
 * [sizeMb] is the measured bitmap size. [largeBitmap] is true when a bitmap
 * in the row is larger than 5 MB.
 */
@Entity(tableName = "bitmap_metrics")
data class BitmapEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,
    @ColumnInfo(name = "timestamp")
    val timestamp: Long,
    @ColumnInfo(name = "sizeMb")
    val sizeMb: Double,
    @ColumnInfo(name = "bitmapCount")
    val bitmapCount: Int,
    @ColumnInfo(name = "largeCount")
    val largeCount: Int,
    @ColumnInfo(name = "largeBitmap")
    val largeBitmap: Boolean,
    @ColumnInfo(name = "isSynced", defaultValue = "0")
    val isSynced: Boolean = false
)
