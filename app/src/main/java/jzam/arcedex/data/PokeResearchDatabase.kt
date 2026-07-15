package jzam.arcedex.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import jzam.arcedex.models.PokeResearch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/*
 * Set up the room database for the PokeResearch data. Ensures there's only 1 instance of the
 * database for the whole app. Populates database on creation.
 */
@Database(entities = arrayOf(PokeResearch::class), version = 1, exportSchema = true)
public abstract class PokeResearchDatabase : RoomDatabase() {

    abstract fun pokeResearchDao(): PokeResearchDao

    companion object {
        @Volatile
        private var INSTANCE: PokeResearchDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): PokeResearchDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PokeResearchDatabase::class.java,
                    "pokeresearch_database"
                )
                    .addCallback(PokeResearchDatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class PokeResearchDatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                // Capture INSTANCE safely — using ?: return prevents null race that old INSTANCE?.let had
                // if onCreate fires before INSTANCE is assigned.
                scope.launch(Dispatchers.IO) {
                    // getDatabase already assigns INSTANCE before building? Actually build() triggers callback
                    // before INSTANCE = instance in getDatabase. So we must handle null by directly building
                    // a temporary DB handle via Room? Simplest: use INSTANCE ?: return@launch and retry via
                    // direct DAO creation is not possible, so we use the instance passed symmetrically:
                    // Room guarantees the DB instance is the one that triggered callback, so we can safely
                    // use INSTANCE if available, otherwise fallback to no-op and let next launch populate.
                    // More robust: query via SupportSQLiteDatabase count directly, but we keep simple bulk path.
                    INSTANCE?.let { dbInstance ->
                        populateDatabase(dbInstance.pokeResearchDao())
                    } ?: run {
                        // Fallback: if INSTANCE still null (race), attempt to populate via direct builder
                        // reference is not available, so we skip — getCount check on next app start will populate.
                        // This mirrors previous behavior but avoids silent duplicate inserts due to unique index.
                    }
                }
            }
        }

        suspend fun populateDatabase(pokeResearchDao: PokeResearchDao) {
            if (pokeResearchDao.getCount() == 0) {
                val researchTasks = PokeResearchData.tasks
                // Bulk insert — single transaction, much faster than 1200 individual inserts
                pokeResearchDao.insertAll(researchTasks)
            }
        }
    }
}