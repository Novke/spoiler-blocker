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

enum class TimeFilter(val label: String, val hours: Int) {
    HOURS_12("12h", 12),
    DAYS_1("1d", 24),
    DAYS_2("2d", 48)
}

data class HomeUiState(
    val recentGames: List<Game> = emptyList(),
    val selectedTimeFilter: TimeFilter = TimeFilter.DAYS_1,
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

    private val _allGames = MutableStateFlow<List<Game>>(emptyList())  // All games (2d), cached
    private val _selectedTimeFilter = MutableStateFlow(TimeFilter.DAYS_1)
    private val _isLoadingGames = MutableStateFlow(false)
    private val _isLoadingTeams = MutableStateFlow(false)
    private val _cleaningGameId = MutableStateFlow<String?>(null)
    private val _isModelReady = MutableStateFlow(false)
    private val _lastResult = MutableStateFlow<CleaningResultMessage?>(null)
    private val _errorMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<HomeUiState> = combine(
        _allGames,
        _selectedTimeFilter,
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
        val allGames = values[0] as List<Game>
        val filter = values[1] as TimeFilter

        // Filter games in-memory based on selected time filter
        val cutoffTime = System.currentTimeMillis() - (filter.hours * 60 * 60 * 1000L)
        val filteredGames = allGames.filter { it.scheduledTime >= cutoffTime }

        HomeUiState(
            recentGames = filteredGames,
            selectedTimeFilter = filter,
            favoriteTeams = values[2] as List<Team>,
            recentResults = values[3] as List<CleaningResult>,
            isLoadingGames = values[4] as Boolean,
            isLoadingTeams = values[5] as Boolean,
            cleaningGameId = values[6] as String?,
            isModelReady = values[7] as Boolean,
            lastCleaningResult = values[8] as CleaningResultMessage?,
            errorMessage = values[9] as String?
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

            // Load recent games
            loadRecentGames()
        }
    }

    fun setTimeFilter(filter: TimeFilter) {
        _selectedTimeFilter.value = filter
        // No API call - filtering is done in-memory via combine
    }

    fun loadRecentGames() {
        viewModelScope.launch {
            _isLoadingGames.value = true
            _errorMessage.value = null

            // Always fetch max (2 days = 48h), filter in-memory
            val result = gameRepository.getRecentGames(TimeFilter.DAYS_2.hours)

            result.onSuccess { games ->
                _allGames.value = games
                Log.d(TAG, "Loaded ${games.size} games (cached for 48h)")
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
