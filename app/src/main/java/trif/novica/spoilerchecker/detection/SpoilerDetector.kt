package trif.novica.spoilerchecker.detection

import android.util.Log
import trif.novica.spoilerchecker.data.model.Game
import trif.novica.spoilerchecker.data.model.NotificationInfo
import trif.novica.spoilerchecker.data.model.Player

/**
 * Main spoiler detection orchestrator.
 *
 * Uses a 3-level detection hierarchy:
 * 1. Keyword matching (fastest, most accurate for known terms)
 * 2. Player matching (catches player-specific notifications)
 * 3. Semantic matching (fallback using ML model)
 */
class SpoilerDetector(
    private val keywordMatcher: KeywordMatcher,
    private val playerMatcher: PlayerMatcher,
    private val semanticMatcher: SemanticMatcher
) {
    companion object {
        private const val TAG = "SpoilerDetector"
        const val SEMANTIC_THRESHOLD = 0.3f
    }

    /**
     * Type of match that was found.
     */
    enum class MatchType {
        KEYWORD,   // Matched team name, city, or abbreviation
        PLAYER,    // Matched player name
        SEMANTIC   // Matched via semantic similarity
    }

    /**
     * Result of spoiler detection for a single notification.
     */
    data class ScoredNotification(
        val notification: NotificationInfo,
        val score: Float,           // 1.0 for keyword/player, actual score for semantic
        val matchType: MatchType
    )

    /**
     * Find all notifications that are spoilers for the given game.
     *
     * @param game The game to check for spoilers
     * @param notifications List of active notifications
     * @param players List of players from both teams (for player matching)
     * @return List of notifications that are spoilers, sorted by score descending
     */
    suspend fun findSpoilers(
        game: Game,
        notifications: List<NotificationInfo>,
        players: List<Player> = emptyList()
    ): List<ScoredNotification> {
        Log.d(TAG, "findSpoilers for ${game.matchDescription} with ${notifications.size} notifications")

        if (notifications.isEmpty()) {
            return emptyList()
        }

        val keywords = keywordMatcher.buildKeywords(game.homeTeam, game.awayTeam)
        Log.d(TAG, "Keywords: $keywords")

        val results = mutableListOf<ScoredNotification>()

        for (notification in notifications) {
            if (notification.fullText.isBlank()) continue

            val text = notification.fullText.lowercase()

            // Level 1: Keyword match
            if (keywordMatcher.matches(text, keywords)) {
                Log.d(TAG, "KEYWORD match: ${notification.fullText.take(50)}")
                results.add(ScoredNotification(notification, 1.0f, MatchType.KEYWORD))
                continue
            }

            // Level 2: Player match (only if we have players)
            if (players.isNotEmpty() && playerMatcher.matches(text, players)) {
                Log.d(TAG, "PLAYER match: ${notification.fullText.take(50)}")
                results.add(ScoredNotification(notification, 0.9f, MatchType.PLAYER))
                continue
            }

            // Level 3: Semantic match (fallback)
            if (semanticMatcher.isReady()) {
                val score = semanticMatcher.score(game.matchDescription, notification.fullText)
                Log.d(TAG, "SEMANTIC score ${"%.3f".format(score)}: ${notification.fullText.take(50)}")

                if (score >= SEMANTIC_THRESHOLD) {
                    results.add(ScoredNotification(notification, score, MatchType.SEMANTIC))
                }
            }
        }

        Log.d(TAG, "Found ${results.size} spoilers")
        return results.sortedByDescending { it.score }
    }

    /**
     * Simple check if any notification is a spoiler for the game.
     * Useful for quick checking without full details.
     */
    suspend fun hasSpoilers(
        game: Game,
        notifications: List<NotificationInfo>,
        players: List<Player> = emptyList()
    ): Boolean {
        return findSpoilers(game, notifications, players).isNotEmpty()
    }
}
