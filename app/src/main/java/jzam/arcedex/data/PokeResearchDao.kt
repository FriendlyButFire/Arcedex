package jzam.arcedex.data

import androidx.room.*
import jzam.arcedex.models.PokeResearch
import kotlinx.coroutines.flow.Flow

/*
 * The DAO (data access object) interface for methods to access the database via SQL queries
 */
@Dao
interface PokeResearchDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(task: PokeResearch)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(tasks: List<PokeResearch>)

    @Update
    suspend fun update(task: PokeResearch)

    @Delete
    suspend fun delete(task: PokeResearch)

    @Query("DELETE FROM pokeresearch_table")
    suspend fun deleteAll()

    @Query("SELECT * FROM pokeresearch_table")
    fun getResearchTasks(): Flow<List<PokeResearch>>

    @Query("SELECT COUNT(*) FROM pokeresearch_table")
    suspend fun getCount(): Int

}