package trif.novica.spoilerchecker.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Response from /scoreboard endpoint
 * Example: https://site.api.espn.com/apis/site/v2/sports/basketball/nba/scoreboard?dates=20240203
 */
data class ScoreboardResponse(
    @SerializedName("events") val events: List<EventDto>?
)

data class EventDto(
    @SerializedName("id") val id: String,
    @SerializedName("date") val date: String,                        // ISO 8601 format
    @SerializedName("name") val name: String,                        // "Golden State Warriors at Philadelphia 76ers"
    @SerializedName("shortName") val shortName: String,              // "GSW @ PHI"
    @SerializedName("competitions") val competitions: List<CompetitionDto>?,
    @SerializedName("status") val status: StatusDto?
)

data class CompetitionDto(
    @SerializedName("competitors") val competitors: List<CompetitorDto>?,
    @SerializedName("venue") val venue: VenueDto?
)

data class CompetitorDto(
    @SerializedName("id") val id: String,
    @SerializedName("homeAway") val homeAway: String,                // "home" or "away"
    @SerializedName("team") val team: CompetitorTeamDto,
    @SerializedName("score") val score: String?                      // We intentionally ignore this to avoid spoilers
)

data class CompetitorTeamDto(
    @SerializedName("id") val id: String,
    @SerializedName("abbreviation") val abbreviation: String,
    @SerializedName("displayName") val displayName: String,
    @SerializedName("shortDisplayName") val shortDisplayName: String,
    @SerializedName("location") val location: String?,
    @SerializedName("logo") val logo: String?
)

data class VenueDto(
    @SerializedName("fullName") val fullName: String?,
    @SerializedName("city") val city: String?,
    @SerializedName("state") val state: String?
)

data class StatusDto(
    @SerializedName("type") val type: StatusTypeDto?
)

data class StatusTypeDto(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,                        // "STATUS_SCHEDULED", "STATUS_IN_PROGRESS", "STATUS_FINAL"
    @SerializedName("state") val state: String,                      // "pre", "in", "post"
    @SerializedName("completed") val completed: Boolean
)
