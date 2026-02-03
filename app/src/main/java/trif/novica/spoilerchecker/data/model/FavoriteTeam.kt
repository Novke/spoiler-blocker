package trif.novica.spoilerchecker.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorite_teams")
data class FavoriteTeam(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val addedAt: Long = System.currentTimeMillis()
)
