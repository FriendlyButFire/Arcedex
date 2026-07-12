package jzam.arcedex.data

import androidx.annotation.WorkerThread
import jzam.arcedex.models.PokeResearch
import kotlinx.coroutines.flow.Flow

/*
 * Intermediary class for accessing PokeResearch database info.
 */
class PokeResearchRepository(private val pokeResearchDao: PokeResearchDao) {

    fun getResearchTasks(): Flow<List<PokeResearch>> {
        return pokeResearchDao.getResearchTasks()
    }

    @Suppress("RedundantSuspendModifier")
    @WorkerThread
    suspend fun insert(task: PokeResearch) {
        pokeResearchDao.insert(task)
    }

    suspend fun update(task: PokeResearch) {
        pokeResearchDao.update(task)
    }

    suspend fun delete(task: PokeResearch) {
        pokeResearchDao.delete(task)
    }
}