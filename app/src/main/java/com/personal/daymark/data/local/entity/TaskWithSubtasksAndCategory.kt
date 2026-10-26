package com.personal.daymark.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation

data class TaskWithSubtasksAndCategory(
    @Embedded
    val task: TaskEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "task_id"
    )
    val subtasks: List<SubtaskEntity> = emptyList(),

    @Relation(
        parentColumn = "category_id",
        entityColumn = "id"
    )
    val category: CategoryEntity? = null
)
