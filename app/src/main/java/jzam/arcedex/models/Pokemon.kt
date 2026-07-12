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
    NATIONAL, ALPHABETICAL, HISUI
}

//The five overworld regions of Hisui where Pokemon can be encountered in the wild
enum class HisuiArea {
    OBSIDIAN_FIELDLANDS, CRIMSON_MIRELANDS, COBALT_COASTLANDS, CORONET_HIGHLANDS, ALABASTER_ICELANDS
}
