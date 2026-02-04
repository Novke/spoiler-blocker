package trif.novica.spoilerchecker.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import trif.novica.spoilerchecker.data.model.CleaningResult
import trif.novica.spoilerchecker.data.model.Game
import trif.novica.spoilerchecker.data.model.Team
import trif.novica.spoilerchecker.data.repository.GameRepository
import trif.novica.spoilerchecker.data.repository.SpoilerRepository
import trif.novica.spoilerchecker.data.repository.TeamRepository
import trif.novica.spoilerchecker.detection.SpoilerDetector
import trif.novica.spoilerchecker.ml.EmbeddingModel
import trif.novica.spoilerchecker.service.SpoilerNotificationListenerService

data class HomeUiState(
    val yesterdaysGames: List<Game> = emptyList(),
    val favoriteTeams: List<Team> = emptyList(),
    val recentResults: List<CleaningResult> = emptyList(),
    val isLoadingGames: Boolean = false,
    val isLoadingTeams: Boolean = false,
    val cleaningGameId: String? = null,       // Which game is currently being cleaned
    val isModelReady: Boolean = false,
    val lastCleaningResult: CleaningResultMessage? = null,
    val errorMessage: String? = null
)

data class CleaningResultMessage(
    val queryText: String,
    val removedCount: Int
)

class HomeViewModel(
    private val teamRepository: TeamRepository,
    private val gameRepository: GameRepository,
    private val spoilerRepository: SpoilerRepository,
    private val spoilerDetector: SpoilerDetector,
    private val embeddingModel: EmbeddingModel
) : ViewModel() {

    companion object {
        private const val TAG = "HomeViewModel"
    }

    private val _yesterdaysGames = MutableStateFlow<List<Game>>(emptyList())
    private val _isLoadingGames = MutableStateFlow(false)
    private val _isLoadingTeams = MutableStateFlow(false)
    private val _cleaningGameId = MutableStateFlow<String?>(null)
    private val _isModelReady = MutableStateFlow(false)
    private val _lastResult = MutableStateFlow<CleaningResultMessage?>(null)
    private val _errorMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<HomeUiState> = combine(
        _yesterdaysGames,
        teamRepository.getFavoriteTeams(),
        spoilerRepository.recentCleaningResults,
        _isLoadingGames,
        _isLoadingTeams,
        _cleaningGameId,
        _isModelReady,
        _lastResult,
        _errorMessage
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        HomeUiState(
            yesterdaysGames = values[0] as List<Game>,
            favoriteTeams = values[1] as List<Team>,
            recentResults = values[2] as List<CleaningResult>,
            isLoadingGames = values[3] as Boolean,
            isLoadingTeams = values[4] as Boolean,
            cleaningGameId = values[5] as String?,
            isModelReady = values[6] as Boolean,
            lastCleaningResult = values[7] as CleaningResultMessage?,
            errorMessage = values[8] as String?
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState()
    )

    init {
        // Wait for embedding model to be ready
        viewModelScope.launch {
            while (!embeddingModel.isReady()) {
                kotlinx.coroutines.delay(500)
            }
            _isModelReady.value = true
        }

        // Ensure teams are loaded, then load games
        viewModelScope.launch {
            _isLoadingTeams.value = true
            try {
                teamRepository.ensureTeamsLoaded()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load teams", e)
            } finally {
                _isLoadingTeams.value = false
            }

            // Load yesterday's games
            loadYesterdaysGames()
        }
    }

    fun loadYesterdaysGames() {
        viewModelScope.launch {
            _isLoadingGames.value = true
            _errorMessage.value = null

            val result = gameRepository.getYesterdaysGames()
            result.onSuccess { games ->
                _yesterdaysGames.value = games
                Log.d(TAG, "Loaded ${games.size} games from yesterday")
            }.onFailure { e ->
                _errorMessage.value = "Failed to load games: ${e.message}"
                Log.e(TAG, "Failed to load games", e)
            }

            _isLoadingGames.value = false
        }
    }

    fun cleanSpoilersForGame(game: Game) {
        viewModelScope.launch {
            _cleaningGameId.value = game.id
            _errorMessage.value = null

            try {
                Log.d(TAG, "=== Starting clean for game: ${game.matchDescription} ===")

                // Refresh notifications
                SpoilerNotificationListenerService.refreshActiveNotifications()
                kotlinx.coroutines.delay(100)

                val notifications = SpoilerNotificationListenerService.activeNotifications.value

                Log.d(TAG, "Found ${notifications.size} notifications")

                if (notifications.isEmpty()) {
                    _lastResult.value = CleaningResultMessage(queryText = game.matchDescription, removedCount = 0)
                    return@launch
                }

                // Get players for both teams (for player matching)
                val players = try {
                    // Ensure rosters are loaded
                    teamRepository.refreshRosterIfNeeded(game.homeTeam.id)
                    teamRepository.refreshRosterIfNeeded(game.awayTeam.id)
                    teamRepository.getPlayersForTeams(listOf(game.homeTeam.id, game.awayTeam.id))
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to load players, continuing without", e)
                    emptyList()
                }

                Log.d(TAG, "Loaded ${players.size} players for matching")

                // Find spoilers
                val spoilers = spoilerDetector.findSpoilers(game, notifications, players)

                Log.d(TAG, "Found ${spoilers.size} spoilers")

                if (spoilers.isNotEmpty()) {
                    val keys = spoilers.map { it.notification.key }
                    SpoilerNotificationListenerService.cancelNotifications(keys)
                    spoilerRepository.recordCleaningResult(game.matchDescription, spoilers.size)
                }

                _lastResult.value = CleaningResultMessage(queryText = game.matchDescription, removedCount = spoilers.size)
            } catch (e: Exception) {
                Log.e(TAG, "Error cleaning spoilers", e)
                _errorMessage.value = e.message ?: "An error occurred"
            } finally {
                _cleaningGameId.value = null
            }
        }
    }

    fun cleanSpoilersForTeam(team: Team) {
        viewModelScope.launch {
            _errorMessage.value = null

            try {
                Log.d(TAG, "=== Starting clean for team: ${team.fullName} ===")

                // Refresh notifications
                SpoilerNotificationListenerService.refreshActiveNotifications()
                kotlinx.coroutines.delay(100)

                val notifications = SpoilerNotificationListenerService.activeNotifications.value

                if (notifications.isEmpty()) {
                    _lastResult.value = CleaningResultMessage(queryText = team.name, removedCount = 0)
                    return@launch
                }

                // Get players for the team
                val players = try {
                    teamRepository.refreshRosterIfNeeded(team.id)
                    teamRepository.getPlayersForTeam(team.id)
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to load players", e)
                    emptyList()
                }

                // Use keyword and player matching only (no game context)
                val keywords = team.getKeywords()
                val lowerKeywords = keywords.map { it.lowercase() }.toSet()

                var removedCount = 0
                val keysToRemove = mutableListOf<String>()

                for (notification in notifications) {
                    val text = notification.fullText.lowercase()

                    // Check keywords
                    val keywordMatch = lowerKeywords.any { keyword ->
                        text.contains(keyword)
                    }

                    // Check players
                    val playerMatch = players.any { player ->
                        player.getNameVariations().any { name ->
                            text.contains(name)
                        }
                    }

                    if (keywordMatch || playerMatch) {
                        keysToRemove.add(notification.key)
                        removedCount++
                    }
                }

                if (keysToRemove.isNotEmpty()) {
                    SpoilerNotificationListenerService.cancelNotifications(keysToRemove)
                    spoilerRepository.recordCleaningResult(team.name, removedCount)
                }

                _lastResult.value = CleaningResultMessage(queryText = team.name, removedCount = removedCount)
            } catch (e: Exception) {
                Log.e(TAG, "Error cleaning spoilers for team", e)
                _errorMessage.value = e.message ?: "An error occurred"
            }
        }
    }

    fun addFavoriteTeam(teamId: Int) {
        viewModelScope.launch {
            teamRepository.addFavorite(teamId)
        }
    }

    fun removeFavoriteTeam(teamId: Int) {
        viewModelScope.launch {
            teamRepository.removeFavorite(teamId)
        }
    }

    fun dismissLastResult() {
        _lastResult.value = null
    }

    fun dismissError() {
        _errorMessage.value = null
    }

    class Factory(
        private val teamRepository: TeamRepository,
        private val gameRepository: GameRepository,
        private val spoilerRepository: SpoilerRepository,
        private val spoilerDetector: SpoilerDetector,
        private val embeddingModel: EmbeddingModel
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HomeViewModel(
                teamRepository,
                gameRepository,
                spoilerRepository,
                spoilerDetector,
                embeddingModel
            ) as T
        }
    }
}
