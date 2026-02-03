package trif.novica.spoilerchecker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import trif.novica.spoilerchecker.data.model.Game
import trif.novica.spoilerchecker.data.repository.SpoilerRepository

data class RecentGamesUiState(
    val games: List<Game> = emptyList(),
    val allGames: List<Game> = emptyList(),
    val selectedSport: String? = null,
    val availableSports: List<String> = emptyList()
)

class RecentGamesViewModel(
    private val repository: SpoilerRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecentGamesUiState())
    val uiState: StateFlow<RecentGamesUiState> = _uiState.asStateFlow()

    init {
        loadGames()
    }

    private fun loadGames() {
        val allGames = repository.getMockGames()
        val sports = allGames.map { it.sport }.distinct().sorted()

        _uiState.value = RecentGamesUiState(
            games = allGames,
            allGames = allGames,
            availableSports = sports
        )
    }

    fun filterBySport(sport: String?) {
        _uiState.update { state ->
            val filtered = if (sport == null) {
                state.allGames
            } else {
                state.allGames.filter { it.sport == sport }
            }
            state.copy(
                games = filtered,
                selectedSport = sport
            )
        }
    }

    class Factory(
        private val repository: SpoilerRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return RecentGamesViewModel(repository) as T
        }
    }
}
