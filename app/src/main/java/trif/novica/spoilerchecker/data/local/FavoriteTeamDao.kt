package trif.novica.spoilerchecker.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import trif.novica.spoilerchecker.data.model.FavoriteTeam

@Dao
interface FavoriteTeamDao {
    @Query("SELECT * FROM favorite_teams ORDER BY addedAt DESC LIMIT 8")
    fun getAllTeams(): Flow<List<FavoriteTeam>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(team: FavoriteTeam): Long

    @Delete
    suspend fun delete(team: FavoriteTeam)

    @Query("SELECT COUNT(*) FROM favorite_teams WHERE LOWER(name) = LOWER(:name)")
    suspend fun countByName(name: String): Int
}
