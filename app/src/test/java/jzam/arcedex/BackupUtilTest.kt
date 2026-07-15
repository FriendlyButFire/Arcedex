package jzam.arcedex

import jzam.arcedex.models.PokeResearch
import jzam.arcedex.utils.exportProgress
import jzam.arcedex.utils.parseBackup
import org.junit.Assert.*
import org.junit.Test

class BackupUtilTest {

    @Test
    fun testExportAndParseBackupRoundTrip() {
        val sampleTasks = listOf(
            PokeResearch(
                id = 1,
                name = "Rowlet",
                task = "Number caught",
                goal1 = "1",
                goal2 = "2",
                goal3 = "4",
                goal4 = "10",
                goal5 = "15",
                goalProgress = 3,
                totalGoals = 5,
                points = 10
            ),
            PokeResearch(
                id = 2,
                name = "Cyndaquil",
                task = "Times you’ve seen it use Ember",
                goal1 = "1",
                goal2 = "3",
                goal3 = "6",
                goal4 = "12",
                goal5 = "25",
                goalProgress = 0, // Unused task, shouldn't be exported
                totalGoals = 5,
                points = 10
            )
        )

        val backupCode = exportProgress(sampleTasks)
        assertNotNull(backupCode)
        assertTrue(backupCode.isNotBlank())

        val restoredEntries = parseBackup(backupCode)
        assertEquals(1, restoredEntries.size)
        val (name, task, progress) = restoredEntries[0]
        assertEquals("Rowlet", name)
        assertEquals("Number caught", task)
        assertEquals(3, progress)
    }

    @Test
    fun testInvalidBackupThrowsException() {
        val invalidCode = "Not A Valid Backup Code!!!"
        try {
            parseBackup(invalidCode)
            fail("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertEquals("This doesn't look like a valid Arcedex backup code.", e.message)
        }
    }
}
