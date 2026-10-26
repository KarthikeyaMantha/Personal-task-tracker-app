package com.personal.daymark.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Represents a non-task "Busy Block" on the daily timeline (e.g. "Lunch", "Client Meeting", "Commute").
 * Used to protect time and prevent conflicting tasks from being scheduled.
 */
@Entity(
    tableName = "time_blocks",
    indices = [Index(value = ["date"])]
)
data class TimeBlockEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "title")
    val title: String,
    @ColumnInfo(name = "start_time")
    val startTime: String, // Format: "HH:mm" e.g. "12:00"
    @ColumnInfo(name = "duration_minutes")
    val durationMinutes: Int, // e.g. 60
    @ColumnInfo(name = "color_hex")
    val colorHex: String? = null,
    @ColumnInfo(name = "date")
    val date: String // Format: "yyyy-MM-dd"
) {
    fun getStartMinutes(): Int {
        val parts = startTime.split(":")
        if (parts.size >= 2) {
            val h = parts[0].trim().toIntOrNull() ?: 0
            val m = parts[1].trim().toIntOrNull() ?: 0
            return h * 60 + m
        }
        return 0
    }

    fun getEndMinutes(): Int {
        return getStartMinutes() + durationMinutes
    }
}
