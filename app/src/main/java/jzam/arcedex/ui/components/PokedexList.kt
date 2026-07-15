package jzam.arcedex.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import jzam.arcedex.R
import jzam.arcedex.data.Pokedex as PokedexData
import jzam.arcedex.models.PokeResearch
import jzam.arcedex.models.PokeSort
import jzam.arcedex.models.Pokemon
import jzam.arcedex.models.ResearchProgress
import jzam.arcedex.models.SupportedLanguage

//Pokedex screen - list Pokemon and click a Pokemon to expand and see their research tasks
@Composable
fun Pokedex(
    language: SupportedLanguage,
    pokedex: List<Pokemon>, progress: List<ResearchProgress>,
    pokeSort: PokeSort, onGoalClick: (PokeResearch, Int) -> Unit,
    pokemonToResearchTasks: Map<String, List<PokeResearch>>?,
    onMoveClick: (String) -> Unit,
    hideFinishedTasks: Boolean
) {
    val progressMap = remember(progress) { progress.associateBy { it.name } }

    if (pokedex.isNotEmpty()) {
        LazyColumn(
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(pokedex, key = { it.name }) {
                PokedexPokemon(
                    language = language,
                    pokemon = it,
                    tasks = pokemonToResearchTasks?.get(it.name),
                    progressMap = progressMap,
                    pokeSort = pokeSort,
                    onGoalClick = onGoalClick,
                    onMoveClick = onMoveClick,
                    hideFinishedTasks = hideFinishedTasks
                )
            }
        }
    } else {
        ShowEmptySearch()
    }
}

//Screen that displays when search results in an empty list
@Composable
fun ShowEmptySearch() {
    val emptyPokemon = PokedexData.emptyDex
    val emptyResearchMap = remember {
        mapOf(
            emptyPokemon.name to ResearchProgress(
                name = emptyPokemon.name,
                goalsTotal = 0,
                pointsTotal = 1,
                pointsDone = 1
            )
        )
    }
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            PokemonHeaderRow(
                language = SupportedLanguage.ENGLISH,
                pokemon = emptyPokemon,
                progressMap = emptyResearchMap,
                pokeSort = PokeSort.HISUI,
                isExpanded = false,
                onClick = {})
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.search_fail_message),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

//Initialization screen when data is retrieving
@Composable
fun InitializationScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        PokemonImage(
            imgId = R.drawable.sprite79, size = 180,
            desc = stringResource(id = R.string.waiting_pic_desc),
            color = Color.Unspecified,
            alpha = 0.9f
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            stringResource(R.string.init_message1),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            stringResource(R.string.init_message2),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
