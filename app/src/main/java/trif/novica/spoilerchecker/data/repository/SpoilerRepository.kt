package trif.novica.spoilerchecker.data.repository

import kotlinx.coroutines.flow.Flow
import trif.novica.spoilerchecker.data.local.CleaningResultDao
import trif.novica.spoilerchecker.data.model.CleaningResult

/**
 * Repository for cleaning history only.
 * Teams are handled by TeamRepository, games by GameRepository.
 */
class SpoilerRepository(
    private val cleaningResultDao: CleaningResultDao
) {
    val recentCleaningResults: Flow<List<CleaningResult>> = cleaningResultDao.getRecentResults()

    suspend fun recordCleaningResult(matchDescription: String, removedCount: Int) {
        cleaningResultDao.insert(
            CleaningResult(queryText = matchDescription, notificationsRemoved = removedCount)
        )
    }

    suspend fun getCleaningHistory(limit: Int = 10): List<CleaningResult> {
        return cleaningResultDao.getRecentResultsOnce(limit)
    }
}
