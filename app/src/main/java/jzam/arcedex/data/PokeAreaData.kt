package jzam.arcedex.data

import jzam.arcedex.models.HisuiArea

/*
 * Maps each Hisui overworld region to the hisuiId of Pokemon that can be encountered there in the
 * wild OR obtained there by evolving a Pokemon already found in that region (evolution in PLA is
 * not area-locked, so evolved forms inherit every area their pre-evolution appears in - matching
 * how the in-game Pokedex tags regions per species). Base spawn data compiled from Game8's
 * per-area encounter tables (game8.co/games/Pokemon-Legends-Arceus) and cross-checked against
 * Pocket Tactics' full Pokedex location table (pockettactics.com/pokemon-legends-arceus/all-pokemon),
 * then propagated across each evolution family and corrected against a few confirmed in-game
 * discrepancies (Ralts/Kirlia/Gengar were wrongly tagged Obsidian Fieldlands in the source tables;
 * Togepi/Togetic were missing it despite Togekiss being present). A Pokemon can appear in more
 * than one area. Excludes Pokemon only obtainable via space-time distortions, main-story/
 * post-credits quests, or Mount Coronet (a separate location from the Coronet Highlands region).
 */
object PokeAreaData {

    private val obsidianFieldlands = setOf(
        10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29,
        30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49,
        50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63, 64, 65, 66, 67, 68, 69,
        70, 71, 72, 73, 74, 75, 76, 77, 78, 79, 80, 81, 82, 83, 84, 85, 86, 87, 88, 127,
        128, 129, 154, 155, 156, 202, 203, 204, 226, 233, 241
    )

    private val crimsonMirelands = setOf(
        4, 5, 6, 10, 11, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 39, 40, 43,
        44, 45, 46, 47, 48, 53, 54, 55, 56, 57, 66, 67, 68, 69, 70, 71, 89, 90, 91, 92,
        93, 94, 95, 96, 97, 98, 99, 100, 101, 102, 103, 104, 105, 106, 107, 108, 109, 110, 111, 112,
        113, 114, 115, 116, 117, 118, 119, 120, 121, 122, 123, 124, 125, 126, 127, 128, 129, 130, 131, 132,
        133, 134, 135, 136, 137, 138, 139, 140, 141, 142, 227, 234
    )

    private val cobaltCoastlands = setOf(
        10, 11, 12, 13, 14, 18, 19, 20, 21, 22, 25, 26, 27, 28, 29, 30, 31, 32, 33, 37,
        38, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 53, 54, 55, 56, 57, 68, 69, 70, 71,
        78, 79, 80, 81, 82, 83, 84, 85, 86, 87, 88, 89, 90, 91, 92, 95, 96, 99, 100, 127,
        128, 129, 140, 141, 143, 144, 145, 146, 147, 148, 149, 150, 151, 152, 153, 154, 155, 156, 157, 158,
        159, 160, 161, 162, 163, 164, 165, 166, 167, 168, 169, 170, 171, 172, 173, 174, 175, 176, 177, 178,
        179, 228, 232, 237, 238, 239
    )

    private val coronetHighlands = setOf(
        1, 2, 3, 15, 16, 17, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 43, 44,
        45, 46, 47, 48, 49, 50, 53, 54, 66, 67, 68, 69, 72, 73, 74, 75, 80, 81, 89, 90,
        91, 92, 95, 96, 97, 98, 99, 100, 105, 106, 107, 108, 110, 111, 112, 113, 114, 115, 116, 117,
        118, 119, 120, 121, 122, 123, 124, 127, 128, 129, 136, 137, 138, 154, 155, 156, 166, 167, 174, 175,
        176, 177, 178, 179, 180, 181, 182, 183, 184, 185, 186, 187, 188, 189, 190, 191, 192, 193, 194, 195,
        196, 197, 198, 199, 200, 201, 202, 203, 204, 208, 209, 210, 211, 230, 235, 236, 240, 242
    )

    private val alabasterIcelands = setOf(
        10, 11, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 43, 44, 45, 46,
        47, 48, 49, 50, 51, 52, 58, 59, 60, 64, 65, 78, 79, 86, 87, 88, 101, 102, 103, 104,
        125, 126, 136, 137, 138, 152, 153, 154, 155, 156, 158, 159, 160, 166, 167, 180, 181, 182, 183, 184,
        185, 186, 187, 188, 189, 195, 196, 197, 198, 202, 203, 204, 205, 206, 207, 212, 213, 214, 215, 216,
        217, 218, 219, 220, 221, 222, 223, 224
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
