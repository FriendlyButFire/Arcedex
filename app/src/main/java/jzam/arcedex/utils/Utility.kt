package jzam.arcedex.utils

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.intl.LocaleList
import jzam.arcedex.data.PokeMovesData
import jzam.arcedex.data.PokeResearchData
import jzam.arcedex.data.PokeTranslateData
import jzam.arcedex.models.SupportedLanguage
import jzam.arcedex.models.TaskCategory

/*
 * General utility methods for the app
 */
fun formatPokemonId(id: Int): String {
    return when {
        id < 10 -> {
            "No. 00" + id.toString()
        }
        id < 100 -> {
            "No. 0" + id.toString()
        }
        else -> {
            "No. " + id.toString()
        }
    }
}

fun formatPokemonResearchInfo(
    lang: SupportedLanguage,
    goalsDone: Int, goalsTotal: Int, pointsDone: Int,
    pointsTotal: Int
): String {
    return (translate(lang, "Tasks") + ": $goalsDone/$goalsTotal | " +
            translate(lang, "Points") + ": $pointsDone/$pointsTotal")
}

fun formatSearchedText(lang: SupportedLanguage, searchText: String): String {
    return translate(lang, "Searched for") + " $searchText"
}

fun getResearchRank(lang: SupportedLanguage, points: Int): String {
    val ranks = PokeResearchData.ranks
    val rank = when {
        points < ranks[1] -> "0"
        points < ranks[2] -> "1"
        points < ranks[3] -> "2"
        points < ranks[4] -> "3"
        points < ranks[5] -> "4"
        points < ranks[6] -> "5"
        points < ranks[7] -> "6"
        points < ranks[8] -> "7"
        points < ranks[9] -> "8"
        points < ranks[10] -> "9"
        else -> "10"
    }
    return translate(lang, "Research Rank") + " " + rank
}

fun getPointsToNextRankText(lang: SupportedLanguage, points: Int): String {
    val ranks = PokeResearchData.ranks
    val pointsNeeded = when {
        points < ranks[1] -> ranks[1] - points
        points < ranks[2] -> ranks[2] - points
        points < ranks[3] -> ranks[3] - points
        points < ranks[4] -> ranks[4] - points
        points < ranks[5] -> ranks[5] - points
        points < ranks[6] -> ranks[6] - points
        points < ranks[7] -> ranks[7] - points
        points < ranks[8] -> ranks[8] - points
        points < ranks[9] -> ranks[9] - points
        points < ranks[10] -> ranks[10] - points
        else -> 0
    }
    return translate(lang, "Points to next rank:") + " " + pointsNeeded
}

//Sum of every task's max points across every Pokemon, plus the 100pt completion bonus per
//Pokemon, gives the theoretical max total research points (currently 125,100). This is a fixed
//value derived from static data, so it's computed once and cached rather than recalculated on
//every call (this function is invoked from a composable that recomposes on every point change).
private val totalPointsPossibleCache: Int by lazy {
    PokeResearchData.tasks
        .groupBy { it.name }
        .values
        .sumOf { tasksForPokemon -> tasksForPokemon.sumOf { it.points * it.totalGoals } + 100 }
}

fun getTotalPointsPossible(): Int = totalPointsPossibleCache

fun getRankProgress(points: Float): Float {
    val ranks = PokeResearchData.ranks
    return when {
        points < ranks[1] -> ((points - ranks[0]) / (ranks[1] - ranks[0]))
        points < ranks[2] -> ((points - ranks[1]) / (ranks[2] - ranks[1]))
        points < ranks[3] -> ((points - ranks[2]) / (ranks[3] - ranks[2]))
        points < ranks[4] -> ((points - ranks[3]) / (ranks[4] - ranks[3]))
        points < ranks[5] -> ((points - ranks[4]) / (ranks[5] - ranks[4]))
        points < ranks[6] -> ((points - ranks[5]) / (ranks[6] - ranks[5]))
        points < ranks[7] -> ((points - ranks[6]) / (ranks[7] - ranks[6]))
        points < ranks[8] -> ((points - ranks[7]) / (ranks[8] - ranks[7]))
        points < ranks[9] -> ((points - ranks[8]) / (ranks[9] - ranks[8]))
        points < ranks[10] -> ((points - ranks[9]) / (ranks[10] - ranks[9]))
        else -> 1f
    }
}

fun translate(lang: SupportedLanguage, text: String): String {

    val translation = when (lang) {
        SupportedLanguage.JAPANESE -> PokeTranslateData.jp.find { it.oldText == text }
        else -> null
    }
    if (translation != null) {
        return translation.newText
    } else {
        return text
    }
}

fun getSupportedLanguage(locales: LocaleList): SupportedLanguage {
    for (locale in locales) {
        if (PokeTranslateData.languages.contains(locale.language)) {
            return when (locale.language) {
                "ja" -> SupportedLanguage.JAPANESE
                else -> SupportedLanguage.ENGLISH
            }
        }
    }
    return SupportedLanguage.ENGLISH
}

fun getMoveType(task: String): String {
    val moveName = task.replace("Times you’ve seen it use ", "")
    val move = PokeMovesData.moves.find { it.name == moveName }
    if (move != null && move.power != "—") {
        return (move.type)
    }
    return ""
}

//Extracts the type relevant to a given category's task text - Defeat tasks embed the type
//directly ("...with Fire-type moves"), Move Seen tasks need a move-name lookup via getMoveType()
fun taskCategoryTypeOf(category: TaskCategory, taskText: String): String {
    return when (category) {
        TaskCategory.DEFEAT -> getDefeatType(taskText)
        TaskCategory.MOVE_SEEN -> getMoveType(taskText)
        else -> ""
    }
}

//Categorizes a research task's text into a TaskCategory "verb", independent of which Pokemon it
//belongs to. Matched against the fixed set of task phrasings used throughout PokeResearchData -
//uses "." in place of the apostrophe in regexes since the source data uses a typographic
//apostrophe (’) rather than a straight one, and this way the match doesn't depend on getting that
//exact character right.
fun getTaskCategory(task: String): TaskCategory? {
    return when {
        task.startsWith("Number caught") -> TaskCategory.CATCH
        task == "Number of alpha specimens caught" -> TaskCategory.CATCH_ALPHA
        Regex("Number of heavy specimens you.ve caught").matches(task) -> TaskCategory.CATCH_HEAVY
        Regex("Number of light specimens you.ve caught").matches(task) -> TaskCategory.CATCH_LIGHT
        Regex("Number of large specimens you.ve caught").matches(task) -> TaskCategory.CATCH_LARGE
        Regex("Number of small specimens you.ve caught").matches(task) -> TaskCategory.CATCH_SMALL
        Regex("Number you.ve caught while they were in the air").matches(task) -> TaskCategory.CATCH_AIRBORNE
        Regex("Number you.ve caught while they were sleeping").matches(task) -> TaskCategory.CATCH_SLEEPING
        Regex("Number you.ve caught without being spotted").matches(task) -> TaskCategory.CATCH_UNNOTICED
        task == "Number defeated" -> TaskCategory.DEFEAT
        Regex("Number you.ve defeated with [A-Za-z]+-type moves").matches(task) -> TaskCategory.DEFEAT
        Regex("Number you.ve evolved").matches(task) -> TaskCategory.EVOLVE
        Regex("Number of different forms you.ve obtained").matches(task) -> TaskCategory.EVOLVE
        task.startsWith("Times you’ve seen it use") -> TaskCategory.MOVE_SEEN
        Regex("Times you.ve given it food").matches(task) -> TaskCategory.FEED
        task.startsWith("Number you’ve seen leap out of") -> TaskCategory.ENVIRONMENT
        Regex("Times you.ve scared it off with a Scatter Bang").matches(task) -> TaskCategory.ITEM_USE
        Regex("Times you.ve stunned it by using items").matches(task) -> TaskCategory.ITEM_USE
        task.startsWith("Investigated") -> TaskCategory.INVESTIGATE
        else -> null
    }
}

//Extracts the elemental type from a "Number you've defeated with X-type moves" task, in the same
//ALL-CAPS format getMoveType()/getTypeColor() expect (e.g. "FIRE"). Returns "" for the plain
//"Number defeated" task, which has no type restriction.
fun getDefeatType(task: String): String {
    val match = Regex("Number you.ve defeated with ([A-Za-z]+)-type moves").find(task)
    return match?.groupValues?.get(1)?.uppercase() ?: ""
}

fun getTypeColor(type: String): Color {
    val typeColor = PokeMovesData.typeColors.find { it.type == type}
    if (typeColor != null) {
        return typeColor.color
    }
    return Color.White
}