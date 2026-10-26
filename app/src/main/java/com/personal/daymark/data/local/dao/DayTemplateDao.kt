package com.personal.daymark.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.personal.daymark.data.local.entity.DayTemplateEntity
import com.personal.daymark.data.local.entity.TemplateItemEntity
import com.personal.daymark.data.local.entity.TemplateWithItems
import kotlinx.coroutines.flow.Flow

@Dao
interface DayTemplateDao {

    @Transaction
    @Query("SELECT * FROM day_templates ORDER BY name ASC")
    fun getAllTemplatesWithItems(): Flow<List<TemplateWithItems>>

    @Transaction
    @Query("SELECT * FROM day_templates WHERE id = :id LIMIT 1")
    suspend fun getTemplateWithItemsById(id: Long): TemplateWithItems?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplate(template: DayTemplateEntity): Long

    @Update
    suspend fun updateTemplate(template: DayTemplateEntity)

    @Delete
    suspend fun deleteTemplate(template: DayTemplateEntity)

    @Query("DELETE FROM day_templates WHERE id = :id")
    suspend fun deleteTemplateById(id: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplateItem(item: TemplateItemEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplateItems(items: List<TemplateItemEntity>): List<Long>

    @Update
    suspend fun updateTemplateItem(item: TemplateItemEntity)

    @Delete
    suspend fun deleteTemplateItem(item: TemplateItemEntity)

    @Query("DELETE FROM template_items WHERE template_id = :templateId")
    suspend fun deleteItemsForTemplate(templateId: Long)
}
