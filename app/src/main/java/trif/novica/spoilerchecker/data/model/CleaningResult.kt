package trif.novica.spoilerchecker.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cleaning_history")
data class CleaningResult(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val queryText: String,
    val notificationsRemoved: Int,
    val timestamp: Long = System.currentTimeMillis()
)
