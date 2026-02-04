package trif.novica.spoilerchecker.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "teams")
data class Team(
    @PrimaryKey val id: Int,                    // ESPN team ID
    val name: String,                            // "Warriors"
    val fullName: String,                        // "Golden State Warriors"
    val abbreviation: String,                    // "GSW"
    val city: String,                            // "Golden State"
    val league: String = "NBA",                  // For future expansion (Euroleague, etc.)
    val logoUrl: String? = null,
    val lastRosterUpdate: Long? = null           // Timestamp of last roster fetch
) {
    /**
     * All searchable keywords for this team (lowercase).
     * Used for keyword matching against notifications.
     */
    fun getKeywords(): Set<String> {
        return setOf(
            name,
            fullName,
            abbreviation,
            city
        ).map { it.lowercase() }.toSet()
    }

    companion object {
        // Alternative names mapping (manual, since ESPN doesn't provide these)
        val ALTERNATIVE_NAMES = mapOf(
            "76ers" to listOf("Sixers", "Philly"),
            "Trail Blazers" to listOf("Blazers"),
            "Timberwolves" to listOf("T-Wolves", "Wolves")
        )
    }
}
