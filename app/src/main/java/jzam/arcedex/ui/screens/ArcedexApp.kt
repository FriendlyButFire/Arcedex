package jzam.arcedex.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.intl.LocaleList
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import jzam.arcedex.models.HideFilter
import jzam.arcedex.ui.components.ArcedexBottomBar
import jzam.arcedex.ui.components.ArcedexTopBar
import jzam.arcedex.ui.components.InitializationScreen
import jzam.arcedex.ui.components.Pokedex
import jzam.arcedex.utils.getSupportedLanguage
import jzam.arcedex.viewmodels.PokeResearchViewModel

//Top-level screen composable that passes state along to child composables
@Composable
fun ArcedexApp(pokeResearchViewModel: PokeResearchViewModel) {
    val researchTasks by pokeResearchViewModel.researchTasks.collectAsStateWithLifecycle()
    val displayedPokedex by pokeResearchViewModel.filteredPokedex.collectAsStateWithLifecycle()
    val researchProgress by pokeResearchViewModel.researchProgress.collectAsStateWithLifecycle()
    val pokeSort by pokeResearchViewModel.pokesort.collectAsStateWithLifecycle()
    val inSearchMode by pokeResearchViewModel.inSearchMode.collectAsStateWithLifecycle()
    val searchedText by pokeResearchViewModel.searchedText.collectAsStateWithLifecycle()
    val userPoints by pokeResearchViewModel.userPoints.collectAsStateWithLifecycle()
    val pokemonToResearchTasks by pokeResearchViewModel.pokemonToResearchTasks.collectAsStateWithLifecycle()
    val hideFilter by pokeResearchViewModel.hideFilter.collectAsStateWithLifecycle()
    val selectedArea by pokeResearchViewModel.selectedArea.collectAsStateWithLifecycle()
    val selectedCategory by pokeResearchViewModel.selectedCategory.collectAsStateWithLifecycle()
    val selectedCategoryType by pokeResearchViewModel.selectedCategoryType.collectAsStateWithLifecycle()
    val language = getSupportedLanguage(LocaleList.current)

    // Side-effect: update ViewModel's language only when locale changes
    LaunchedEffect(language) {
        pokeResearchViewModel.setLanguage(language)
    }

    // Recalculate progress only when research tasks change
    LaunchedEffect(researchTasks) {
        pokeResearchViewModel.calcProgress()
    }

    if (researchTasks.size < 242) {
        InitializationScreen()
    } else {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                ArcedexTopBar(
                    language = language,
                    userPoints = userPoints,
                    hideFilter = hideFilter,
                    selectedArea = selectedArea,
                    selectedCategory = selectedCategory,
                    selectedCategoryType = selectedCategoryType,
                    onSortChosen = pokeResearchViewModel::setSort,
                    onHideFilterCycled = pokeResearchViewModel::cycleHideFilter,
                    onAreaChosen = pokeResearchViewModel::setAreaFilter,
                    onCategoryChosen = pokeResearchViewModel::setCategoryFilter,
                    onCategoryTypeChosen = pokeResearchViewModel::setCategoryTypeFilter,
                    onExport = pokeResearchViewModel::exportProgressBackup,
                    onImport = pokeResearchViewModel::importProgressBackup
                )
            },
            bottomBar = {
                ArcedexBottomBar(
                    language = language,
                    inSearchMode = inSearchMode,
                    searchText = searchedText,
                    onSearch = pokeResearchViewModel::searchPokedex,
                    searchClear = pokeResearchViewModel::searchClear
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                Pokedex(
                    language = language,
                    pokedex = displayedPokedex,
                    progress = researchProgress,
                    pokeSort = pokeSort,
                    onGoalClick = pokeResearchViewModel::onGoalClick,
                    pokemonToResearchTasks = pokemonToResearchTasks,
                    onMoveClick = pokeResearchViewModel::searchPokedex,
                    hideFinishedTasks = hideFilter != HideFilter.SHOW_ALL
                )
            }
        }
    }
}
