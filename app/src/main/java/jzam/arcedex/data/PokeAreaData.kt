package jzam.arcedex.data

import jzam.arcedex.models.HisuiArea

/*
 * Maps each Hisui overworld region to the hisuiId of Pokemon that can be encountered there in the
 * wild. Compiled from Game8's per-area encounter tables (game8.co/games/Pokemon-Legends-Arceus)
 * and cross-checked against Pocket Tactics' full Pokedex location table
 * (pockettactics.com/pokemon-legends-arceus/all-pokemon). A Pokemon can appear in more than one
 * area. Excludes Pokemon only obtainable via evolution, space-time distortions, main-story/
 * post-credits quests, or Mount Coronet (a separate location from the Coronet Highlands region).
 */
object PokeAreaData {

    private val obsidianFieldlands = setOf(
        10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 31, 33, 34, 35, 37, 38, 39,
        40, 41, 42, 43, 44, 45, 46, 47, 49, 51, 52, 53, 54, 55, 56, 58, 59, 60, 61, 62, 63, 64, 65,
        66, 67, 68, 69, 70, 72, 75, 76, 77, 78, 80, 81, 82, 83, 84, 86, 87, 88, 101, 102, 129, 138,
        154, 155, 202, 204, 226, 233, 241
    )

    private val crimsonMirelands = setOf(
        4, 5, 6, 10, 11, 28, 30, 34, 35, 39, 40, 43, 44, 46, 47, 53, 54, 55, 56, 57, 66, 67, 68, 69,
        70, 71, 89, 90, 91, 92, 93, 95, 96, 97, 98, 99, 100, 101, 102, 105, 106, 107, 108, 109, 110,
        111, 112, 113, 115, 116, 118, 120, 123, 124, 125, 126, 127, 128, 130, 131, 132, 133, 134,
        135, 136, 137, 139, 140, 141, 142, 227, 234
    )

    private val cobaltCoastlands = setOf(
        11, 12, 13, 14, 20, 22, 25, 26, 37, 38, 41, 42, 43, 44, 45, 46, 47, 49, 53, 54, 56, 68, 69,
        70, 78, 79, 81, 82, 83, 84, 86, 87, 89, 90, 92, 95, 100, 127, 128, 140, 141, 143, 144, 145,
        146, 147, 148, 149, 150, 152, 153, 154, 155, 157, 158, 159, 160, 161, 162, 163, 164, 165,
        166, 168, 169, 170, 171, 172, 173, 174, 175, 177, 178, 228, 232, 237, 238, 239
    )

    private val coronetHighlands = setOf(
        1, 2, 3, 15, 16, 17, 27, 34, 35, 36, 43, 44, 45, 46, 47, 48, 49, 53, 54, 66, 67, 68, 69, 72,
        75, 80, 81, 89, 90, 92, 95, 97, 98, 99, 100, 105, 106, 107, 108, 110, 111, 112, 113, 115,
        116, 117, 118, 119, 120, 121, 122, 123, 124, 127, 128, 136, 137, 154, 155, 166, 176, 179,
        180, 181, 182, 183, 184, 185, 186, 187, 188, 189, 190, 191, 192, 194, 195, 196, 197, 198,
        199, 200, 201, 202, 208, 209, 210, 211, 230, 235, 236, 240, 242
    )

    private val alabasterIcelands = setOf(
        10, 11, 25, 34, 35, 36, 37, 38, 43, 44, 47, 49, 51, 52, 58, 59, 64, 65, 78, 79, 86, 87, 88,
        101, 102, 103, 104, 125, 126, 136, 137, 152, 153, 154, 155, 156, 158, 159, 160, 166, 180,
        181, 182, 183, 185, 187, 188, 189, 195, 196, 197, 202, 205, 206, 207, 212, 213, 214, 215,
        216, 217, 218, 219, 220, 221, 222, 223, 224
    )

    private val areasByHisuiId: Map<Int, Set<HisuiArea>> by lazy {
        val map = mutableMapOf<Int, MutableSet<HisuiArea>>()
        fun add(ids: Set<Int>, area: HisuiArea) {
            for (id in ids) {
                map.getOrPut(id) { mutableSetOf() }.add(area)
            }
        }
        add(obsidianFieldlands, HisuiArea.OBSIDIAN_FIELDLANDS)
        add(crimsonMirelands, HisuiArea.CRIMSON_MIRELANDS)
        add(cobaltCoastlands, HisuiArea.COBALT_COASTLANDS)
        add(coronetHighlands, HisuiArea.CORONET_HIGHLANDS)
        add(alabasterIcelands, HisuiArea.ALABASTER_ICELANDS)
        map
    }

    fun getAreas(hisuiId: Int): Set<HisuiArea> = areasByHisuiId[hisuiId] ?: emptySet()
}
