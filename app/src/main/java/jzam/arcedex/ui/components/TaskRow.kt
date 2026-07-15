package jzam.arcedex.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        PointsIcon(points = pokemonTask.points)
        TaskText(language, pokemonTask.task, onMoveClick)
        Row(
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GoalText(language = language, goal = pokemonTask.goal1, goalNum = 1, pokemonTask, onGoalClick)
            GoalText(language = language, goal = pokemonTask.goal2, goalNum = 2, pokemonTask, onGoalClick)
            GoalText(language = language, goal = pokemonTask.goal3, goalNum = 3, pokemonTask, onGoalClick)
            GoalText(language = language, goal = pokemonTask.goal4, goalNum = 4, pokemonTask, onGoalClick)
            GoalText(language = language, goal = pokemonTask.goal5, goalNum = 5, pokemonTask, onGoalClick)
        }
    }
}

//Points icon - only shown for tasks that actually give double points (points == 20). A same-size
//Spacer keeps every row's icon column aligned regardless of whether this task has one.
@Composable
fun PointsIcon(points: Int) {
    if (points == 20) {
        PokemonImage(
            imgId = R.drawable.double_points, size = 20,
            desc = stringResource(id = R.string.points_icon_desc),
            color = MaterialTheme.colorScheme.primary, alpha = 1f
        )
    } else {
        Spacer(modifier = Modifier.size(20.dp))
    }
}

//Task description with optional compact move type badge
@Composable
fun RowScope.TaskText(language: SupportedLanguage, task: String, onMoveClick: (String) -> Unit) {
    Column(
        modifier = Modifier
            .weight(1f)
            .padding(horizontal = 6.dp)
    ) {
        Text(
            translate(language, task),
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 13.5.sp,
                lineHeight = 17.sp
            )
        )

        val type = getMoveType(task)
        if (type.isNotBlank()) {
            val typeText = translate(language, "-type")
            val translatedType = translate(language, type)
            val bgColor = getTypeColor(type)

            Spacer(modifier = Modifier.height(3.dp))
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = bgColor,
                modifier = Modifier
                    .semantics { role = Role.Button }
                    .clickable { onMoveClick("$translatedType$typeText") }
            ) {
                Text(
                    text = translate(language, type),
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
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
            border = if (reached) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier
                .semantics { role = Role.Button }
                .clickable { onGoalClick(pokemonTask, goalNum) }
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .defaultMinSize(minWidth = 30.dp, minHeight = 30.dp)
                    .padding(horizontal = 5.dp, vertical = 3.dp)
            ) {
                Text(
                    translate(lang = language, text = goal),
                    color = if (reached) MaterialTheme.colorScheme.onTertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontSize = 11.5.sp
                    ),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
