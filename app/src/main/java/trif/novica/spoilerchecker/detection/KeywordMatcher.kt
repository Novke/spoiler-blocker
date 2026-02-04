package trif.novica.spoilerchecker.detection

import trif.novica.spoilerchecker.data.model.Team

/**
 * Level 1 detection: Simple keyword matching.
 * Checks if notification text contains team name, city, or abbreviation.
 *
 * This is the fastest and most accurate detection method for known teams.
 */
class KeywordMatcher {

    /**
     * Check if text contains any of the keywords.
     *
     * @param text Notification text (already lowercase)
     * @param keywords Set of keywords to match against (already lowercase)
     * @return true if any keyword is found in text
     */
    fun matches(text: String, keywords: Set<String>): Boolean {
        val lowerText = text.lowercase()
        return keywords.any { keyword ->
            // Use word boundary matching to avoid false positives
            // e.g., "Magic" should match "Magic" but not "Magical"
            containsWord(lowerText, keyword)
        }
    }

    /**
     * Build keywords set from two teams.
     * Includes: team name, full name, abbreviation, city, and alternative names.
     */
    fun buildKeywords(homeTeam: Team, awayTeam: Team): Set<String> {
        val keywords = mutableSetOf<String>()

        listOf(homeTeam, awayTeam).forEach { team ->
            keywords.add(team.name.lowercase())
            keywords.add(team.fullName.lowercase())
            keywords.add(team.abbreviation.lowercase())
            keywords.add(team.city.lowercase())

            // Add alternative names if available
            Team.ALTERNATIVE_NAMES[team.name]?.forEach { alt ->
                keywords.add(alt.lowercase())
            }
        }

        return keywords
    }

    /**
     * Check if text contains keyword as a word (not substring).
     * Uses simple boundary checking.
     */
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
