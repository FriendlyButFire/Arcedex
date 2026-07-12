package jzam.arcedex.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import jzam.arcedex.data.PokeResearchRepository
import jzam.arcedex.data.Pokedex
import jzam.arcedex.models.*
import jzam.arcedex.utils.translate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    private val _pokemonToResearchTasks: MutableStateFlow<Map<String, MutableList<PokeResearch>>> =
        MutableStateFlow(emptyMap())
    val pokemonToResearchTasks: StateFlow<Map<String, MutableList<PokeResearch>>> =
        _pokemonToResearchTasks.asStateFlow()

    //Whether completed Pokemon and completed tasks should be hidden from view
    //3-state filter for the main Pokemon list: show all, hide Rank10+, or hide only Perfect
    private val _hideFilter: MutableStateFlow<HideFilter> = MutableStateFlow(HideFilter.SHOW_ALL)
    val hideFilter: StateFlow<HideFilter> = _hideFilter.asStateFlow()

    //Which Hisui region the Pokemon list is currently filtered to, null means no filter (all regions)
    private val _selectedArea: MutableStateFlow<HisuiArea?> = MutableStateFlow(null)
    val selectedArea: StateFlow<HisuiArea?> = _selectedArea.asStateFlow()

    private var language = SupportedLanguage.ENGLISH

    //Calculates research progress for each Pokemon and builds map of Pokemon name to their research
    //tasks.
    fun calcProgress() {
        var idx = 0
        var points = 0
        val progList: MutableList<ResearchProgress> = mutableListOf()
        val mapPokemonToResearchTasks: MutableMap<String, MutableList<PokeResearch>> =
            mutableMapOf()
        val nameToProgListIdx: MutableMap<String, Int> = mutableMapOf()
        val taskList = researchTasks.value
        for (task in taskList) {
            if (nameToProgListIdx.containsKey(task.name)) {
                val tempIdx = nameToProgListIdx.getValue(task.name)
                progList[tempIdx].goalsDone += task.goalProgress
                progList[tempIdx].goalsTotal += task.totalGoals
                progList[tempIdx].pointsDone += task.goalProgress * task.points
                progList[tempIdx].pointsTotal += task.totalGoals * task.points
                if (progList[tempIdx].pointsDone >= 100) {
                    progList[tempIdx].bonusEarned = 100
                } else {
                    progList[tempIdx].bonusEarned = 0
                }
            } else {
                val newProgress = ResearchProgress(
                    name = task.name,
                    goalsDone = task.goalProgress,
                    goalsTotal = task.totalGoals,
                    pointsDone = task.goalProgress * task.points,
                    //TODO - Adding bonus points here, make this a constant
                    pointsTotal = task.totalGoals * task.points + 100
                )
                if (newProgress.pointsDone >= 100) {
                    newProgress.bonusEarned = 100
                } else {
                    newProgress.bonusEarned = 0
                }
                progList.add(idx, newProgress)
                nameToProgListIdx[task.name] = idx
                idx += 1
            }
            if (mapPokemonToResearchTasks.contains(task.name)) {
                mapPokemonToResearchTasks.getValue(task.name).add(task)
            } else {
                mapPokemonToResearchTasks[task.name] = mutableListOf(task)
            }
        }
        for (item in progList) {
            points += item.pointsDone + item.bonusEarned
        }
        _researchProgress.value = progList
        _pokemonToResearchTasks.value = mapPokemonToResearchTasks
        _userPoints.value = points
    }

    //Sorts a Pokemon list according to the current sort mode. Shared by setSort and searchClear
    //so the sort logic (including the research-level tie-break) only lives in one place.
    private fun sortedPokedex(list: List<Pokemon>): List<Pokemon> {
        return when (_pokesort.value) {
            PokeSort.ALPHABETICAL -> list.sortedBy { translate(language, it.name) }
            PokeSort.NATIONAL -> list.sortedBy { it.natId }
            PokeSort.RESEARCH_LEVEL -> {
                //Most progress first (Perfect, then Rank10, then in-progress, then untouched),
                //tie-broken by Hisui dex order
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

    //Set and execute the sorting.
    fun setSort(sort: PokeSort) {
        _pokesort.value = sort
        _pokedex.value = sortedPokedex(pokedex.value)
    }

    //Search for given text in research task list. Can match on Pokemon name or description of a
    //task. Set Pokemon list to filtered list.
    fun searchPokedex(searchText: String) {
        searchClear()
        var idx = 0
        val matchingPokemon: MutableList<Pokemon> = mutableListOf()
        val nameToListIdx: MutableMap<String, Int> = mutableMapOf()
        val taskList = researchTasks.value
        val oldPokedex = pokedex.value
        var translatedName: String
        var translatedTask: String
        val findText = translate(language, searchText).lowercase()
        for (task in taskList) {
            translatedName = translate(language, task.name).lowercase()
            translatedTask = translate(language, task.task).lowercase()
            if (!nameToListIdx.containsKey(task.name) &&
                (translatedName.contains(findText) ||
                        translatedTask.contains(findText))
            ) {
                for (pokemon in oldPokedex) {
                    if (pokemon.name == task.name) {
                        matchingPokemon.add(idx, pokemon)
                        nameToListIdx[task.name] = idx
                        idx += 1
                        break
                    }
                }
            }
        }
        _pokedex.value = matchingPokemon
        _inSearchMode.value = false
        _searchedText.value = searchText
        setSort(pokesort.value)
    }

    //Reset the search variables
    fun searchClear() {
        _pokedex.value = sortedPokedex(Pokedex.pokedex)
        _searchedText.value = ""
        _inSearchMode.value = true
    }

    //Set the goal progress for a task based on what the user clicked. If they clicked the current
    //goal progress, reset progress to 0. Updating the task does not trigger recomposition, so
    //making a copy with changed values, deleting the original, and inserting copy instead.
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

    //Parses and applies a backup string, returning the number of tasks that were actually
    //updated. Throws IllegalArgumentException (with a user-facing message) if the string isn't a
    //valid backup produced by this app.
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
