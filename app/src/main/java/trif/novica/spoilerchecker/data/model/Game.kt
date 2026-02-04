package trif.novica.spoilerchecker.data.model

enum class GameStatus {
    SCHEDULED,
    LIVE,
    FINISHED
}

data class Game(
    val id: String,
    val homeTeam: Team,
    val awayTeam: Team,
    val scheduledTime: Long,
    val status: GameStatus = GameStatus.SCHEDULED,
    val league: String = "NBA"
) {
    /**
     * Human-readable match description (e.g., "Warriors vs 76ers")
     */
    val matchDescription: String
        get() = "${homeTeam.name} vs ${awayTeam.name}"

    /**
     * Short format using abbreviations (e.g., "GSW vs PHI")
     */
    val shortDescription: String
        get() = "${homeTeam.abbreviation} vs ${awayTeam.abbreviation}"

    /**
     * Get all keywords for both teams in this game.
     * Used for notification matching.
     */
    fun getAllKeywords(): Set<String> {
        return homeTeam.getKeywords() + awayTeam.getKeywords()
    }
}
