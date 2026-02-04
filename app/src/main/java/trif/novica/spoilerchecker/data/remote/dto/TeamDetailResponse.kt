package trif.novica.spoilerchecker.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Response from /teams/{id}?enable=roster endpoint
 * Example: https://site.api.espn.com/apis/site/v2/sports/basketball/nba/teams/9?enable=roster
 */
data class TeamDetailResponse(
    @SerializedName("team") val team: TeamDetailDto
)

data class TeamDetailDto(
    @SerializedName("id") val id: String,
    @SerializedName("abbreviation") val abbreviation: String,
    @SerializedName("displayName") val displayName: String,
    @SerializedName("shortDisplayName") val shortDisplayName: String,
    @SerializedName("location") val location: String,
    @SerializedName("logos") val logos: List<LogoDto>?,
    @SerializedName("athletes") val athletes: List<AthleteDto>?
)

data class AthleteDto(
    @SerializedName("id") val id: String,
    @SerializedName("fullName") val fullName: String,
    @SerializedName("displayName") val displayName: String?,      // Same as fullName usually
    @SerializedName("shortName") val shortName: String?,          // "S. Curry"
    @SerializedName("jersey") val jersey: String?,
    @SerializedName("position") val position: PositionDto?
)

data class PositionDto(
    @SerializedName("abbreviation") val abbreviation: String?,    // "PG", "SF", etc.
    @SerializedName("name") val name: String?                     // "Point Guard"
)
