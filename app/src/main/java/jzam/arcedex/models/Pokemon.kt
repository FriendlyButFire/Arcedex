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
