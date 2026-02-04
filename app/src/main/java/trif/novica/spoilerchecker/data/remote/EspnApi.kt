package trif.novica.spoilerchecker.data.remote

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import trif.novica.spoilerchecker.data.remote.dto.ScoreboardResponse
import trif.novica.spoilerchecker.data.remote.dto.TeamDetailResponse
import trif.novica.spoilerchecker.data.remote.dto.TeamsResponse

/**
 * ESPN API interface for NBA data.
 * Base URL: https://site.api.espn.com/apis/site/v2/sports/basketball/nba/
 *
 * This is an unofficial API - no authentication required.
 */
interface EspnApi {

    /**
     * Get all NBA teams.
     * Example: /teams
     */
    @GET("teams")
    suspend fun getTeams(): TeamsResponse

    /**
     * Get team details with roster.
     * Example: /teams/9?enable=roster
     *
     * @param teamId ESPN team ID (e.g., "9" for Golden State Warriors)
     * @param enable Comma-separated features to enable (e.g., "roster")
     */
    @GET("teams/{teamId}")
    suspend fun getTeamDetail(
        @Path("teamId") teamId: String,
        @Query("enable") enable: String = "roster"
    ): TeamDetailResponse

    /**
     * Get games for a specific date.
     * Example: /scoreboard?dates=20240203
     *
     * @param dates Date in YYYYMMDD format (e.g., "20240203")
     */
    @GET("scoreboard")
    suspend fun getScoreboard(
        @Query("dates") dates: String? = null
    ): ScoreboardResponse

    companion object {
        const val BASE_URL = "https://site.api.espn.com/apis/site/v2/sports/basketball/nba/"
    }
}
