package com.personal.daymark.data.repository

import com.personal.daymark.data.local.dao.CategoryDao
import com.personal.daymark.data.local.dao.TaskDao
import com.personal.daymark.data.local.entity.CategoryEntity
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

interface CategoryRepository {
    fun getAllCategories(): Flow<List<CategoryEntity>>
    fun getCategoryById(id: Long): Flow<CategoryEntity?>
    suspend fun createCategory(name: String, colorHex: String, iconName: String? = null, sortOrder: Int = 0): Long
    suspend fun updateCategory(category: CategoryEntity)
    suspend fun deleteCategory(category: CategoryEntity)
    suspend fun deleteCategoryById(id: Long)
    suspend fun seedDefaultCategoriesIfEmpty()
}

class CategoryRepositoryImpl(
    private val categoryDao: CategoryDao,
    private val taskDao: TaskDao? = null,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : CategoryRepository {

    override fun getAllCategories(): Flow<List<CategoryEntity>> {
        return categoryDao.getAllCategories().flowOn(ioDispatcher)
    }

    override fun getCategoryById(id: Long): Flow<CategoryEntity?> {
        return categoryDao.getCategoryById(id).flowOn(ioDispatcher)
    }

    override suspend fun createCategory(
        name: String,
        colorHex: String,
        iconName: String?,
        sortOrder: Int
    ): Long = withContext(ioDispatcher) {
        val entity = CategoryEntity(
            name = name.trim(),
            colorHex = colorHex,
            iconName = iconName,
            sortOrder = sortOrder
        )
        categoryDao.insertCategory(entity)
    }

    override suspend fun updateCategory(category: CategoryEntity) = withContext(ioDispatcher) {
        categoryDao.updateCategory(category.copy(name = category.name.trim()))
    }

    override suspend fun deleteCategory(category: CategoryEntity) = withContext(ioDispatcher) {
        deleteCategoryById(category.id)
    }

    override suspend fun deleteCategoryById(id: Long) = withContext(ioDispatcher) {
        // Safe deletion: Nullify category references on tasks before removing category
        taskDao?.clearCategoryFromTasks(id)
        categoryDao.deleteCategoryById(id)
    }

    override suspend fun seedDefaultCategoriesIfEmpty() = withContext(ioDispatcher) {
        val existing = categoryDao.getAllCategories().firstOrNull().orEmpty()
        if (existing.isEmpty()) {
            val now = System.currentTimeMillis()
            val defaults = listOf(
                CategoryEntity(name = "Personal", colorHex = "#40916C", sortOrder = 0, createdAt = now),
                CategoryEntity(name = "Work", colorHex = "#1B4332", sortOrder = 1, createdAt = now),
                CategoryEntity(name = "Study", colorHex = "#4A6FA5", sortOrder = 2, createdAt = now),
                CategoryEntity(name = "Health", colorHex = "#D97706", sortOrder = 3, createdAt = now),
                CategoryEntity(name = "Other", colorHex = "#6B7280", sortOrder = 4, createdAt = now)
            )
            categoryDao.insertCategories(defaults)
        }
    }
}
