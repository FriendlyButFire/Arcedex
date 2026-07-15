package jzam.arcedex

import jzam.arcedex.models.SupportedLanguage
import jzam.arcedex.models.TaskCategory
import jzam.arcedex.utils.*
import org.junit.Assert.*
import org.junit.Test

class UtilityTest {

    @Test
    fun testFormatPokemonId() {
        assertEquals("No. 001", formatPokemonId(1))
        assertEquals("No. 015", formatPokemonId(15))
        assertEquals("No. 100", formatPokemonId(100))
        assertEquals("No. 242", formatPokemonId(242))
    }

    @Test
    fun testResearchRankCalculation() {
        assertEquals("Research Rank 0", getResearchRank(SupportedLanguage.ENGLISH, 0))
        assertEquals("Research Rank 0", getResearchRank(SupportedLanguage.ENGLISH, 100))
        assertEquals("Research Rank 1", getResearchRank(SupportedLanguage.ENGLISH, 500))
        assertEquals("Research Rank 10", getResearchRank(SupportedLanguage.ENGLISH, 125100))
    }

    @Test
    fun testPointsToNextRankText() {
        val text = getPointsToNextRankText(SupportedLanguage.ENGLISH, 0)
        assertTrue(text.contains("500") || text.contains("Points to next rank"))
    }

    @Test
    fun testTotalPointsPossible() {
        val total = getTotalPointsPossible()
        assertTrue(total > 100000)
    }

    @Test
    fun testTaskCategoryParsing() {
        assertEquals(TaskCategory.CATCH, getTaskCategory("Number caught"))
        assertEquals(TaskCategory.CATCH_ALPHA, getTaskCategory("Number of alpha specimens caught"))
        assertEquals(TaskCategory.CATCH_HEAVY, getTaskCategory("Number of heavy specimens you’ve caught"))
        assertEquals(TaskCategory.CATCH_HEAVY, getTaskCategory("Number of heavy specimens you've caught"))
        assertEquals(TaskCategory.CATCH_LIGHT, getTaskCategory("Number of light specimens you’ve caught"))
        assertEquals(TaskCategory.CATCH_LARGE, getTaskCategory("Number of large specimens you’ve caught"))
        assertEquals(TaskCategory.CATCH_SMALL, getTaskCategory("Number of small specimens you’ve caught"))
        assertEquals(TaskCategory.CATCH_AIRBORNE, getTaskCategory("Number you’ve caught while they were in the air"))
        assertEquals(TaskCategory.CATCH_SLEEPING, getTaskCategory("Number you’ve caught while they were sleeping"))
        assertEquals(TaskCategory.CATCH_UNNOTICED, getTaskCategory("Number you’ve caught without being spotted"))
        assertEquals(TaskCategory.DEFEAT, getTaskCategory("Number defeated"))
        assertEquals(TaskCategory.DEFEAT, getTaskCategory("Number you’ve defeated with Fire-type moves"))
        assertEquals(TaskCategory.MOVE_SEEN, getTaskCategory("Times you’ve seen it use Thunderbolt"))
        assertEquals(TaskCategory.EVOLVE, getTaskCategory("Number you’ve evolved"))
        assertEquals(TaskCategory.FEED, getTaskCategory("Times you’ve given it food"))
        assertEquals(TaskCategory.ENVIRONMENT, getTaskCategory("Number you’ve seen leap out of ore deposits"))
        assertEquals(TaskCategory.ITEM_USE, getTaskCategory("Times you’ve stunned it by using items"))
        assertEquals(TaskCategory.INVESTIGATE, getTaskCategory("Investigated whether Clefairy dance under a full moon"))
        assertNull(getTaskCategory("Some random non-task string"))
    }

    @Test
    fun testDefeatTypeParsing() {
        assertEquals("FIRE", getDefeatType("Number you’ve defeated with Fire-type moves"))
        assertEquals("WATER", getDefeatType("Number you've defeated with Water-type moves"))
        assertEquals("", getDefeatType("Number defeated"))
    }

    @Test
    fun testMoveTypeLookup() {
        assertEquals("ELECTRIC", getMoveType("Times you’ve seen it use Thunderbolt"))
        assertEquals("FIRE", getMoveType("Times you’ve seen it use Flamethrower"))
        assertEquals("", getMoveType("Random non move task"))
    }

    @Test
    fun testTranslation() {
        assertEquals("モクロー", translate(SupportedLanguage.JAPANESE, "Rowlet"))
        assertEquals("Rowlet", translate(SupportedLanguage.ENGLISH, "Rowlet"))
        assertEquals("UnknownWord123", translate(SupportedLanguage.JAPANESE, "UnknownWord123"))
    }
}