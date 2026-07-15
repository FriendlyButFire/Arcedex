package jzam.arcedex.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import jzam.arcedex.R
import jzam.arcedex.models.PokeResearch
import jzam.arcedex.models.SupportedLanguage
import jzam.arcedex.utils.getMoveType
import jzam.arcedex.utils.getTypeColor
import jzam.arcedex.utils.translate

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

//Points icon - only shown for tasks that actually give double points (points == 20). A same-size
//Spacer keeps every row's icon column aligned regardless of whether this task has one.
@Composable
fun PointsIcon(points: Int) {
    if (points == 20) {
        PokemonImage(
            imgId = R.drawable.double_points, size = 22,
            desc = stringResource(id = R.string.points_icon_desc),
            color = MaterialTheme.colorScheme.primary, alpha = 1f
        )
    } else {
        Spacer(modifier = Modifier.size(22.dp))
    }
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

//Goal description that can clicked to modify goal progress for a research task.
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
