package com.personal.daymark.ui.categories

import com.personal.daymark.data.local.entity.CategoryEntity
import com.personal.daymark.data.repository.CategoryRepository
import com.personal.daymark.ui.screens.categories.ManageCategoriesViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ManageCategoriesViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private class FakeCategoryRepository : CategoryRepository {
        val categories = MutableStateFlow<List<CategoryEntity>>(
            listOf(
                CategoryEntity(id = 1L, name = "Personal", colorHex = "#40916C"),
                CategoryEntity(id = 2L, name = "Work", colorHex = "#1B4332")
            )
        )

        override fun getAllCategories(): Flow<List<CategoryEntity>> = categories.asStateFlow()
        override fun getCategoryById(id: Long): Flow<CategoryEntity?> =
            flowOf(categories.value.find { it.id == id })

        override suspend fun createCategory(name: String, colorHex: String, iconName: String?, sortOrder: Int): Long {
            val id = (categories.value.size + 1).toLong()
            categories.value += CategoryEntity(id = id, name = name, colorHex = colorHex, sortOrder = sortOrder)
            return id
        }

        override suspend fun updateCategory(category: CategoryEntity) {
            categories.value = categories.value.map {
                if (it.id == category.id) category else it
            }
        }

        override suspend fun deleteCategory(category: CategoryEntity) {
            categories.value = categories.value.filterNot { it.id == category.id }
        }

        override suspend fun deleteCategoryById(id: Long) {
            categories.value = categories.value.filterNot { it.id == id }
        }

        override suspend fun seedDefaultCategoriesIfEmpty() {}
    }

    private lateinit var fakeRepo: FakeCategoryRepository
    private lateinit var viewModel: ManageCategoriesViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeRepo = FakeCategoryRepository()
        viewModel = ManageCategoriesViewModel(fakeRepo)
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialCategoriesAreLoaded() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.categories.collect()
        }
        advanceUntilIdle()

        val cats = viewModel.categories.value
        assertEquals(2, cats.size)
        assertEquals("Personal", cats[0].name)
    }

    @Test
    fun addCategory_validInputCreatesCategoryAndClearsFields() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.categories.collect()
        }
        advanceUntilIdle()

        viewModel.startCreateCategory()
        viewModel.onNameChanged("Finance")
        viewModel.onColorSelected("#4A6FA5")
        viewModel.saveCategory()
        advanceUntilIdle()

        val cats = viewModel.categories.value
        assertEquals(3, cats.size)
        assertTrue(cats.any { it.name == "Finance" && it.colorHex == "#4A6FA5" })

        val state = viewModel.uiState.value
        assertEquals("", state.nameInput)
        assertNull(state.editingCategory)
    }

    @Test
    fun addCategory_blankNameSetsError() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.categories.collect()
        }
        advanceUntilIdle()

        viewModel.startCreateCategory()
        viewModel.onNameChanged("   ")
        viewModel.saveCategory()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Category name cannot be empty", state.errorMessage)
    }

    @Test
    fun editCategory_loadsCategoryForEditingAndSavesUpdates() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.categories.collect()
        }
        advanceUntilIdle()

        val toEdit = fakeRepo.categories.value.first()
        viewModel.startEditCategory(toEdit)

        val stateEditing = viewModel.uiState.value
        assertEquals(toEdit.id, stateEditing.editingCategory?.id)
        assertEquals("Personal", stateEditing.nameInput)

        viewModel.onNameChanged("Personal Life")
        viewModel.saveCategory()
        advanceUntilIdle()

        val updatedCats = viewModel.categories.value
        assertTrue(updatedCats.any { it.name == "Personal Life" })
    }

    @Test
    fun deleteCategory_requestsConfirmationAndDeletesSafely() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.categories.collect()
        }
        advanceUntilIdle()

        val toDelete = fakeRepo.categories.value.last()
        viewModel.promptDelete(toDelete)

        val stateConfirm = viewModel.uiState.value
        assertEquals(toDelete.id, stateConfirm.categoryPendingDelete?.id)

        viewModel.confirmDeleteCategory()
        advanceUntilIdle()

        val catsAfter = viewModel.categories.value
        assertNull(viewModel.uiState.value.categoryPendingDelete)
        assertEquals(1, catsAfter.size)
        assertEquals("Personal", catsAfter.first().name)
    }
}
