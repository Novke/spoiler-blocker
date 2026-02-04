package trif.novica.spoilerchecker.data.repository

import android.util.Log
import kotlinx.coroutines.flow.Flow
import trif.novica.spoilerchecker.data.local.FavoriteTeamDao
import trif.novica.spoilerchecker.data.local.PlayerDao
import trif.novica.spoilerchecker.data.local.TeamDao
import trif.novica.spoilerchecker.data.model.FavoriteTeam
import trif.novica.spoilerchecker.data.model.Player
import trif.novica.spoilerchecker.data.model.Team
import trif.novica.spoilerchecker.data.remote.EspnApi
import java.util.concurrent.TimeUnit

class TeamRepository(
    private val teamDao: TeamDao,
    private val playerDao: PlayerDao,
    private val favoriteTeamDao: FavoriteTeamDao,
    private val espnApi: EspnApi
) {
    companion object {
        private const val TAG = "TeamRepository"
        private val ROSTER_CACHE_DURATION = TimeUnit.DAYS.toMillis(30)  // 30 days
    }

    // ===== TEAMS =====

    fun getTeams(league: String = "NBA"): Flow<List<Team>> = teamDao.getTeamsByLeague(league)

    suspend fun getTeamById(teamId: Int): Team? = teamDao.getTeamById(teamId)

    suspend fun searchTeams(query: String): List<Team> = teamDao.searchTeams(query)

    /**
     * Fetch teams from ESPN API and save to database.
     * Should be called once on first app launch.
     */
    suspend fun refreshTeams(): Result<Int> {
        return try {
            val response = espnApi.getTeams()
            val teams = response.sports?.firstOrNull()
                ?.leagues?.firstOrNull()
                ?.teams
                ?.map { wrapper ->
                    Team(
                        id = wrapper.team.id.toInt(),
                        name = wrapper.team.shortDisplayName,
                        fullName = wrapper.team.displayName,
                        abbreviation = wrapper.team.abbreviation,
                        city = wrapper.team.location,
                        league = "NBA",
                        logoUrl = wrapper.team.logos?.firstOrNull()?.href
                    )
                } ?: emptyList()

            teamDao.insertAll(teams)
            Log.d(TAG, "Refreshed ${teams.size} teams from ESPN API")
            Result.success(teams.size)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to refresh teams", e)
            Result.failure(e)
        }
    }

    /**
     * Ensure teams are loaded. Fetches from API if database is empty.
     */
    suspend fun ensureTeamsLoaded(): Result<Unit> {
        val count = teamDao.getTeamCount()
        if (count == 0) {
            val result = refreshTeams()
            return if (result.isSuccess) Result.success(Unit) else Result.failure(result.exceptionOrNull()!!)
        }
        return Result.success(Unit)
    }

    // ===== PLAYERS =====

    suspend fun getPlayersForTeam(teamId: Int): List<Player> {
        return playerDao.getPlayersByTeam(teamId)
    }

    suspend fun getPlayersForTeams(teamIds: List<Int>): List<Player> {
        return playerDao.getPlayersByTeams(teamIds)
    }

    /**
     * Fetch roster for a team from ESPN API and save to database.
     * Uses caching - only fetches if cache is expired (30 days).
     */
    suspend fun refreshRosterIfNeeded(teamId: Int, force: Boolean = false): Result<Int> {
        val team = teamDao.getTeamById(teamId) ?: return Result.failure(Exception("Team not found"))

        // Check cache
        if (!force && team.lastRosterUpdate != null) {
            val age = System.currentTimeMillis() - team.lastRosterUpdate
            if (age < ROSTER_CACHE_DURATION) {
                val cachedPlayers = playerDao.getPlayersByTeam(teamId)
                if (cachedPlayers.isNotEmpty()) {
                    Log.d(TAG, "Using cached roster for team $teamId (${cachedPlayers.size} players)")
                    return Result.success(cachedPlayers.size)
                }
            }
        }

        return refreshRoster(teamId)
    }

    /**
     * Force refresh roster from API.
     */
    suspend fun refreshRoster(teamId: Int): Result<Int> {
        return try {
            val response = espnApi.getTeamDetail(teamId.toString(), "roster")
            val athletes = response.team.athletes ?: emptyList()

            val players = athletes.map { athlete ->
                Player(
                    id = athlete.id.toInt(),
                    teamId = teamId,
                    fullName = athlete.fullName,
                    shortName = athlete.shortName,
                    jersey = athlete.jersey,
                    position = athlete.position?.abbreviation
                )
            }

            // Delete old players and insert new ones
            playerDao.deleteByTeam(teamId)
            playerDao.insertAll(players)

            // Update roster timestamp
            teamDao.updateRosterTimestamp(teamId, System.currentTimeMillis())

            Log.d(TAG, "Refreshed roster for team $teamId: ${players.size} players")
            Result.success(players.size)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to refresh roster for team $teamId", e)
            Result.failure(e)
        }
    }

    // ===== FAVORITES =====

    fun getFavoriteTeams(): Flow<List<Team>> = favoriteTeamDao.getFavoriteTeams()

    suspend fun getFavoriteTeamsOnce(): List<Team> = favoriteTeamDao.getFavoriteTeamsOnce()

    suspend fun addFavorite(teamId: Int) {
        favoriteTeamDao.insert(FavoriteTeam(teamId = teamId))
        Log.d(TAG, "Added team $teamId to favorites")
    }

    suspend fun removeFavorite(teamId: Int) {
        favoriteTeamDao.deleteByTeamId(teamId)
        Log.d(TAG, "Removed team $teamId from favorites")
    }

    suspend fun isFavorite(teamId: Int): Boolean = favoriteTeamDao.isFavorite(teamId)

    fun isFavoriteFlow(teamId: Int): Flow<Boolean> = favoriteTeamDao.isFavoriteFlow(teamId)
}
