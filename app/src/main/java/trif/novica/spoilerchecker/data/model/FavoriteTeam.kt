package trif.novica.spoilerchecker.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "favorite_teams",
    foreignKeys = [
        ForeignKey(
            entity = Team::class,
            parentColumns = ["id"],
            childColumns = ["teamId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class FavoriteTeam(
    @PrimaryKey val teamId: Int,                  // ESPN team ID (foreign key to Team)
    val addedAt: Long = System.currentTimeMillis()
)
