package trif.novica.spoilerchecker.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import trif.novica.spoilerchecker.data.model.Team

@Dao
interface TeamDao {

    @Query("SELECT * FROM teams WHERE league = :league ORDER BY fullName ASC")
    fun getTeamsByLeague(league: String = "NBA"): Flow<List<Team>>

    @Query("SELECT * FROM teams WHERE league = :league ORDER BY fullName ASC")
    suspend fun getTeamsByLeagueOnce(league: String = "NBA"): List<Team>

    @Query("SELECT * FROM teams WHERE id = :teamId")
    suspend fun getTeamById(teamId: Int): Team?

    @Query("SELECT * FROM teams WHERE id IN (:teamIds)")
    suspend fun getTeamsByIds(teamIds: List<Int>): List<Team>

    @Query("SELECT * FROM teams WHERE fullName LIKE '%' || :query || '%' OR name LIKE '%' || :query || '%' OR abbreviation LIKE '%' || :query || '%' OR city LIKE '%' || :query || '%'")
    suspend fun searchTeams(query: String): List<Team>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(teams: List<Team>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(team: Team)

    @Update
    suspend fun update(team: Team)

    @Query("UPDATE teams SET lastRosterUpdate = :timestamp WHERE id = :teamId")
    suspend fun updateRosterTimestamp(teamId: Int, timestamp: Long)

    @Query("SELECT COUNT(*) FROM teams WHERE league = :league")
    suspend fun getTeamCount(league: String = "NBA"): Int

    @Query("DELETE FROM teams WHERE league = :league")
    suspend fun deleteByLeague(league: String = "NBA")
}
