package trif.novica.spoilerchecker.data.repository

import android.util.Log
import trif.novica.spoilerchecker.data.local.TeamDao
import trif.novica.spoilerchecker.data.model.Game
import trif.novica.spoilerchecker.data.model.GameStatus
import trif.novica.spoilerchecker.data.model.Team
import trif.novica.spoilerchecker.data.remote.EspnApi
import trif.novica.spoilerchecker.data.remote.dto.EventDto
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class GameRepository(
    private val teamDao: TeamDao,
    private val espnApi: EspnApi
) {
    companion object {
        private const val TAG = "GameRepository"
        private val DATE_FORMAT = SimpleDateFormat("yyyyMMdd", Locale.US)
        private val ISO_FORMAT = SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
    }

    /**
     * Get games for yesterday (most common use case for spoiler blocking).
     */
    suspend fun getYesterdaysGames(): Result<List<Game>> {
        val yesterday = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -1)
        }
        return getGamesForDate(yesterday)
    }

    /**
     * Get games for today.
     */
    suspend fun getTodaysGames(): Result<List<Game>> {
        return getGamesForDate(Calendar.getInstance())
    }

    /**
     * Get games for a specific date.
     */
    suspend fun getGamesForDate(date: Calendar): Result<List<Game>> {
        val dateString = DATE_FORMAT.format(date.time)
        return getGamesForDate(dateString)
    }

    /**
     * Get games for a specific date string (YYYYMMDD format).
     */
    suspend fun getGamesForDate(dateString: String): Result<List<Game>> {
        return try {
            val response = espnApi.getScoreboard(dateString)
            val events = response.events ?: emptyList()

            // Get all teams from local DB for mapping
            val allTeams = teamDao.getTeamsByLeagueOnce("NBA")
            val teamsById = allTeams.associateBy { it.id }

            val games = events.mapNotNull { event ->
                eventToGame(event, teamsById)
            }

            Log.d(TAG, "Fetched ${games.size} games for $dateString")
            Result.success(games)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch games for $dateString", e)
            Result.failure(e)
        }
    }

    /**
     * Convert API event to domain Game model.
     */
    private fun eventToGame(event: EventDto, teamsById: Map<Int, Team>): Game? {
        val competition = event.competitions?.firstOrNull() ?: return null
        val competitors = competition.competitors ?: return null

        val homeCompetitor = competitors.find { it.homeAway == "home" } ?: return null
        val awayCompetitor = competitors.find { it.homeAway == "away" } ?: return null

        val homeTeamId = homeCompetitor.team.id.toIntOrNull() ?: return null
        val awayTeamId = awayCompetitor.team.id.toIntOrNull() ?: return null

        // Try to get team from local DB, or create a minimal Team from API data
        val homeTeam = teamsById[homeTeamId] ?: createTeamFromCompetitor(homeCompetitor.team)
        val awayTeam = teamsById[awayTeamId] ?: createTeamFromCompetitor(awayCompetitor.team)

        val scheduledTime = try {
            ISO_FORMAT.parse(event.date)?.time ?: System.currentTimeMillis()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }

        val status = when (event.status?.type?.state) {
            "pre" -> GameStatus.SCHEDULED
            "in" -> GameStatus.LIVE
            "post" -> GameStatus.FINISHED
            else -> GameStatus.FINISHED
        }

        return Game(
            id = event.id,
            homeTeam = homeTeam,
            awayTeam = awayTeam,
            scheduledTime = scheduledTime,
            status = status,
            league = "NBA"
        )
    }

    /**
     * Create a minimal Team object from API competitor data.
     * Used when team is not in local DB yet.
     */
    private fun createTeamFromCompetitor(competitor: trif.novica.spoilerchecker.data.remote.dto.CompetitorTeamDto): Team {
        return Team(
            id = competitor.id.toInt(),
            name = competitor.shortDisplayName,
            fullName = competitor.displayName,
            abbreviation = competitor.abbreviation,
            city = competitor.location ?: "",
            league = "NBA",
            logoUrl = competitor.logo
        )
    }
}
