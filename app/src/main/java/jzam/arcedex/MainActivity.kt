package jzam.arcedex

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import android.content.ClipData
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import jzam.arcedex.data.PokeAreaData
import jzam.arcedex.models.*
import jzam.arcedex.ui.theme.*
import jzam.arcedex.utils.*
import jzam.arcedex.viewmodels.PokeResearchViewModel
import jzam.arcedex.viewmodels.PokeResearchViewModelFactory
import kotlinx.coroutines.launch

/*
 * Arcedex by jzam (https://github.com/jzam)
 *
 * This is the main activity of the Arcedex app. It displays a sortable and searchable list of
 * Pokemon. Each Pokemon can be clicked to display the research tasks for that Pokemon. Research
 * task progress can be tracked by clicking the box that matches the new progression. To clear
 * progression of a task, click the box matching the current progression. This screen also displays
 * the user's research rank progress based on all tasks completed.
 */
class MainActivity : ComponentActivity() {

    private val pokeResearchViewModel: PokeResearchViewModel by viewModels {
        PokeResearchViewModelFactory((application as ArcedexApplication).pokeResearchRepository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ArcedexTheme {
                ArcedexApp(pokeResearchViewModel)
            }
        }
    }
}

//Top-level screen composable that keeps track of various state variables used across the screen
//and passes them along to the child composables that need them
@Composable
fun ArcedexApp(pokeResearchViewModel: PokeResearchViewModel) {
    val researchTasks by pokeResearchViewModel.researchTasks.collectAsStateWithLifecycle()
    val pokedex by pokeResearchViewModel.pokedex.collectAsStateWithLifecycle()
    val researchProgress by pokeResearchViewModel.researchProgress.collectAsStateWithLifecycle()
    val pokeSort by pokeResearchViewModel.pokesort.collectAsStateWithLifecycle()
    val inSearchMode by pokeResearchViewModel.inSearchMode.collectAsStateWithLifecycle()
    val searchedText by pokeResearchViewModel.searchedText.collectAsStateWithLifecycle()
    val userPoints by pokeResearchViewModel.userPoints.collectAsStateWithLifecycle()
    val pokemonToResearchTasks by pokeResearchViewModel.pokemonToResearchTasks.collectAsStateWithLifecycle()
    val hideFilter by pokeResearchViewModel.hideFilter.collectAsStateWithLifecycle()
    val selectedArea by pokeResearchViewModel.selectedArea.collectAsStateWithLifecycle()
    val language = getSupportedLanguage(LocaleList.current)

    pokeResearchViewModel.setLanguage(language)

    //Recalculate progress only when the underlying research tasks actually change (e.g. after
    //saving a goal), not on every unrelated recomposition (toggling a filter, changing sort, etc.)
    LaunchedEffect(researchTasks) {
        pokeResearchViewModel.calcProgress()
    }

    //Show the main app screen or the initialization screen if we are still waiting on the view
    //model to retrieve all tasks from the repository
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
                    onSortChosen = pokeResearchViewModel::setSort,
                    onHideFilterCycled = pokeResearchViewModel::cycleHideFilter,
                    onAreaChosen = pokeResearchViewModel::setAreaFilter,
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
                val displayedPokedex = pokedex
                    .filter { pokemon ->
                        val prog = researchProgress.find { it.name == pokemon.name }
                        when (hideFilter) {
                            HideFilter.SHOW_ALL -> true
                            //Rank10 = 100+ raw points, same threshold ProgressPokeballImage uses
                            //to decide whether to show a pokeball at all
                            HideFilter.HIDE_RANK10 -> prog == null || prog.pointsDone < 100
                            //Perfect = every single research task completed (masterball threshold)
                            HideFilter.HIDE_PERFECT -> prog == null || (prog.pointsDone + prog.bonusEarned) < prog.pointsTotal
                        }
                    }
                    .filter { pokemon ->
                        selectedArea == null || PokeAreaData.getAreas(pokemon.hisuiId).contains(selectedArea)
                    }
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

//App's top bar - Shows app name, research rank progress, and filter/sort chips
@Composable
fun ArcedexTopBar(
    language: SupportedLanguage, userPoints: Int, hideFilter: HideFilter, selectedArea: HisuiArea?,
    onSortChosen: (PokeSort) -> Unit, onHideFilterCycled: () -> Unit,
    onAreaChosen: (HisuiArea?) -> Unit,
    onExport: () -> String, onImport: suspend (String) -> Int
) {
    Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 4.dp) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.app_name),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                ResearchRankInfo(language, userPoints)
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState())
            ) {
                HideFilterButton(hideFilter, onHideFilterCycled)
                SortButton(onSortChosen)
                RegionButton(selectedArea, onAreaChosen)
                BackupButton(onExport, onImport)
            }
        }
    }
}

//3-state chip cycling SHOW_ALL -> HIDE_RANK10 -> HIDE_PERFECT -> SHOW_ALL on tap. Label always
//names the CURRENT state (what's hidden right now), not the action the next tap will take -
//clearer for a 3-state cycle than the old 2-state "next action" wording.
@Composable
fun HideFilterButton(hideFilter: HideFilter, onCycle: () -> Unit) {
    val label = when (hideFilter) {
        HideFilter.SHOW_ALL -> stringResource(R.string.filter_show_all_label)
        HideFilter.HIDE_RANK10 -> stringResource(R.string.filter_hide_rank10_label)
        HideFilter.HIDE_PERFECT -> stringResource(R.string.filter_hide_perfect_label)
    }
    val selected = hideFilter != HideFilter.SHOW_ALL
    //Different tone per hidden state so it's visually obvious which of the two you're in, not
    //just that "something" is filtered
    val selectedColor = when (hideFilter) {
        HideFilter.HIDE_PERFECT -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.primary
    }

    FilterChip(
        selected = selected,
        onClick = onCycle,
        label = { Text(label, style = MaterialTheme.typography.labelLarge) },
        colors = FilterChipDefaults.filterChipColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            labelColor = MaterialTheme.colorScheme.onSurface,
            selectedContainerColor = selectedColor,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
        ),
        border = if (selected) null else FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = false,
            borderColor = MaterialTheme.colorScheme.outline
        )
    )
}

//Display current research rank, progress bar to next rank, and points to next rank
@Composable
fun ResearchRankInfo(language: SupportedLanguage, userPoints: Int) {
    val researchRank = getResearchRank(language, userPoints)
    val pointsToNext = getPointsToNextRankText(language, userPoints)
    val barProgress = getRankProgress(userPoints.toFloat())
    val totalPointsPossible = getTotalPointsPossible()

    Column(horizontalAlignment = Alignment.End) {
        Text(researchRank, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        LinearProgressIndicator(
            progress = { barProgress },
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.outline,
            modifier = Modifier
                .width(120.dp)
                .padding(vertical = 6.dp)
                .clip(RoundedCornerShape(50))
        )
        Text(pointsToNext, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            "$userPoints / $totalPointsPossible",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

//Chip to open the sort menu - an assist chip since it triggers an action/menu rather than
//toggling a persistent state.
@Composable
fun SortButton(onSortChosen: (PokeSort) -> Unit) {
    var sortExpanded by remember { mutableStateOf(false) }

    Box {
        AssistChip(
            onClick = { sortExpanded = true },
            label = { Text(stringResource(R.string.sort_label), style = MaterialTheme.typography.labelLarge) },
            colors = AssistChipDefaults.assistChipColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                labelColor = MaterialTheme.colorScheme.onSurface
            ),
            border = AssistChipDefaults.assistChipBorder(
                enabled = true,
                borderColor = MaterialTheme.colorScheme.outline
            )
        )
        DropdownMenu(
            expanded = sortExpanded,
            onDismissRequest = { sortExpanded = false }
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.hisui_sort_label)) },
                onClick = {
                    onSortChosen(PokeSort.HISUI)
                    sortExpanded = false
                })
            DropdownMenuItem(
                text = { Text(stringResource(R.string.alpha_sort_label)) },
                onClick = {
                    onSortChosen(PokeSort.ALPHABETICAL)
                    sortExpanded = false
                })
            DropdownMenuItem(
                text = { Text(stringResource(R.string.national_sort_label)) },
                onClick = {
                    onSortChosen(PokeSort.NATIONAL)
                    sortExpanded = false
                })
            DropdownMenuItem(
                text = { Text(stringResource(R.string.research_level_sort_label)) },
                onClick = {
                    onSortChosen(PokeSort.RESEARCH_LEVEL)
                    sortExpanded = false
                })
        }
    }
}

//Chip to open the region filter menu - filters the Pokemon list down to only those found in the
//selected Hisui area. Selecting the currently-selected area again, or "All Regions", clears it.
@Composable
fun RegionButton(selectedArea: HisuiArea?, onAreaChosen: (HisuiArea?) -> Unit) {
    var regionExpanded by remember { mutableStateOf(false) }

    Box {
        AssistChip(
            onClick = { regionExpanded = true },
            label = {
                Text(
                    selectedArea?.let { areaDisplayName(it) } ?: stringResource(R.string.region_label),
                    style = MaterialTheme.typography.labelLarge
                )
            },
            colors = AssistChipDefaults.assistChipColors(
                containerColor = if (selectedArea != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                labelColor = if (selectedArea != null) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
            ),
            border = if (selectedArea != null) null else AssistChipDefaults.assistChipBorder(
                enabled = true,
                borderColor = MaterialTheme.colorScheme.outline
            )
        )
        DropdownMenu(
            expanded = regionExpanded,
            onDismissRequest = { regionExpanded = false }
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.all_regions_label)) },
                onClick = {
                    onAreaChosen(null)
                    regionExpanded = false
                })
            for (area in HisuiArea.values()) {
                DropdownMenuItem(
                    text = { Text(areaDisplayName(area)) },
                    onClick = {
                        onAreaChosen(area)
                        regionExpanded = false
                    })
            }
        }
    }
}

//Localized display name for a Hisui region
@Composable
fun areaDisplayName(area: HisuiArea): String {
    return stringResource(
        when (area) {
            HisuiArea.OBSIDIAN_FIELDLANDS -> R.string.obsidian_fieldlands_label
            HisuiArea.CRIMSON_MIRELANDS -> R.string.crimson_mirelands_label
            HisuiArea.COBALT_COASTLANDS -> R.string.cobalt_coastlands_label
            HisuiArea.CORONET_HIGHLANDS -> R.string.coronet_highlands_label
            HisuiArea.ALABASTER_ICELANDS -> R.string.alabaster_icelands_label
        }
    )
}

//Chip that opens the backup/restore dialog
@Composable
fun BackupButton(onExport: () -> String, onImport: suspend (String) -> Int) {
    var dialogOpen by remember { mutableStateOf(false) }

    AssistChip(
        onClick = { dialogOpen = true },
        label = { Text(stringResource(R.string.backup_label), style = MaterialTheme.typography.labelLarge) },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            labelColor = MaterialTheme.colorScheme.onSurface
        ),
        border = AssistChipDefaults.assistChipBorder(
            enabled = true,
            borderColor = MaterialTheme.colorScheme.outline
        )
    )
    if (dialogOpen) {
        BackupDialog(onExport = onExport, onImport = onImport, onDismiss = { dialogOpen = false })
    }
}

//Dialog with two sections: export shows a copy-pasteable backup string for the current progress,
//import lets you paste one back in to restore/merge progress. The backup only contains
//goalProgress per (Pokemon, task) - not the static task data itself, which the app already has.
@Composable
fun BackupDialog(onExport: () -> String, onImport: suspend (String) -> Int, onDismiss: () -> Unit) {
    val clipboard = LocalClipboard.current
    val coroutineScope = rememberCoroutineScope()
    var pasteText by remember { mutableStateOf("") }
    var restoredCount by remember { mutableStateOf<Int?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val backupString = remember { onExport() }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    stringResource(R.string.backup_export_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    stringResource(R.string.backup_export_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        backupString,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .padding(12.dp)
                            .heightIn(max = 100.dp)
                            .verticalScroll(rememberScrollState())
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        coroutineScope.launch {
                            clipboard.setClipEntry(
                                ClipEntry(ClipData.newPlainText("Arcedex backup", backupString))
                            )
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(stringResource(R.string.backup_copy_label))
                }

                Spacer(modifier = Modifier.height(20.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp)
                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    stringResource(R.string.backup_import_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    stringResource(R.string.backup_import_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = pasteText,
                    onValueChange = {
                        pasteText = it
                        restoredCount = null
                        errorMessage = null
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        cursorColor = MaterialTheme.colorScheme.primary,
                        focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                        unfocusedIndicatorColor = MaterialTheme.colorScheme.outline
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.backup_paste_label)) },
                    maxLines = 3
                )
                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else if (restoredCount != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        stringResource(R.string.backup_restored_message, restoredCount!!),
                        color = MaterialTheme.colorScheme.tertiary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.align(Alignment.End)
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.backup_close_label))
                    }
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                try {
                                    val count = onImport(pasteText)
                                    errorMessage = null
                                    restoredCount = count
                                } catch (e: IllegalArgumentException) {
                                    restoredCount = null
                                    errorMessage = e.message
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text(stringResource(R.string.backup_restore_label))
                    }
                }
            }
        }
    }
}

//Pokedex screen - list Pokemon and click a Pokemon to expand and see their research tasks
@Composable
fun Pokedex(
    language: SupportedLanguage,
    pokedex: List<Pokemon>, progress: List<ResearchProgress>,
    pokeSort: PokeSort, onGoalClick: (PokeResearch, Int) -> Unit,
    pokemonToResearchTasks: Map<String, MutableList<PokeResearch>>?,
    onMoveClick: (String) -> Unit,
    hideFinishedTasks: Boolean
) {
    if (pokedex.isNotEmpty()) {
        LazyColumn(
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(pokedex) {
                PokedexPokemon(
                    language = language,
                    pokemon = it,
                    tasks = pokemonToResearchTasks?.get(it.name),
                    progress = progress,
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

//Display for a Pokemon from the Pokedex screen
@Composable
fun PokedexPokemon(
    language: SupportedLanguage,
    pokemon: Pokemon,
    tasks: MutableList<PokeResearch>?,
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

    //When the filter is on, drop tasks whose goal has already been fully reached
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
                onClick = { isExpanded = !isExpanded })
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

//Show a research task with points icon, description, and clickable progress goals
@Composable
fun TaskRow(
    language: SupportedLanguage,
    pokemonTask: PokeResearch,
    onGoalClick: (PokeResearch, Int) -> Unit,
    onMoveClick: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        PointsIcon(points = pokemonTask.points)
        TaskText(language, pokemonTask.task, onMoveClick)
        GoalText(language = language, goal = pokemonTask.goal1, goalNum = 1, pokemonTask, onGoalClick)
        GoalText(language = language, goal = pokemonTask.goal2, goalNum = 2, pokemonTask, onGoalClick)
        GoalText(language = language, goal = pokemonTask.goal3, goalNum = 3, pokemonTask, onGoalClick)
        GoalText(language = language, goal = pokemonTask.goal4, goalNum = 4, pokemonTask, onGoalClick)
        GoalText(language = language, goal = pokemonTask.goal5, goalNum = 5, pokemonTask, onGoalClick)
    }
}

//Points icon. 20 = double points, 10 = standard points
@Composable
fun PointsIcon(points: Int) {
    val color = if (points == 20) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    PokemonImage(
        imgId = R.drawable.double_points, size = 22,
        desc = stringResource(id = R.string.points_icon_desc), color = color, alpha = 1f
    )
}

//Task description that will show move type if it's for a Pokemon move, using an assist chip
//since tapping it triggers a search action.
@Composable
fun RowScope.TaskText(language: SupportedLanguage, task: String, onMoveClick: (String) -> Unit) {
    Column(modifier = Modifier.weight(1f)) {
        Text(
            translate(language, task),
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        val type = getMoveType(task)
        val typeText = translate(language, "-type")
        val translatedType = translate(language, type)
        if (type.isNotBlank()) {
            val bgColor = getTypeColor(type)
            AssistChip(
                onClick = { onMoveClick(("$translatedType$typeText")) },
                label = { Text(translate(language, type), style = MaterialTheme.typography.labelMedium) },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = bgColor,
                    labelColor = Color.White
                ),
                border = null,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

//Goal description that can clicked to modify goal progress for a research task. Kept as a
//custom compact Surface rather than a FilterChip - M3's chip padding/min-touch-target is too
//large to fit 5 of these in a row alongside the task text.
@Composable
fun GoalText(
    language: SupportedLanguage,
    goal: String, goalNum: Int, pokemonTask: PokeResearch,
    onGoalClick: (PokeResearch, Int) -> Unit
) {
    if (goal.isNotBlank()) {
        val reached = pokemonTask.goalProgress >= goalNum
        Surface(
            shape = CircleShape,
            color = if (reached) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.surfaceVariant,
            border = if (reached) null else androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outline
            ),
            modifier = Modifier
                .padding(horizontal = 2.dp)
                .clickable { onGoalClick(pokemonTask, goalNum) }
        ) {
            Text(
                translate(lang = language, text = goal),
                color = if (reached) MaterialTheme.colorScheme.onTertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
            )
        }
    }
}

//Screen that displays when search results in an empty list
@Composable
fun ShowEmptySearch() {
    val emptyPokemon = jzam.arcedex.data.Pokedex.emptyDex
    val emptyResearch =
        listOf(
            ResearchProgress(
                name = emptyPokemon.name,
                goalsTotal = 0,
                pointsTotal = 1,
                pointsDone = 1
            )
        )
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
                progress = emptyResearch,
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

//App's bottom bar - used for searching the Pokemon list
@Composable
fun ArcedexBottomBar(
    language: SupportedLanguage,
    inSearchMode: Boolean, searchText: String, onSearch: (String) -> Unit,
    searchClear: () -> Unit
) {
    Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 8.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!inSearchMode) {
                Text(
                    formatSearchedText(language, searchText),
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
                OutlinedButton(
                    onClick = { searchClear() },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Text(stringResource(R.string.clear_label), style = MaterialTheme.typography.labelLarge)
                }
            } else {
                SearchInputText(onSearch)
            }
        }
    }
}

//Search bar and button
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun RowScope.SearchInputText(
    onDone: (String) -> Unit = {}
) {
    var text by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current

    TextField(
        value = text,
        onValueChange = { text = it },
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
            focusedLabelColor = MaterialTheme.colorScheme.primary,
            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            cursorColor = MaterialTheme.colorScheme.primary,
            focusedIndicatorColor = MaterialTheme.colorScheme.primary,
            unfocusedIndicatorColor = MaterialTheme.colorScheme.outline
        ),
        maxLines = 1,
        keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = {
            onDone(text)
            keyboardController?.hide()
        }),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .weight(1f)
            .padding(end = 8.dp),
        label = { Text(stringResource(R.string.search_label)) }
    )
    Button(
        onClick = { onDone(text) },
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        )
    ) {
        Text(stringResource(R.string.done_label), style = MaterialTheme.typography.labelLarge)
    }
}

//Temporary screen when ViewModel data is not ready to display yet. Probably replace with splash
//screen when I get a chance.
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
