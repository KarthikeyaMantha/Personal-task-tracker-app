package com.personal.daymark.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * An item within a DayTemplate.
 * Can represent either an actionable Task (isTask = true) or a Busy Block (isTask = false).
 */
@Entity(
    tableName = "template_items",
    foreignKeys = [
        ForeignKey(
            entity = DayTemplateEntity::class,
            parentColumns = ["id"],
            childColumns = ["template_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["template_id"])]
)
data class TemplateItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "template_id")
    val templateId: Long,
    @ColumnInfo(name = "title")
    val title: String,
    @ColumnInfo(name = "start_time")
    val startTime: String, // Format: "HH:mm"
    @ColumnInfo(name = "duration_minutes")
    val durationMinutes: Int,
    @ColumnInfo(name = "is_task")
    val isTask: Boolean = true
)
