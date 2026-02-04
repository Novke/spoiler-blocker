package trif.novica.spoilerchecker.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import trif.novica.spoilerchecker.data.model.CleaningResult
import trif.novica.spoilerchecker.data.model.FavoriteTeam
import trif.novica.spoilerchecker.data.model.Player
import trif.novica.spoilerchecker.data.model.Team

@Database(
    entities = [
        Team::class,
        Player::class,
        FavoriteTeam::class,
        CleaningResult::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun teamDao(): TeamDao
    abstract fun playerDao(): PlayerDao
    abstract fun favoriteTeamDao(): FavoriteTeamDao
    abstract fun cleaningResultDao(): CleaningResultDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "spoiler_shield_db"
                )
                    .fallbackToDestructiveMigration()  // For development - recreate DB on schema change
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
