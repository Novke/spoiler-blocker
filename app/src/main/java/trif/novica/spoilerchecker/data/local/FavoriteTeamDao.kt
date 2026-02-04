package trif.novica.spoilerchecker.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import trif.novica.spoilerchecker.data.model.FavoriteTeam
import trif.novica.spoilerchecker.data.model.Team

@Dao
interface FavoriteTeamDao {

    @Query("SELECT * FROM favorite_teams ORDER BY addedAt DESC")
    fun getAllFavorites(): Flow<List<FavoriteTeam>>

    @Query("SELECT * FROM favorite_teams ORDER BY addedAt DESC")
    suspend fun getAllFavoritesOnce(): List<FavoriteTeam>

    /**
     * Get favorite teams with full Team data joined.
     */
    @Query("""
        SELECT t.* FROM teams t
        INNER JOIN favorite_teams f ON t.id = f.teamId
        ORDER BY f.addedAt DESC
    """)
    fun getFavoriteTeams(): Flow<List<Team>>

    /**
     * Get favorite teams with full Team data (non-flow version).
     */
    @Query("""
        SELECT t.* FROM teams t
        INNER JOIN favorite_teams f ON t.id = f.teamId
        ORDER BY f.addedAt DESC
    """)
    suspend fun getFavoriteTeamsOnce(): List<Team>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(favorite: FavoriteTeam)

    @Query("DELETE FROM favorite_teams WHERE teamId = :teamId")
    suspend fun deleteByTeamId(teamId: Int)

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_teams WHERE teamId = :teamId)")
    suspend fun isFavorite(teamId: Int): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_teams WHERE teamId = :teamId)")
    fun isFavoriteFlow(teamId: Int): Flow<Boolean>

    @Query("SELECT COUNT(*) FROM favorite_teams")
    suspend fun count(): Int
}
