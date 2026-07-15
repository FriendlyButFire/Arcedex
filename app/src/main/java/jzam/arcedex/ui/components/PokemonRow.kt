package jzam.arcedex.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jzam.arcedex.R
import jzam.arcedex.models.PokeResearch
import jzam.arcedex.models.PokeSort
import jzam.arcedex.models.Pokemon
import jzam.arcedex.models.ResearchProgress
import jzam.arcedex.models.SupportedLanguage
import jzam.arcedex.utils.formatPokemonId
import jzam.arcedex.utils.formatPokemonResearchInfo
import jzam.arcedex.utils.translate

//Display for a Pokemon card with research tasks
@Composable
fun PokedexPokemon(
    language: SupportedLanguage,
    pokemon: Pokemon,
    tasks: List<PokeResearch>?,
    progress: List<ResearchProgress>,
    pokeSort: PokeSort,
    onGoalClick: (PokeResearch, Int) -> Unit,
    onMoveClick: (String) -> Unit,
    hideFinishedTasks: Boolean,
) {
    var isExpanded by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }

    if (pokemon.name != name) {
        isExpanded = false
        name = pokemon.name
    }

    val visibleTasks = if (hideFinishedTasks) {
        tasks?.filter { it.goalProgress < it.totalGoals }
    } else {
        tasks
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isExpanded) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isExpanded) 6.dp else 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
    ) {
        Column {
            PokemonHeaderRow(
                language = language,
                pokemon = pokemon,
                progress = progress,
                pokeSort = pokeSort,
                isExpanded = isExpanded,
                onClick = { isExpanded = !isExpanded }
            )
            if (isExpanded) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp)
                if (visibleTasks != null) {
                    if (visibleTasks.isEmpty()) {
                        Text(
                            stringResource(R.string.search_fail_message),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(16.dp)
                        )
                    } else {
                        Column(modifier = Modifier.padding(vertical = 4.dp)) {
                            for (item in visibleTasks) {
                                TaskRow(language, item, onGoalClick, onMoveClick)
                            }
                        }
                    }
                }
            }
        }
    }
}

//Header row that displays info about Pokemon and a summary of its research progress.
@Composable
fun PokemonHeaderRow(
    language: SupportedLanguage,
    pokemon: Pokemon, progress: List<ResearchProgress>, pokeSort: PokeSort, isExpanded: Boolean,
    onClick: () -> Unit
) {
    val pokeProgress = progress.find { it.name == pokemon.name }

    if (pokeProgress != null) {
        val started = pokeProgress.pointsDone > 0
        val avatarAlpha = if (started) 1f else 0.35f
        val chevronRotation by animateFloatAsState(if (isExpanded) 180f else 0f)

        Row(
            modifier = Modifier
                .clickable { onClick() }
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PokemonAvatar(imgId = pokemon.imgId, alpha = avatarAlpha)
            Spacer(modifier = Modifier.width(12.dp))
            PokemonProgress(
                language = language,
                modifier = Modifier.weight(1f),
                pokemon = pokemon,
                pokeProgress = pokeProgress,
                pokeSort = pokeSort
            )
            ProgressPokeballImage(pokeProgress)
            Text(
                "\u25BE",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 18.sp,
                modifier = Modifier
                    .padding(start = 8.dp)
                    .rotate(chevronRotation)
            )
        }
    }
}

//Circular Pokemon avatar with a tinted ring background
@Composable
fun PokemonAvatar(imgId: Int, alpha: Float) {
    Box(
        modifier = Modifier
            .size(52.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = imgId),
            contentDescription = stringResource(id = R.string.pokemon_pic_desc),
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .alpha(alpha)
        )
    }
}

//Generic image format used for icons elsewhere on screen (points icon, pokeball, etc.)
@Composable
fun PokemonImage(imgId: Int, size: Int, desc: String, color: Color, alpha: Float) {
    Image(
        painter = painterResource(id = imgId),
        contentDescription = desc,
        modifier = Modifier
            .size(size.dp)
            .alpha(alpha)
    )
}

//Display Pokemon's basic info, a slim per-Pokemon progress bar, and research progress summary
@Composable
fun PokemonProgress(
    language: SupportedLanguage,
    modifier: Modifier, pokemon: Pokemon, pokeProgress: ResearchProgress,
    pokeSort: PokeSort
) {
    val fraction = if (pokeProgress.goalsTotal > 0) {
        (pokeProgress.goalsDone.toFloat() / pokeProgress.goalsTotal.toFloat()).coerceIn(0f, 1f)
    } else 0f
    val complete = pokeProgress.pointsDone + pokeProgress.bonusEarned == pokeProgress.pointsTotal

    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val idText = when (pokeSort) {
                PokeSort.NATIONAL -> formatPokemonId(pokemon.natId)
                PokeSort.HISUI -> formatPokemonId(pokemon.hisuiId)
                else -> ""
            }
            if (idText.isNotBlank()) {
                Text(idText, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                translate(language, pokemon.name),
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleMedium
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { fraction },
            color = if (complete) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.outline,
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .height(5.dp)
                .clip(RoundedCornerShape(50))
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            formatPokemonResearchInfo(
                lang = language,
                goalsDone = pokeProgress.goalsDone,
                goalsTotal = pokeProgress.goalsTotal,
                pointsDone = pokeProgress.pointsDone + pokeProgress.bonusEarned,
                pointsTotal = pokeProgress.pointsTotal
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelMedium
        )
    }
}

//Show Pokeball image if enough research points have been earned for a Pokemon.
@Composable
fun ProgressPokeballImage(pokeProgress: ResearchProgress) {
    if (pokeProgress.pointsDone >= 100) {
        val pokeballImg = if (pokeProgress.pointsDone + pokeProgress.bonusEarned == pokeProgress.pointsTotal) {
            R.drawable.masterball
        } else {
            R.drawable.pokeball
        }
        PokemonImage(
            imgId = pokeballImg, size = 32,
            desc = stringResource(R.string.pokeball_pic_desc),
            color = Color.Unspecified,
            alpha = 1f
        )
    }
}
