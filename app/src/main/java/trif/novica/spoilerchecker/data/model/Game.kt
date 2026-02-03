package trif.novica.spoilerchecker.data.model

data class Game(
    val id: String,
    val homeTeam: String,
    val awayTeam: String,
    val sport: String,
    val league: String,
    val scheduledTime: Long,
    val isLive: Boolean = false
) {
    val matchDescription: String
        get() = "$homeTeam vs $awayTeam"
}
