package io.apppulse.sdk.storage.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room row for one activity or fragment leak candidate.
 */
@Entity(tableName = "leak_candidates")
data class LeakEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,
    @ColumnInfo(name = "componentName")
    val componentName: String,
    @ColumnInfo(name = "componentType")
    val componentType: String,
    @ColumnInfo(name = "destroyedTimestamp")
    val destroyedTimestamp: Long,
    @ColumnInfo(name = "detectedTimestamp")
    val detectedTimestamp: Long,
    @ColumnInfo(name = "isSynced", defaultValue = "0")
    val isSynced: Boolean = false
)
