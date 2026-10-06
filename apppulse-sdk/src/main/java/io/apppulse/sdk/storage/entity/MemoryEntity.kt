package io.apppulse.sdk.storage.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room row for one heap sample.
 */
@Entity(tableName = "memory_metrics")
data class MemoryEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,
    @ColumnInfo(name = "timestamp")
    val timestamp: Long,
    @ColumnInfo(name = "usedMemoryMb")
    val usedMemoryMb: Double,
    @ColumnInfo(name = "freeMemoryMb")
    val freeMemoryMb: Double,
    @ColumnInfo(name = "maxMemoryMb")
    val maxMemoryMb: Double,
    @ColumnInfo(name = "totalMemoryMb")
    val totalMemoryMb: Double,
    @ColumnInfo(name = "nativeMemoryMb")
    val nativeMemoryMb: Double,
    @ColumnInfo(name = "isSynced", defaultValue = "0")
    val isSynced: Boolean = false
)
