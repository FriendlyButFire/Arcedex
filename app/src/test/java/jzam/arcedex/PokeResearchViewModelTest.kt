package jzam.arcedex

import jzam.arcedex.data.PokeResearchDao
import jzam.arcedex.data.PokeResearchRepository
import jzam.arcedex.models.*
import jzam.arcedex.viewmodels.PokeResearchViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PokeResearchViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    class FakePokeResearchDao(
        private val tasksFlow: MutableStateFlow<List<PokeResearch>>
    ) : PokeResearchDao {
        override fun getResearchTasks(): Flow<List<PokeResearch>> = tasksFlow
        override suspend fun insert(task: PokeResearch) {}
        override suspend fun insertAll(tasks: List<PokeResearch>) {}
        override suspend fun update(task: PokeResearch) {
            val current = tasksFlow.value.toMutableList()
            val idx = current.indexOfFirst { it.id == task.id }
            if (idx != -1) {
                current[idx] = task
                tasksFlow.value = current
            }
        }
        override suspend fun delete(task: PokeResearch) {}
        override suspend fun deleteAll() {}
        override suspend fun getCount(): Int = tasksFlow.value.size
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testViewModelInitializationAndFilterCycles() = runTest {
        val tasksFlow = MutableStateFlow<List<PokeResearch>>(emptyList())
        val fakeDao = FakePokeResearchDao(tasksFlow)
        val repository = PokeResearchRepository(fakeDao)
        val viewModel = PokeResearchViewModel(repository)

        assertEquals(HideFilter.SHOW_ALL, viewModel.hideFilter.value)
        viewModel.cycleHideFilter()
        assertEquals(HideFilter.HIDE_RANK10, viewModel.hideFilter.value)
        viewModel.cycleHideFilter()
        assertEquals(HideFilter.HIDE_PERFECT, viewModel.hideFilter.value)
        viewModel.cycleHideFilter()
        assertEquals(HideFilter.SHOW_ALL, viewModel.hideFilter.value)
    }

    @Test
    fun testSetAreaAndCategoryFilter() = runTest {
        val tasksFlow = MutableStateFlow<List<PokeResearch>>(emptyList())
        val fakeDao = FakePokeResearchDao(tasksFlow)
        val repository = PokeResearchRepository(fakeDao)
        val viewModel = PokeResearchViewModel(repository)

        assertNull(viewModel.selectedArea.value)
        viewModel.setAreaFilter(HisuiArea.OBSIDIAN_FIELDLANDS)
        assertEquals(HisuiArea.OBSIDIAN_FIELDLANDS, viewModel.selectedArea.value)

        assertNull(viewModel.selectedCategory.value)
        viewModel.setCategoryFilter(TaskCategory.CATCH)
        assertEquals(TaskCategory.CATCH, viewModel.selectedCategory.value)
    }
}
