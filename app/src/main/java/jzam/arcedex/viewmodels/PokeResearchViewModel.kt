package jzam.arcedex.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import jzam.arcedex.data.PokeAreaData
import jzam.arcedex.data.PokeResearchRepository
import jzam.arcedex.data.Pokedex
import jzam.arcedex.models.*
import jzam.arcedex.utils.getTaskCategory
import jzam.arcedex.utils.taskCategoryTypeOf
import jzam.arcedex.utils.translate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/*
 * ViewModel for the main (and currently the only) Arcedex screen
 */
class PokeResearchViewModel(
    private val repository: PokeResearchRepository
) : ViewModel() {

    //List of all Pokemon research tasks - collected live from the Room database as a StateFlow,
    //so it updates automatically whenever the underlying table changes.
    val researchTasks: StateFlow<List<PokeResearch>> = repository.getResearchTasks()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    //List of all Pokemon
    private val _pokedex: MutableStateFlow<List<Pokemon>> =
        MutableStateFlow(Pokedex.pokedex.sortedBy { it.hisuiId })
    val pokedex: StateFlow<List<Pokemon>> = _pokedex.asStateFlow()

    //List of objects that track how much research progress has been completed for each Pokemon
    private val _researchProgress: MutableStateFlow<List<ResearchProgress>> =
        MutableStateFlow(emptyList())
    val researchProgress: StateFlow<List<ResearchProgress>> = _researchProgress.asStateFlow()

    //The current sort of this screen
    private val _pokesort: MutableStateFlow<PokeSort> = MutableStateFlow(PokeSort.HISUI)
    val pokesort: StateFlow<PokeSort> = _pokesort.asStateFlow()

    //Is the search box ready to receive input?
    private val _inSearchMode: MutableStateFlow<Boolean> = MutableStateFlow(true)
    val inSearchMode: StateFlow<Boolean> = _inSearchMode.asStateFlow()

    //The text that has been searched for
    private val _searchedText: MutableStateFlow<String> = MutableStateFlow("")
    val searchedText: StateFlow<String> = _searchedText.asStateFlow()

    //Sum of all research points to calculate the user's research rank
    private val _userPoints: MutableStateFlow<Int> = MutableStateFlow(0)
    val userPoints: StateFlow<Int> = _userPoints.asStateFlow()

    //Map of a Pokemon name to the list of research tasks associated to it for quicker processing
    private val _pokemonToResearchTasks: MutableStateFlow<Map<String, List<PokeResearch>>> =
        MutableStateFlow(emptyMap())
    val pokemonToResearchTasks: StateFlow<Map<String, List<PokeResearch>>> =
        _pokemonToResearchTasks.asStateFlow()

    //Whether completed Pokemon and completed tasks should be hidden from view
    //3-state filter for the main Pokemon list: show all, hide Rank10+, or hide only Perfect
    private val _hideFilter: MutableStateFlow<HideFilter> = MutableStateFlow(HideFilter.SHOW_ALL)
    val hideFilter: StateFlow<HideFilter> = _hideFilter.asStateFlow()

    //Which Hisui region the Pokemon list is currently filtered to, null means no filter (all regions)
    private val _selectedArea: MutableStateFlow<HisuiArea?> = MutableStateFlow(null)
    val selectedArea: StateFlow<HisuiArea?> = _selectedArea.asStateFlow()

    //Which research task category the list is filtered to, null means no filter
    private val _selectedCategory: MutableStateFlow<TaskCategory?> = MutableStateFlow(null)
    val selectedCategory: StateFlow<TaskCategory?> = _selectedCategory.asStateFlow()

    //Secondary move-type filter, only meaningful when selectedCategory is DEFEAT or MOVE_SEEN
    private val _selectedCategoryType: MutableStateFlow<String?> = MutableStateFlow(null)
    val selectedCategoryType: StateFlow<String?> = _selectedCategoryType.asStateFlow()

    private var language = SupportedLanguage.ENGLISH

    //Combined StateFlow that handles filtering off the main thread
    val filteredPokedex: StateFlow<List<Pokemon>> = combine(
        pokedex,
        researchProgress,
        hideFilter,
        selectedArea,
        selectedCategory,
        selectedCategoryType,
        pokemonToResearchTasks
    ) { pokedexList, progressList, hide, area, category, categoryType, tasksMap ->
        val progressByName = progressList.associateBy { it.name }
        pokedexList.filter { pokemon ->
            val prog = progressByName[pokemon.name]
            when (hide) {
                HideFilter.SHOW_ALL -> true
                HideFilter.HIDE_RANK10 -> prog == null || prog.pointsDone < 100
                HideFilter.HIDE_PERFECT -> prog == null || (prog.pointsDone + prog.bonusEarned) < prog.pointsTotal
            }
        }.filter { pokemon ->
            area == null || PokeAreaData.getAreas(pokemon.hisuiId).contains(area)
        }.filter { pokemon ->
            if (category == null) return@filter true
            val tasks = tasksMap[pokemon.name] ?: return@filter false
            tasks.any { task ->
                getTaskCategory(task.task) == category &&
                        task.goalProgress < task.totalGoals &&
                        (categoryType == null || taskCategoryTypeOf(category, task.task) == categoryType)
            }
        }
    }.flowOn(Dispatchers.Default)
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = Pokedex.pokedex.sortedBy { it.hisuiId }
    )

    //Calculates research progress for each Pokemon and builds map of Pokemon name to their research tasks on Dispatchers.Default
    fun calcProgress() {
        viewModelScope.launch(Dispatchers.Default) {
            val taskList = researchTasks.value
            var points = 0
            val progList: MutableList<ResearchProgress> = mutableListOf()
            val mapPokemonToResearchTasks: MutableMap<String, MutableList<PokeResearch>> =
                mutableMapOf()
            val nameToProgListIdx: MutableMap<String, Int> = mutableMapOf()

            for (task in taskList) {
                val progIdx = nameToProgListIdx[task.name]
                if (progIdx != null) {
                    val prog = progList[progIdx]
                    prog.goalsDone += task.goalProgress
                    prog.goalsTotal += task.totalGoals
                    prog.pointsDone += task.goalProgress * task.points
                    prog.pointsTotal += task.totalGoals * task.points
                    prog.bonusEarned = if (prog.pointsDone >= 100) 100 else 0
                } else {
                    val newProgress = ResearchProgress(
                        name = task.name,
                        goalsDone = task.goalProgress,
                        goalsTotal = task.totalGoals,
                        pointsDone = task.goalProgress * task.points,
                        pointsTotal = task.totalGoals * task.points + 100
                    )
                    newProgress.bonusEarned = if (newProgress.pointsDone >= 100) 100 else 0
                    progList.add(newProgress)
                    nameToProgListIdx[task.name] = progList.size - 1
                }
                mapPokemonToResearchTasks.getOrPut(task.name) { mutableListOf() }.add(task)
            }
            for (item in progList) {
                points += item.pointsDone + item.bonusEarned
            }
            _researchProgress.value = progList
            _pokemonToResearchTasks.value = mapPokemonToResearchTasks
            _userPoints.value = points
        }
    }

    //Sorts a Pokemon list according to the current sort mode.
    private fun sortedPokedex(list: List<Pokemon>): List<Pokemon> {
        return when (_pokesort.value) {
            PokeSort.ALPHABETICAL -> list.sortedBy { translate(language, it.name) }
            PokeSort.NATIONAL -> list.sortedBy { it.natId }
            PokeSort.RESEARCH_LEVEL -> {
                val progressByName = _researchProgress.value.associateBy { it.name }
                list.sortedWith(
                    compareByDescending<Pokemon> { pokemon ->
                        val prog = progressByName[pokemon.name]
                        (prog?.pointsDone ?: 0) + (prog?.bonusEarned ?: 0)
                    }.thenBy { it.hisuiId }
                )
            }
            else -> list.sortedBy { it.hisuiId }
        }
    }

    //Set and execute the sorting off main thread.
    fun setSort(sort: PokeSort) {
        _pokesort.value = sort
        viewModelScope.launch(Dispatchers.Default) {
            _pokedex.value = sortedPokedex(pokedex.value)
        }
    }

    //Search for given text in research task list off main thread.
    fun searchPokedex(searchText: String) {
        val trimmed = searchText.trim()
        if (trimmed.isBlank()) {
            searchClear()
            return
        }
        viewModelScope.launch(Dispatchers.Default) {
            val matchingPokemon: MutableList<Pokemon> = mutableListOf()
            val addedNames = mutableSetOf<String>()
            val taskList = researchTasks.value
            val findText = translate(language, trimmed).lowercase()

            for (task in taskList) {
                if (addedNames.contains(task.name)) continue
                val translatedName = translate(language, task.name).lowercase()
                val translatedTask = translate(language, task.task).lowercase()
                if (translatedName.contains(findText) || translatedTask.contains(findText)) {
                    val pokemon = Pokedex.pokemonByName[task.name]
                    if (pokemon != null) {
                        matchingPokemon.add(pokemon)
                        addedNames.add(task.name)
                    }
                }
            }
            _pokedex.value = sortedPokedex(matchingPokemon)
            _inSearchMode.value = false
            _searchedText.value = trimmed
        }
    }

    //Reset the search variables off main thread
    fun searchClear() {
        viewModelScope.launch(Dispatchers.Default) {
            _pokedex.value = sortedPokedex(Pokedex.pokedex)
            _searchedText.value = ""
            _inSearchMode.value = true
        }
    }

    //Set the goal progress for a task based on what the user clicked.
    fun onGoalClick(task: PokeResearch, goalNum: Int) {
        viewModelScope.launch {
            val savTask = task.copy()
            if (goalNum == task.goalProgress) {
                savTask.goalProgress = 0
            } else {
                savTask.goalProgress = goalNum
            }
            repository.update(savTask)
        }
    }

    fun setLanguage(lang: SupportedLanguage) {
        language = lang
    }

    //Builds a copy-pasteable backup string of current research progress
    fun exportProgressBackup(): String = jzam.arcedex.utils.exportProgress(researchTasks.value)

    //Parses and applies a backup string, returning the number of tasks that were actually updated.
    suspend fun importProgressBackup(backup: String): Int {
        val entries = jzam.arcedex.utils.parseBackup(backup)
        val currentTasks = researchTasks.value
        var appliedCount = 0
        for ((name, task, progress) in entries) {
            val match = currentTasks.find { it.name == name && it.task == task }
            if (match != null && match.goalProgress != progress) {
                repository.update(match.copy(goalProgress = progress))
                appliedCount++
            }
        }
        return appliedCount
    }

    //Flip the hide-completed filter on/off
    //Cycle SHOW_ALL -> HIDE_RANK10 -> HIDE_PERFECT -> SHOW_ALL
    fun cycleHideFilter() {
        _hideFilter.value = when (_hideFilter.value) {
            HideFilter.SHOW_ALL -> HideFilter.HIDE_RANK10
            HideFilter.HIDE_RANK10 -> HideFilter.HIDE_PERFECT
            HideFilter.HIDE_PERFECT -> HideFilter.SHOW_ALL
        }
    }

    //Set which Hisui region to filter the Pokemon list to, or null to clear the filter
    fun setAreaFilter(area: HisuiArea?) {
        _selectedArea.value = area
    }

    //Set which research task category to filter the Pokemon list to.
    fun setCategoryFilter(category: TaskCategory?) {
        _selectedCategory.value = category
        _selectedCategoryType.value = null
    }

    //Set the secondary type filter (only meaningful under Defeat/Move Seen), or null to clear it
    fun setCategoryTypeFilter(type: String?) {
        _selectedCategoryType.value = type
    }
}

//Boilerplate view model factory code
class PokeResearchViewModelFactory(
    private val repository: PokeResearchRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PokeResearchViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PokeResearchViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}