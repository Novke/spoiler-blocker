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
import trif.novica.spoilerchecker.data.model.Team
import trif.novica.spoilerchecker.data.repository.TeamRepository

data class TeamsUiState(
    val searchQuery: String = "",
    val allTeams: List<Team> = emptyList(),
    val filteredTeams: List<Team> = emptyList(),
    val favoriteTeamIds: Set<Int> = emptySet(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class TeamsViewModel(
    private val teamRepository: TeamRepository
) : ViewModel() {

    companion object {
        private const val TAG = "TeamsViewModel"
    }

    private val _searchQuery = MutableStateFlow("")
    private val _allTeams = MutableStateFlow<List<Team>>(emptyList())
    private val _isLoading = MutableStateFlow(false)
    private val _errorMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<TeamsUiState> = combine(
        _searchQuery,
        _allTeams,
        teamRepository.getFavoriteTeams(),
        _isLoading,
        _errorMessage
    ) { query, teams, favorites, loading, error ->
        val favoriteIds = favorites.map { it.id }.toSet()
        val filtered = if (query.isBlank()) {
            teams
        } else {
            teams.filter { team ->
                team.name.contains(query, ignoreCase = true) ||
                team.fullName.contains(query, ignoreCase = true) ||
                team.city.contains(query, ignoreCase = true) ||
                team.abbreviation.contains(query, ignoreCase = true)
            }
        }

        // Sort: favorites first, then alphabetically
        val sorted = filtered.sortedWith(
            compareByDescending<Team> { it.id in favoriteIds }
                .thenBy { it.fullName }
        )

        TeamsUiState(
            searchQuery = query,
            allTeams = teams,
            filteredTeams = sorted,
            favoriteTeamIds = favoriteIds,
            isLoading = loading,
            errorMessage = error
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TeamsUiState()
    )

    init {
        loadTeams()
    }

    private fun loadTeams() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                // Ensure teams are loaded from API if DB is empty
                teamRepository.ensureTeamsLoaded()

                // Collect teams from DB
                teamRepository.getTeams().collect { teams ->
                    _allTeams.value = teams
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load teams", e)
                _errorMessage.value = e.message
                _isLoading.value = false
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun toggleFavorite(teamId: Int) {
        viewModelScope.launch {
            val isFavorite = teamRepository.isFavorite(teamId)
            if (isFavorite) {
                teamRepository.removeFavorite(teamId)
            } else {
                teamRepository.addFavorite(teamId)
            }
        }
    }

    fun dismissError() {
        _errorMessage.value = null
    }

    class Factory(
        private val teamRepository: TeamRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return TeamsViewModel(teamRepository) as T
        }
    }
}
