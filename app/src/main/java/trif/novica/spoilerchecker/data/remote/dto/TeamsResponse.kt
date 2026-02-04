package trif.novica.spoilerchecker.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Response from /teams endpoint
 * Example: https://site.api.espn.com/apis/site/v2/sports/basketball/nba/teams
 */
data class TeamsResponse(
    @SerializedName("sports") val sports: List<SportDto>?
)

data class SportDto(
    @SerializedName("leagues") val leagues: List<LeagueDto>?
)

data class LeagueDto(
    @SerializedName("teams") val teams: List<TeamWrapperDto>?
)

data class TeamWrapperDto(
    @SerializedName("team") val team: TeamDto
)

data class TeamDto(
    @SerializedName("id") val id: String,
    @SerializedName("abbreviation") val abbreviation: String,
    @SerializedName("displayName") val displayName: String,         // "Golden State Warriors"
    @SerializedName("shortDisplayName") val shortDisplayName: String, // "Warriors"
    @SerializedName("name") val name: String?,                       // "Warriors"
    @SerializedName("location") val location: String,                // "Golden State"
    @SerializedName("color") val color: String?,
    @SerializedName("alternateColor") val alternateColor: String?,
    @SerializedName("logos") val logos: List<LogoDto>?
)

data class LogoDto(
    @SerializedName("href") val href: String,
    @SerializedName("width") val width: Int?,
    @SerializedName("height") val height: Int?
)
