package trif.novica.spoilerchecker.detection

import trif.novica.spoilerchecker.data.model.Player

/**
 * Level 2 detection: Player name matching.
 * Checks if notification text contains any player name from the game rosters.
 *
 * This catches notifications like "LeBron scores 40 points" or "Curry with the game-winner".
 */
class PlayerMatcher {

    /**
     * Check if text contains any player name.
     *
     * @param text Notification text
     * @param players List of players from both teams
     * @return true if any player name is found
     */
    fun matches(text: String, players: List<Player>): Boolean {
        val lowerText = text.lowercase()

        return players.any { player ->
            player.getNameVariations().any { variation ->
                containsName(lowerText, variation)
            }
        }
    }

    /**
     * Get all matched player names from text.
     */
    fun findMatchedPlayers(text: String, players: List<Player>): List<Player> {
        val lowerText = text.lowercase()

        return players.filter { player ->
            player.getNameVariations().any { variation ->
                containsName(lowerText, variation)
            }
        }
    }

    /**
     * Build a set of all player name variations.
     */
    fun buildPlayerKeywords(players: List<Player>): Set<String> {
        return players.flatMap { it.getNameVariations() }.toSet()
    }

    /**
     * Check if text contains name.
     * For last names (single word), uses word boundary.
     * For full names, uses simple contains (to handle "Stephen Curry" and "Steph Curry").
     */
    private fun containsName(text: String, name: String): Boolean {
        if (name.isEmpty()) return false

        // For single-word names (last names), use word boundary
        if (!name.contains(" ")) {
            return containsWord(text, name)
        }

        // For multi-word names, use simple contains
        return text.contains(name)
    }

    private fun containsWord(text: String, word: String): Boolean {
        if (word.isEmpty()) return false

        var startIndex = 0
        while (true) {
            val index = text.indexOf(word, startIndex)
            if (index < 0) return false

            val beforeOk = index == 0 || !text[index - 1].isLetterOrDigit()
            val afterOk = index + word.length >= text.length ||
                    !text[index + word.length].isLetterOrDigit()

            if (beforeOk && afterOk) return true

            startIndex = index + 1
        }
    }
}
