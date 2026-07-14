package jzam.arcedex.models

//Pokemon class with basic information unique to a Pokemon
data class Pokemon(
    val hisuiId: Int,
    val natId: Int,
    val name: String,
    val imgId: Int
)

//Enums representing sort options
enum class PokeSort {
    NATIONAL, ALPHABETICAL, HISUI, RESEARCH_LEVEL
}

//The five overworld regions of Hisui where Pokemon can be encountered in the wild
enum class HisuiArea {
    OBSIDIAN_FIELDLANDS, CRIMSON_MIRELANDS, COBALT_COASTLANDS, CORONET_HIGHLANDS, ALABASTER_ICELANDS
}

//3-state filter for the main Pokemon list, matching the same Rank10/Perfect thresholds already
//used for the pokeball/masterball badge (see ProgressPokeballImage): Rank10 = 100+ raw points,
//Perfect = every single research task completed.
enum class HideFilter {
    SHOW_ALL, HIDE_RANK10, HIDE_PERFECT
}

//Categorizes research task text into a "verb" - what kind of task it is, independent of which
//Pokemon it belongs to. Lets the app filter/search across all 242 Pokemon by task type (e.g. "who
//still needs a Defeat-with-Fire-type task done") rather than just by move type as before.
enum class TaskCategory {
    CATCH, CATCH_ALPHA, CATCH_HEAVY, CATCH_LIGHT, CATCH_LARGE, CATCH_SMALL, CATCH_AIRBORNE,
    CATCH_SLEEPING, CATCH_UNNOTICED, DEFEAT, MOVE_SEEN, EVOLVE, FEED, ENVIRONMENT, ITEM_USE,
    INVESTIGATE
}
