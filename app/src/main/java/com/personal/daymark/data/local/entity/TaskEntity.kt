package com.personal.daymark.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "tasks",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["category_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index("category_id"),
        Index("due_date"),
        Index("is_completed"),
        Index("priority"),
        Index("series_id")
    ]
)
data class TaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String? = null,
    @ColumnInfo(name = "due_date")
    val dueDate: Long? = null, // epoch millis representing calendar date
    @ColumnInfo(name = "due_time")
    val dueTime: Int? = null, // minutes from midnight (0..1439)
    val priority: Int = 0, // 0 = None, 1 = Low, 2 = Medium, 3 = High, 4 = Urgent
    @ColumnInfo(name = "category_id")
    val categoryId: Long? = null,
    @ColumnInfo(name = "is_completed")
    val isCompleted: Boolean = false,
    @ColumnInfo(name = "completion_timestamp")
    val completionTimestamp: Long? = null,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "recurrence_rule")
    val recurrenceRule: String? = null,
    @ColumnInfo(name = "series_id")
    val seriesId: String? = null
)
