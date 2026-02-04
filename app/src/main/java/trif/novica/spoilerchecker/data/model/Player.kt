package trif.novica.spoilerchecker.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "players",
    foreignKeys = [
        ForeignKey(
            entity = Team::class,
            parentColumns = ["id"],
            childColumns = ["teamId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("teamId")]
)
data class Player(
    @PrimaryKey val id: Int,                     // ESPN player ID
    val teamId: Int,                              // Foreign key to Team
    val fullName: String,                         // "Stephen Curry"
    val shortName: String?,                       // "S. Curry"
    val jersey: String? = null,                   // "30"
    val position: String? = null                  // "PG"
) {
    /**
     * Get searchable name variations for this player.
     */
    fun getNameVariations(): Set<String> {
        val variations = mutableSetOf(fullName.lowercase())

        shortName?.let { variations.add(it.lowercase()) }

        // Add last name only (e.g., "Curry")
        val parts = fullName.split(" ")
        if (parts.size > 1) {
            variations.add(parts.last().lowercase())
        }

        return variations
    }
}
