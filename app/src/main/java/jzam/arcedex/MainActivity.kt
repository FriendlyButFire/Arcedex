package jzam.arcedex

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jzam.arcedex.models.*
import jzam.arcedex.ui.theme.*
import jzam.arcedex.utils.*
import jzam.arcedex.viewmodels.PokeResearchViewModel
import jzam.arcedex.viewmodels.PokeResearchViewModelFactory

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
    val researchTasks by pokeResearchViewModel.researchTasks.observeAsState()
    val pokedex by pokeResearchViewModel.pokedex.observeAsState()
    val researchProgress by pokeResearchViewModel.researchProgress.observeAsState()
    val pokeSort by pokeResearchViewModel.pokesort.observeAsState()
    val inSearchMode by pokeResearchViewModel.inSearchMode.observeAsState()
    val searchedText by pokeResearchViewModel.searchedText.observeAsState()
    val userPoints by pokeResearchViewModel.userPoints.observeAsState()
    val pokemonToResearchTasks by pokeResearchViewModel.pokemonToResearchTasks.observeAsState()
    val hideCompleted by pokeResearchViewModel.hideCompleted.observeAsState(false)
    val language = getSupportedLanguage(LocaleList.current)

    pokeResearchViewModel.setLanguage(language)

    //Recalculate progress on recomposition, need to make sure research tasks has been fetched
    //before running or this will fail
    if (researchTasks != null) {
        pokeResearchViewModel.calcProgress()
    }

    //Show the main app screen or the initialization screen if we are still waiting on the view
    //model to retrieve all tasks from the repository
    if (researchTasks == null || researchTasks!!.size < 242) {
        InitializationScreen()
    } else {
        Scaffold(
            backgroundColor = Background,
            topBar = {
                ArcedexTopBar(
                    language = language,
                    userPoints = userPoints!!,
                    hideCompleted = hideCompleted,
                    onSortChosen = pokeResearchViewModel::setSort,
                    onHideCompletedToggled = pokeResearchViewModel::toggleHideCompleted
                )
            },
            bottomBar = {
                ArcedexBottomBar(
                    language = language,
                    inSearchMode = inSearchMode!!,
                    searchText = searchedText!!,
                    onSearch = pokeResearchViewModel::searchPokedex,
                    searchClear = pokeResearchViewModel::searchClear
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .padding(innerPadding)
                    .background(Background)
            ) {
                val displayedPokedex = if (hideCompleted) {
                    pokedex!!.filter { pokemon ->
                        val prog = researchProgress!!.find { it.name == pokemon.name }
                        //Keep Pokemon with no progress entry (not yet started) or not fully done
                        prog == null || (prog.pointsDone + prog.bonusEarned) < prog.pointsTotal
                    }
                } else {
                    pokedex!!
                }
                Pokedex(
                    language = language,
                    pokedex = displayedPokedex,
                    progress = researchProgress!!,
                    pokeSort = pokeSort!!,
                    onGoalClick = pokeResearchViewModel::onGoalClick,
                    pokemonToResearchTasks = pokemonToResearchTasks,
                    onMoveClick = pokeResearchViewModel::searchPokedex,
                    hideCompleted = hideCompleted
                )
            }
        }
    }
}

//App's top bar - Shows app name, research rank progress, and Sort/Hide-completed buttons
@Composable
fun ArcedexTopBar(
    language: SupportedLanguage, userPoints: Int, hideCompleted: Boolean,
    onSortChosen: (PokeSort) -> Unit, onHideCompletedToggled: () -> Unit
) {
    Surface(color = Surface, elevation = 4.dp) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.app_name),
                    style = Typography.h6,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f)
                )
                ResearchRankInfo(language, userPoints)
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row {
                HideCompletedButton(hideCompleted, onHideCompletedToggled)
                Spacer(modifier = Modifier.width(8.dp))
                SortButton(onSortChosen)
            }
        }
    }
}

//Toggle button to show/hide Pokemon and tasks that are already fully completed
@Composable
fun HideCompletedButton(hideCompleted: Boolean, onToggle: () -> Unit) {
    PillButton(
        text = stringResource(if (hideCompleted) R.string.show_completed_label else R.string.hide_completed_label),
        selected = hideCompleted,
        onClick = onToggle
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
        Text(researchRank, style = Typography.button, color = AccentRed)
        LinearProgressIndicator(
            progress = barProgress,
            color = AccentRed,
            backgroundColor = SurfaceBorder,
            modifier = Modifier
                .width(120.dp)
                .padding(vertical = 6.dp)
                .clip(RoundedCornerShape(50))
        )
        Text(pointsToNext, style = Typography.caption, color = TextSecondary)
        Text("$userPoints / $totalPointsPossible", style = Typography.caption, color = TextSecondary)
    }
}

//Button to set how the Pokemon list is sorted
@Composable
fun SortButton(onSortChosen: (PokeSort) -> Unit) {
    var sortExpanded by remember { mutableStateOf(false) }

    Box {
        PillButton(
            text = stringResource(R.string.sort_label),
            selected = false,
            onClick = { sortExpanded = true }
        )
        DropdownMenu(
            expanded = sortExpanded,
            onDismissRequest = { sortExpanded = false },
            modifier = Modifier.background(SurfaceElevated)
        ) {
            DropdownMenuItem(onClick = {
                onSortChosen(PokeSort.HISUI)
                sortExpanded = false
            }) {
                Text(stringResource(R.string.hisui_sort_label), color = TextPrimary)
            }
            DropdownMenuItem(onClick = {
                onSortChosen(PokeSort.ALPHABETICAL)
                sortExpanded = false
            }) {
                Text(stringResource(R.string.alpha_sort_label), color = TextPrimary)
            }
            DropdownMenuItem(onClick = {
                onSortChosen(PokeSort.NATIONAL)
                sortExpanded = false
            }) {
                Text(stringResource(R.string.national_sort_label), color = TextPrimary)
            }
        }
    }
}

//Small reusable pill-shaped button used across the top bar and bottom bar
@Composable
fun PillButton(text: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        shape = PillShape,
        color = if (selected) AccentRed else SurfaceElevated,
        border = if (selected) null else BorderStroke(1.dp, SurfaceBorder),
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text,
            style = Typography.button,
            color = if (selected) Background else TextPrimary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
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
    hideCompleted: Boolean
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
                    hideCompleted = hideCompleted
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
    hideCompleted: Boolean,
) {

    var isExpanded by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }

    if (pokemon.name != name) {
        isExpanded = false
        name = pokemon.name
    }

    //When the filter is on, drop tasks whose goal has already been fully reached
    val visibleTasks = if (hideCompleted) {
        tasks?.filter { it.goalProgress < it.totalGoals }
    } else {
        tasks
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        backgroundColor = if (isExpanded) SurfaceElevated else Surface,
        elevation = if (isExpanded) 6.dp else 1.dp,
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
                Divider(color = SurfaceBorder, thickness = 1.dp)
                if (visibleTasks != null) {
                    if (visibleTasks.isEmpty()) {
                        Text(
                            stringResource(R.string.search_fail_message),
                            color = TextSecondary,
                            style = Typography.body2,
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
                color = TextSecondary,
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
            .background(SurfaceElevated),
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
                Text(idText, color = TextSecondary, style = Typography.body2)
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                translate(language, pokemon.name),
                color = TextPrimary,
                style = Typography.subtitle1
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = fraction,
            color = if (complete) AccentGreen else AccentRed,
            backgroundColor = SurfaceBorder,
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
            color = TextSecondary,
            style = Typography.caption
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
            color = Background,
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
    val color = if (points == 20) AccentRed else TextSecondary
    PokemonImage(
        imgId = R.drawable.double_points, size = 22,
        desc = stringResource(id = R.string.points_icon_desc), color = color, alpha = 1f
    )
}

//Task description that will show move type if it's for a Pokemon move
@Composable
fun RowScope.TaskText(language: SupportedLanguage, task: String, onMoveClick: (String) -> Unit) {
    Column(modifier = Modifier.weight(1f)) {
        Text(
            translate(language, task),
            color = TextPrimary,
            style = Typography.body2,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        val type = getMoveType(task)
        val typeText = translate(language, "-type")
        val translatedType = translate(language, type)
        if (type.isNotBlank()) {
            val bgColor = getTypeColor(type)
            Surface(
                shape = PillShape,
                color = bgColor,
                modifier = Modifier
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .clickable { onMoveClick(("$translatedType$typeText")) }
            ) {
                Text(
                    text = translate(language, type),
                    color = Color.White,
                    style = Typography.caption,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
    }
}

//Goal description that can clicked to modify goal progress for a research task
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
            color = if (reached) AccentGreen else SurfaceElevated,
            border = if (reached) null else BorderStroke(1.dp, SurfaceBorder),
            modifier = Modifier
                .padding(horizontal = 2.dp)
                .clickable { onGoalClick(pokemonTask, goalNum) }
        ) {
            Text(
                translate(lang = language, text = goal),
                color = if (reached) Background else TextSecondary,
                style = Typography.caption,
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
            .background(Background)
            .padding(16.dp)
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            backgroundColor = Surface,
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
        Text(text = stringResource(R.string.search_fail_message), color = TextSecondary, style = Typography.body1)
    }
}

//App's bottom bar - used for searching the Pokemon list
@Composable
fun ArcedexBottomBar(
    language: SupportedLanguage,
    inSearchMode: Boolean, searchText: String, onSearch: (String) -> Unit,
    searchClear: () -> Unit
) {
    Surface(color = Surface, elevation = 8.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!inSearchMode) {
                Text(
                    formatSearchedText(language, searchText),
                    color = TextPrimary,
                    style = Typography.body2,
                    modifier = Modifier.weight(1f)
                )
                PillButton(
                    text = stringResource(R.string.clear_label),
                    selected = false,
                    onClick = { searchClear() }
                )
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
        colors = TextFieldDefaults.textFieldColors(
            backgroundColor = SurfaceElevated,
            textColor = TextPrimary,
            placeholderColor = TextSecondary,
            focusedLabelColor = AccentRed,
            unfocusedLabelColor = TextSecondary,
            cursorColor = AccentRed,
            focusedIndicatorColor = AccentRed,
            unfocusedIndicatorColor = SurfaceBorder
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
    PillButton(
        text = stringResource(R.string.done_label),
        selected = true,
        onClick = { onDone(text) }
    )
}

//Temporary screen when ViewModel data is not ready to display yet. Probably replace with splash
//screen when I get a chance.
@Composable
fun InitializationScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        PokemonImage(
            imgId = R.drawable.sprite79, size = 180,
            desc = stringResource(id = R.string.waiting_pic_desc),
            color = Background,
            alpha = 0.9f
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            stringResource(R.string.init_message1),
            style = Typography.h6,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            stringResource(R.string.init_message2),
            style = Typography.body1,
            color = TextSecondary
        )
    }
}
