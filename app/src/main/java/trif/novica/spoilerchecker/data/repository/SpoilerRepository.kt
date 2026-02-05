package trif.novica.spoilerchecker.data.repository

import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import trif.novica.spoilerchecker.data.local.CleaningResultDao
import trif.novica.spoilerchecker.data.model.CleaningResult
import trif.novica.spoilerchecker.data.model.RemovedNotification

/**
 * Repository for cleaning history only.
 * Teams are handled by TeamRepository, games by GameRepository.
 */
class SpoilerRepository(
    private val cleaningResultDao: CleaningResultDao
) {
    private val gson = Gson()

    val recentCleaningResults: Flow<List<CleaningResult>> = cleaningResultDao.getRecentResults()

    suspend fun recordCleaningResult(
        matchDescription: String,
        removedNotifications: List<RemovedNotification>
    ) {
        val json = if (removedNotifications.isNotEmpty()) {
            gson.toJson(removedNotifications)
        } else {
            null
        }

        cleaningResultDao.insert(
            CleaningResult(
                queryText = matchDescription,
                notificationsRemoved = removedNotifications.size,
                removedNotificationsJson = json
            )
        )
    }

    fun parseRemovedNotifications(result: CleaningResult): List<RemovedNotification> {
        if (result.removedNotificationsJson.isNullOrBlank()) return emptyList()
        return try {
            gson.fromJson(result.removedNotificationsJson, Array<RemovedNotification>::class.java).toList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getCleaningHistory(limit: Int = 10): List<CleaningResult> {
        return cleaningResultDao.getRecentResultsOnce(limit)
    }
}
