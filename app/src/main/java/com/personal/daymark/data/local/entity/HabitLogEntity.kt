package com.personal.daymark.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "habit_logs",
    foreignKeys = [
        ForeignKey(
            entity = HabitEntity::class,
            parentColumns = ["id"],
            childColumns = ["habit_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["habit_id", "date"], unique = true),
        Index("date"),
        Index(value = ["habit_id", "date_string"]),
        Index("date_string")
    ]
)
data class HabitLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "habit_id")
    val habitId: Long,
    val date: Long, // Epoch day representation (e.g. LocalDate.toEpochDay())
    @ColumnInfo(name = "date_string", defaultValue = "")
    val dateString: String = "", // ISO-8601 date string (e.g. "2026-10-26")
    @ColumnInfo(name = "is_completed")
    val isCompleted: Boolean = true,
    @ColumnInfo(name = "logged_at")
    val loggedAt: Long = System.currentTimeMillis()
)
