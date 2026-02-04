package trif.novica.spoilerchecker.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import trif.novica.spoilerchecker.data.model.Player

@Dao
interface PlayerDao {

    @Query("SELECT * FROM players WHERE teamId = :teamId ORDER BY fullName ASC")
    suspend fun getPlayersByTeam(teamId: Int): List<Player>

    @Query("SELECT * FROM players WHERE teamId IN (:teamIds) ORDER BY fullName ASC")
    suspend fun getPlayersByTeams(teamIds: List<Int>): List<Player>

    @Query("SELECT * FROM players WHERE id = :playerId")
    suspend fun getPlayerById(playerId: Int): Player?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(players: List<Player>)

    @Query("DELETE FROM players WHERE teamId = :teamId")
    suspend fun deleteByTeam(teamId: Int)

    @Query("DELETE FROM players")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM players WHERE teamId = :teamId")
    suspend fun getPlayerCountForTeam(teamId: Int): Int
}
