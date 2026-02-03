package trif.novica.spoilerchecker.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import trif.novica.spoilerchecker.data.model.CleaningResult

@Dao
interface CleaningResultDao {
    @Query("SELECT * FROM cleaning_history ORDER BY timestamp DESC LIMIT 20")
    fun getRecentResults(): Flow<List<CleaningResult>>

    @Insert
    suspend fun insert(result: CleaningResult): Long

    @Query("DELETE FROM cleaning_history WHERE timestamp < :beforeTimestamp")
    suspend fun deleteOlderThan(beforeTimestamp: Long)
}
