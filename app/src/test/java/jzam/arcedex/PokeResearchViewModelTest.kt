package jzam.arcedex

import jzam.arcedex.data.PokeResearchDao
import jzam.arcedex.data.PokeResearchRepository
import jzam.arcedex.models.*
import jzam.arcedex.viewmodels.PokeResearchViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import kotlinx.coroutines.withContext
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

    //Waits (on a real dispatcher, not the virtual test clock) for a condition to become true.
    //Needed because calcProgress()/searchPokedex()/searchClear() hardcode Dispatchers.Default
    //internally, so their work isn't tracked by runTest's virtual-time scheduler.
    private suspend fun awaitUntil(timeoutMs: Long = 3_000, condition: () -> Boolean) {
        withContext(Dispatchers.Default) {
            val deadline = System.currentTimeMillis() + timeoutMs
            while (!condition()) {
                if (System.currentTimeMillis() > deadline) {
                    throw AssertionError("Timed out waiting for condition")
                }
                delay(10)
            }
        }
    }

    //Rowlet (hisuiId 1) only appears in Coronet Highlands; Eevee (hisuiId 25) appears in every
    //Hisui area (verified directly against PokeAreaData.kt). Used here to exercise the real
    //area-filter branch of filteredPokedex without depending on PokeResearchData.kt's task text.
    @Test
    fun testFilteredPokedexAreaAndCategoryFilters() = runTest {
        val rowletCatchTask = PokeResearch(
            name = "Rowlet", task = "Number caught",
            goal1 = "1", goal2 = "", goal3 = "", goal4 = "", goal5 = "",
            points = 10, goalProgress = 0, totalGoals = 1
        )
        val eeveeDefeatFireTask = PokeResearch(
            name = "Eevee", task = "Number you\u2019ve defeated with Fire-type moves",
            goal1 = "1", goal2 = "", goal3 = "", goal4 = "", goal5 = "",
            points = 10, goalProgress = 0, totalGoals = 1
        )
        val tasksFlow = MutableStateFlow(listOf(rowletCatchTask, eeveeDefeatFireTask))
        val fakeDao = FakePokeResearchDao(tasksFlow)
        val repository = PokeResearchRepository(fakeDao)
        val viewModel = PokeResearchViewModel(repository)

        val researchTasksJob = backgroundScope.launch { viewModel.researchTasks.collect {} }
        val filteredJob = backgroundScope.launch { viewModel.filteredPokedex.collect {} }
        advanceUntilIdle()

        viewModel.calcProgress()
        awaitUntil { viewModel.pokemonToResearchTasks.value.isNotEmpty() }

        // Area filter: Rowlet isn't in Obsidian Fieldlands, Eevee is in every area.
        // NOTE: both are already visible in the unfiltered list, so we must wait for the FULL
        // combined end-state (Rowlet gone AND Eevee still there), not just "Eevee is present" -
        // that alone would be true even before the filter has actually been applied.
        viewModel.setAreaFilter(HisuiArea.OBSIDIAN_FIELDLANDS)
        awaitUntil {
            val v = viewModel.filteredPokedex.value.map { it.name }
            !v.contains("Rowlet") && v.contains("Eevee")
        }
        var visible = viewModel.filteredPokedex.value.map { it.name }
        assertFalse("Rowlet isn't in Obsidian Fieldlands, should be filtered out", visible.contains("Rowlet"))
        assertTrue("Eevee is in every area, should stay visible", visible.contains("Eevee"))
        viewModel.setAreaFilter(null)

        // Category filter: only Rowlet has an incomplete CATCH task.
        viewModel.setCategoryFilter(TaskCategory.CATCH)
        awaitUntil {
            val v = viewModel.filteredPokedex.value.map { it.name }
            v.contains("Rowlet") && !v.contains("Eevee")
        }
        visible = viewModel.filteredPokedex.value.map { it.name }
        assertTrue(visible.contains("Rowlet"))
        assertFalse(visible.contains("Eevee"))

        // Category + type filter: only Eevee has a Fire-type Defeat task.
        viewModel.setCategoryFilter(TaskCategory.DEFEAT)
        viewModel.setCategoryTypeFilter("FIRE")
        awaitUntil {
            val v = viewModel.filteredPokedex.value.map { it.name }
            v.contains("Eevee") && !v.contains("Rowlet")
        }
        visible = viewModel.filteredPokedex.value.map { it.name }
        assertTrue(visible.contains("Eevee"))
        assertFalse(visible.contains("Rowlet"))

        // Wrong move-type filter excludes it too.
        viewModel.setCategoryTypeFilter("WATER")
        awaitUntil {
            val v = viewModel.filteredPokedex.value.map { it.name }
            !v.contains("Eevee") && !v.contains("Rowlet")
        }
        visible = viewModel.filteredPokedex.value.map { it.name }
        assertFalse(visible.contains("Eevee"))

        researchTasksJob.cancel()
        filteredJob.cancel()
    }

    //Rowlet gets two tasks so it crosses the 100-point Rank10 threshold (pointsDone == 100) but
    //still has an unfinished second task, so it's not "Perfect". Eevee gets a single fully-done
    //task, so it hits both Rank10 and Perfect at once. This exercises the actual arithmetic in
    //calcProgress(), not just the filter branch, since HIDE_RANK10 and HIDE_PERFECT use different
    //fields (pointsDone vs pointsDone+bonusEarned==pointsTotal).
    @Test
    fun testFilteredPokedexHideFilterDistinguishesRank10FromPerfect() = runTest {
        val rowletTaskDone = PokeResearch(
            name = "Rowlet", task = "Number caught",
            goal1 = "1", goal2 = "", goal3 = "", goal4 = "", goal5 = "",
            points = 100, goalProgress = 1, totalGoals = 1
        )
        val rowletTaskNotDone = PokeResearch(
            name = "Rowlet", task = "Number of alpha specimens caught",
            goal1 = "1", goal2 = "", goal3 = "", goal4 = "", goal5 = "",
            points = 50, goalProgress = 0, totalGoals = 1
        )
        val eeveeTaskDone = PokeResearch(
            name = "Eevee", task = "Number caught",
            goal1 = "1", goal2 = "", goal3 = "", goal4 = "", goal5 = "",
            points = 100, goalProgress = 1, totalGoals = 1
        )
        val tasksFlow = MutableStateFlow(listOf(rowletTaskDone, rowletTaskNotDone, eeveeTaskDone))
        val fakeDao = FakePokeResearchDao(tasksFlow)
        val repository = PokeResearchRepository(fakeDao)
        val viewModel = PokeResearchViewModel(repository)

        val researchTasksJob = backgroundScope.launch { viewModel.researchTasks.collect {} }
        val filteredJob = backgroundScope.launch { viewModel.filteredPokedex.collect {} }
        advanceUntilIdle()

        viewModel.calcProgress()
        awaitUntil { viewModel.researchProgress.value.size == 2 }

        val rowletProg = viewModel.researchProgress.value.first { it.name == "Rowlet" }
        val eeveeProg = viewModel.researchProgress.value.first { it.name == "Eevee" }
        assertEquals(100, rowletProg.pointsDone)
        assertTrue("Rowlet should not be Perfect (second task unfinished)", rowletProg.pointsDone + rowletProg.bonusEarned < rowletProg.pointsTotal)
        assertEquals(100, eeveeProg.pointsDone)
        assertEquals("Eevee's single task should be Perfect", eeveeProg.pointsTotal, eeveeProg.pointsDone + eeveeProg.bonusEarned)

        // HIDE_RANK10 hides anyone with pointsDone >= 100 - both Rowlet and Eevee qualify.
        viewModel.cycleHideFilter()
        awaitUntil {
            val v = viewModel.filteredPokedex.value.map { it.name }
            !v.contains("Rowlet") && !v.contains("Eevee")
        }
        var visible = viewModel.filteredPokedex.value.map { it.name }
        assertFalse(visible.contains("Rowlet"))
        assertFalse(visible.contains("Eevee"))

        // HIDE_PERFECT only hides fully-completed Pokemon - Rowlet still has an open task, Eevee doesn't.
        viewModel.cycleHideFilter()
        awaitUntil {
            val v = viewModel.filteredPokedex.value.map { it.name }
            v.contains("Rowlet") && !v.contains("Eevee")
        }
        visible = viewModel.filteredPokedex.value.map { it.name }
        assertTrue("Rowlet has an unfinished task, shouldn't be hidden by HIDE_PERFECT", visible.contains("Rowlet"))
        assertFalse("Eevee is fully done, should be hidden by HIDE_PERFECT", visible.contains("Eevee"))

        researchTasksJob.cancel()
        filteredJob.cancel()
    }

    @Test
    fun testSearchPokedexFindsByNameAndTaskThenClears() = runTest {
        val rowletTask = PokeResearch(
            name = "Rowlet", task = "Number caught",
            goal1 = "1", goal2 = "", goal3 = "", goal4 = "", goal5 = "",
            points = 10, goalProgress = 0, totalGoals = 1
        )
        val eeveeTask = PokeResearch(
            name = "Eevee", task = "Number you\u2019ve seen leap out of ore deposits",
            goal1 = "1", goal2 = "", goal3 = "", goal4 = "", goal5 = "",
            points = 10, goalProgress = 0, totalGoals = 1
        )
        val tasksFlow = MutableStateFlow(listOf(rowletTask, eeveeTask))
        val fakeDao = FakePokeResearchDao(tasksFlow)
        val repository = PokeResearchRepository(fakeDao)
        val viewModel = PokeResearchViewModel(repository)

        val researchTasksJob = backgroundScope.launch { viewModel.researchTasks.collect {} }
        advanceUntilIdle()

        // Search by (partial, case-insensitive) Pokemon name.
        viewModel.searchPokedex("rowlet")
        awaitUntil { viewModel.searchedText.value == "rowlet" }
        assertEquals(listOf("Rowlet"), viewModel.pokedex.value.map { it.name })
        assertFalse(viewModel.inSearchMode.value)

        // Search by (partial) task text instead of name.
        viewModel.searchPokedex("ore deposits")
        awaitUntil { viewModel.searchedText.value == "ore deposits" }
        assertEquals(listOf("Eevee"), viewModel.pokedex.value.map { it.name })

        // A blank search clears back to the full Pokedex and re-enables search mode.
        viewModel.searchPokedex("   ")
        awaitUntil { viewModel.inSearchMode.value }
        assertTrue(
            "Clearing search should restore the full 242-entry Pokedex",
            viewModel.pokedex.value.size > 2
        )
        assertEquals("", viewModel.searchedText.value)

        researchTasksJob.cancel()
    }
}