package trif.novica.spoilerchecker.data.repository

import kotlinx.coroutines.flow.Flow
import trif.novica.spoilerchecker.data.local.CleaningResultDao
import trif.novica.spoilerchecker.data.local.FavoriteTeamDao
import trif.novica.spoilerchecker.data.model.CleaningResult
import trif.novica.spoilerchecker.data.model.FavoriteTeam
import trif.novica.spoilerchecker.data.model.Game

class SpoilerRepository(
    private val favoriteTeamDao: FavoriteTeamDao,
    private val cleaningResultDao: CleaningResultDao
) {
    val favoriteTeams: Flow<List<FavoriteTeam>> = favoriteTeamDao.getAllTeams()
    val recentCleaningResults: Flow<List<CleaningResult>> = cleaningResultDao.getRecentResults()

    suspend fun addFavoriteTeam(name: String): Boolean {
        val exists = favoriteTeamDao.countByName(name) > 0
        if (!exists) {
            favoriteTeamDao.insert(FavoriteTeam(name = name))
            return true
        }
        return false
    }

    suspend fun removeFavoriteTeam(team: FavoriteTeam) {
        favoriteTeamDao.delete(team)
    }

    suspend fun recordCleaningResult(queryText: String, removedCount: Int) {
        cleaningResultDao.insert(
            CleaningResult(queryText = queryText, notificationsRemoved = removedCount)
        )
    }

    fun getMockGames(): List<Game> {
        val now = System.currentTimeMillis()
        return listOf(
            Game(
                id = "1",
                homeTeam = "Lakers",
                awayTeam = "Celtics",
                sport = "Basketball",
                league = "NBA",
                scheduledTime = now + 3600000,
                isLive = false
            ),
            Game(
                id = "2",
                homeTeam = "Real Madrid",
                awayTeam = "Barcelona",
                sport = "Football",
                league = "La Liga",
                scheduledTime = now - 1800000,
                isLive = true
            ),
            Game(
                id = "3",
                homeTeam = "Man United",
                awayTeam = "Liverpool",
                sport = "Football",
                league = "Premier League",
                scheduledTime = now + 7200000,
                isLive = false
            ),
            Game(
                id = "4",
                homeTeam = "Warriors",
                awayTeam = "Nets",
                sport = "Basketball",
                league = "NBA",
                scheduledTime = now + 86400000,
                isLive = false
            ),
            Game(
                id = "5",
                homeTeam = "PSG",
                awayTeam = "Bayern Munich",
                sport = "Football",
                league = "Champions League",
                scheduledTime = now + 172800000,
                isLive = false
            ),
            Game(
                id = "6",
                homeTeam = "Denver Nuggets",
                awayTeam = "Miami Heat",
                sport = "Basketball",
                league = "NBA",
                scheduledTime = now + 259200000,
                isLive = false
            )
        )
    }
}
