package com.personal.daymark.data.repository

import com.personal.daymark.data.local.dao.DayTemplateDao
import com.personal.daymark.data.local.dao.TaskDao
import com.personal.daymark.data.local.dao.TimeBlockDao
import com.personal.daymark.data.local.entity.DayTemplateEntity
import com.personal.daymark.data.local.entity.TaskEntity
import com.personal.daymark.data.local.entity.TemplateItemEntity
import com.personal.daymark.data.local.entity.TemplateWithItems
import com.personal.daymark.data.local.entity.TimeBlockEntity
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId

data class ApplyTemplateResult(
    val templateName: String,
    val tasksAdded: Int,
    val busyBlocksAdded: Int,
    val skippedDuplicates: Int
)

interface TemplateRepository {
    fun getAllTemplatesWithItems(): Flow<List<TemplateWithItems>>
    suspend fun getTemplateWithItemsById(id: Long): TemplateWithItems?
    suspend fun createTemplate(name: String, items: List<TemplateItemEntity>): Long
    suspend fun updateTemplate(template: DayTemplateEntity, items: List<TemplateItemEntity>)
    suspend fun deleteTemplate(id: Long)
    suspend fun addTemplateItem(item: TemplateItemEntity): Long
    suspend fun deleteTemplateItem(item: TemplateItemEntity)
    suspend fun applyTemplateToDate(templateId: Long, date: LocalDate): ApplyTemplateResult
}

class TemplateRepositoryImpl(
    private val dayTemplateDao: DayTemplateDao,
    private val taskDao: TaskDao,
    private val timeBlockDao: TimeBlockDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : TemplateRepository {

    override fun getAllTemplatesWithItems(): Flow<List<TemplateWithItems>> {
        return dayTemplateDao.getAllTemplatesWithItems().flowOn(ioDispatcher)
    }

    override suspend fun getTemplateWithItemsById(id: Long): TemplateWithItems? =
        withContext(ioDispatcher) {
            dayTemplateDao.getTemplateWithItemsById(id)
        }

    override suspend fun createTemplate(name: String, items: List<TemplateItemEntity>): Long =
        withContext(ioDispatcher) {
            val templateId = dayTemplateDao.insertTemplate(DayTemplateEntity(name = name))
            val itemsWithId = items.map { it.copy(templateId = templateId) }
            if (itemsWithId.isNotEmpty()) {
                dayTemplateDao.insertTemplateItems(itemsWithId)
            }
            templateId
        }

    override suspend fun updateTemplate(template: DayTemplateEntity, items: List<TemplateItemEntity>) =
        withContext(ioDispatcher) {
            dayTemplateDao.updateTemplate(template)
            dayTemplateDao.deleteItemsForTemplate(template.id)
            val itemsWithId = items.map { it.copy(templateId = template.id) }
            if (itemsWithId.isNotEmpty()) {
                dayTemplateDao.insertTemplateItems(itemsWithId)
            }
        }

    override suspend fun deleteTemplate(id: Long) =
        withContext(ioDispatcher) {
            dayTemplateDao.deleteTemplateById(id)
        }

    override suspend fun addTemplateItem(item: TemplateItemEntity): Long =
        withContext(ioDispatcher) {
            dayTemplateDao.insertTemplateItem(item)
        }

    override suspend fun deleteTemplateItem(item: TemplateItemEntity) =
        withContext(ioDispatcher) {
            dayTemplateDao.deleteTemplateItem(item)
        }

    override suspend fun applyTemplateToDate(templateId: Long, date: LocalDate): ApplyTemplateResult =
        withContext(ioDispatcher) {
            val templateWithItems = dayTemplateDao.getTemplateWithItemsById(templateId)
                ?: return@withContext ApplyTemplateResult("", 0, 0, 0)

            val zoneId = ZoneId.systemDefault()
            val startOfDay = date.atStartOfDay(zoneId).toInstant().toEpochMilli()
            val endOfDay = date.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli() - 1
            val dateStr = date.toString() // "yyyy-MM-dd"

            val existingTasks = taskDao.getTasksInDateRangeSync(startOfDay, endOfDay)
            val existingBusyBlocks = timeBlockDao.getTimeBlocksForDateSync(dateStr)

            var tasksAdded = 0
            var busyBlocksAdded = 0
            var skippedDuplicates = 0

            for (item in templateWithItems.items) {
                if (item.isTask) {
                    // Check duplicate task by matching title and start time (or due time)
                    val isDuplicate = existingTasks.any { task ->
                        task.title.trim().equals(item.title.trim(), ignoreCase = true) &&
                                (task.startTime == item.startTime || task.getEffectiveStartTime() == item.startTime)
                    }

                    if (isDuplicate) {
                        skippedDuplicates++
                    } else {
                        val dueMinutes = parseTimeToMinutes(item.startTime)
                        val task = TaskEntity(
                            title = item.title,
                            dueDate = startOfDay,
                            dueTime = dueMinutes,
                            startTime = item.startTime,
                            durationMinutes = item.durationMinutes,
                            createdAt = System.currentTimeMillis()
                        )
                        taskDao.insertTask(task)
                        tasksAdded++
                    }
                } else {
                    // Busy Block: Check duplicate busy block by matching title and start time
                    val isDuplicate = existingBusyBlocks.any { block ->
                        block.title.trim().equals(item.title.trim(), ignoreCase = true) &&
                                block.startTime == item.startTime
                    }

                    if (isDuplicate) {
                        skippedDuplicates++
                    } else {
                        val busyBlock = TimeBlockEntity(
                            title = item.title,
                            startTime = item.startTime,
                            durationMinutes = item.durationMinutes,
                            colorHex = null,
                            date = dateStr
                        )
                        timeBlockDao.insert(busyBlock)
                        busyBlocksAdded++
                    }
                }
            }

            ApplyTemplateResult(
                templateName = templateWithItems.template.name,
                tasksAdded = tasksAdded,
                busyBlocksAdded = busyBlocksAdded,
                skippedDuplicates = skippedDuplicates
            )
        }

    private fun parseTimeToMinutes(timeString: String): Int? {
        val parts = timeString.split(":")
        if (parts.size >= 2) {
            val h = parts[0].trim().toIntOrNull() ?: return null
            val m = parts[1].trim().toIntOrNull() ?: return null
            return h * 60 + m
        }
        return null
    }
}
