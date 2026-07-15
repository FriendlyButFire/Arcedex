package jzam.arcedex.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import jzam.arcedex.R
import jzam.arcedex.data.PokeMovesData
import jzam.arcedex.models.*
import jzam.arcedex.utils.getPointsToNextRankText
import jzam.arcedex.utils.getRankProgress
import jzam.arcedex.utils.getResearchRank
import jzam.arcedex.utils.getTotalPointsPossible

//App's top bar - Shows app name, research rank progress, and filter/sort chips
@Composable
fun ArcedexTopBar(
    language: SupportedLanguage, userPoints: Int, hideFilter: HideFilter, selectedArea: HisuiArea?,
    selectedCategory: TaskCategory?, selectedCategoryType: String?,
    onSortChosen: (PokeSort) -> Unit, onHideFilterCycled: () -> Unit,
    onAreaChosen: (HisuiArea?) -> Unit,
    onCategoryChosen: (TaskCategory?) -> Unit, onCategoryTypeChosen: (String?) -> Unit,
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
                CategoryButton(selectedCategory, onCategoryChosen)
                if (selectedCategory == TaskCategory.DEFEAT || selectedCategory == TaskCategory.MOVE_SEEN) {
                    CategoryTypeButton(selectedCategoryType, onCategoryTypeChosen)
                }
                BackupButton(onExport, onImport)
            }
        }
    }
}

//3-state chip cycling SHOW_ALL -> HIDE_RANK10 -> HIDE_PERFECT -> SHOW_ALL on tap.
@Composable
fun HideFilterButton(hideFilter: HideFilter, onCycle: () -> Unit) {
    val label = when (hideFilter) {
        HideFilter.SHOW_ALL -> stringResource(R.string.filter_show_all_label)
        HideFilter.HIDE_RANK10 -> stringResource(R.string.filter_hide_rank10_label)
        HideFilter.HIDE_PERFECT -> stringResource(R.string.filter_hide_perfect_label)
    }
    val selected = hideFilter != HideFilter.SHOW_ALL
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

//Chip to open the sort menu
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

//Chip to open the region filter menu
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

//Chip that opens the task-category filter menu
@Composable
fun CategoryButton(selectedCategory: TaskCategory?, onCategoryChosen: (TaskCategory?) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        AssistChip(
            onClick = { expanded = true },
            label = {
                Text(
                    selectedCategory?.let { categoryDisplayName(it) } ?: stringResource(R.string.category_label),
                    style = MaterialTheme.typography.labelLarge
                )
            },
            colors = AssistChipDefaults.assistChipColors(
                containerColor = if (selectedCategory != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                labelColor = if (selectedCategory != null) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
            ),
            border = if (selectedCategory != null) null else AssistChipDefaults.assistChipBorder(
                enabled = true,
                borderColor = MaterialTheme.colorScheme.outline
            )
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.all_categories_label)) },
                onClick = {
                    onCategoryChosen(null)
                    expanded = false
                })
            for (category in TaskCategory.values()) {
                DropdownMenuItem(
                    text = { Text(categoryDisplayName(category)) },
                    onClick = {
                        onCategoryChosen(category)
                        expanded = false
                    })
            }
        }
    }
}

//Chip that opens the secondary elemental-type filter
@Composable
fun CategoryTypeButton(selectedType: String?, onTypeChosen: (String?) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val types = remember { PokeMovesData.typeColors.map { it.type } }

    Box {
        AssistChip(
            onClick = { expanded = true },
            label = {
                Text(
                    selectedType ?: stringResource(R.string.type_label),
                    style = MaterialTheme.typography.labelLarge
                )
            },
            colors = AssistChipDefaults.assistChipColors(
                containerColor = if (selectedType != null) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant,
                labelColor = if (selectedType != null) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurface
            ),
            border = if (selectedType != null) null else AssistChipDefaults.assistChipBorder(
                enabled = true,
                borderColor = MaterialTheme.colorScheme.outline
            )
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.any_type_label)) },
                onClick = {
                    onTypeChosen(null)
                    expanded = false
                })
            for (type in types) {
                DropdownMenuItem(
                    text = { Text(type) },
                    onClick = {
                        onTypeChosen(type)
                        expanded = false
                    })
            }
        }
    }
}

//Localized display name for a task category
@Composable
fun categoryDisplayName(category: TaskCategory): String {
    return stringResource(
        when (category) {
            TaskCategory.CATCH -> R.string.category_catch_label
            TaskCategory.CATCH_ALPHA -> R.string.category_catch_alpha_label
            TaskCategory.CATCH_HEAVY -> R.string.category_catch_heavy_label
            TaskCategory.CATCH_LIGHT -> R.string.category_catch_light_label
            TaskCategory.CATCH_LARGE -> R.string.category_catch_large_label
            TaskCategory.CATCH_SMALL -> R.string.category_catch_small_label
            TaskCategory.CATCH_AIRBORNE -> R.string.category_catch_airborne_label
            TaskCategory.CATCH_SLEEPING -> R.string.category_catch_sleeping_label
            TaskCategory.CATCH_UNNOTICED -> R.string.category_catch_unnoticed_label
            TaskCategory.DEFEAT -> R.string.category_defeat_label
            TaskCategory.MOVE_SEEN -> R.string.category_move_seen_label
            TaskCategory.EVOLVE -> R.string.category_evolve_label
            TaskCategory.FEED -> R.string.category_feed_label
            TaskCategory.ENVIRONMENT -> R.string.category_environment_label
            TaskCategory.ITEM_USE -> R.string.category_item_use_label
            TaskCategory.INVESTIGATE -> R.string.category_investigate_label
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
