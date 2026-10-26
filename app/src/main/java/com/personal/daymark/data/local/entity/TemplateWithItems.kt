package com.personal.daymark.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation

/**
 * 1-to-many relationship for a DayTemplate with its list of TemplateItemEntities.
 */
data class TemplateWithItems(
    @Embedded
    val template: DayTemplateEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "template_id"
    )
    val items: List<TemplateItemEntity>
)
