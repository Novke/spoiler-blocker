package trif.novica.spoilerchecker.ui.viewmodel

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
import trif.novica.spoilerchecker.data.model.FavoriteTeam
import trif.novica.spoilerchecker.data.repository.SpoilerRepository
import trif.novica.spoilerchecker.ml.EmbeddingModel
import trif.novica.spoilerchecker.ml.SpoilerDetector
import trif.novica.spoilerchecker.service.SpoilerNotificationListenerService

data class HomeUiState(
    val queryText: String = "",
    val favoriteTeams: List<FavoriteTeam> = emptyList(),
    val recentResults: List<CleaningResult> = emptyList(),
    val isLoading: Boolean = false,
    val isModelReady: Boolean = false,
    val lastCleaningResult: CleaningResultMessage? = null,
    val errorMessage: String? = null
)

data class CleaningResultMessage(
    val queryText: String,
    val removedCount: Int
)

class HomeViewModel(
    private val repository: SpoilerRepository,
    private val spoilerDetector: SpoilerDetector,
    private val embeddingModel: EmbeddingModel
) : ViewModel() {

    private val _queryText = MutableStateFlow("")
    private val _isLoading = MutableStateFlow(false)
    private val _isModelReady = MutableStateFlow(false)
    private val _lastResult = MutableStateFlow<CleaningResultMessage?>(null)
    private val _errorMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<HomeUiState> = combine(
        _queryText,
        repository.favoriteTeams,
        repository.recentCleaningResults,
        _isLoading,
        _isModelReady,
        _lastResult,
        _errorMessage
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        HomeUiState(
            queryText = values[0] as String,
            favoriteTeams = values[1] as List<FavoriteTeam>,
            recentResults = values[2] as List<CleaningResult>,
            isLoading = values[3] as Boolean,
            isModelReady = values[4] as Boolean,
            lastCleaningResult = values[5] as CleaningResultMessage?,
            errorMessage = values[6] as String?
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState()
    )

    init {
        viewModelScope.launch {
            while (!embeddingModel.isReady()) {
                kotlinx.coroutines.delay(500)
            }
            _isModelReady.value = true
        }
    }

    fun onQueryChanged(text: String) {
        _queryText.value = text
    }

    fun cleanSpoilers() {
        val query = _queryText.value.trim()
        if (query.isBlank()) return

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                SpoilerNotificationListenerService.refreshActiveNotifications()
                kotlinx.coroutines.delay(100)

                val notifications = SpoilerNotificationListenerService.activeNotifications.value

                if (notifications.isEmpty()) {
                    _lastResult.value = CleaningResultMessage(query, 0)
                    _queryText.value = ""
                    return@launch
                }

                val spoilerNotifications = spoilerDetector.findSpoilerNotifications(
                    query = query,
                    notifications = notifications
                )

                if (spoilerNotifications.isNotEmpty()) {
                    val keys = spoilerNotifications.map { it.notification.key }
                    SpoilerNotificationListenerService.cancelNotifications(keys)
                    repository.recordCleaningResult(query, spoilerNotifications.size)
                }

                _lastResult.value = CleaningResultMessage(query, spoilerNotifications.size)
                _queryText.value = ""
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "An error occurred"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun addFavoriteTeam(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.addFavoriteTeam(name.trim())
        }
    }

    fun removeFavoriteTeam(team: FavoriteTeam) {
        viewModelScope.launch {
            repository.removeFavoriteTeam(team)
        }
    }

    fun useTeamAsQuery(team: FavoriteTeam) {
        _queryText.value = team.name
    }

    fun dismissLastResult() {
        _lastResult.value = null
    }

    fun dismissError() {
        _errorMessage.value = null
    }

    class Factory(
        private val repository: SpoilerRepository,
        private val spoilerDetector: SpoilerDetector,
        private val embeddingModel: EmbeddingModel
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HomeViewModel(repository, spoilerDetector, embeddingModel) as T
        }
    }
}
