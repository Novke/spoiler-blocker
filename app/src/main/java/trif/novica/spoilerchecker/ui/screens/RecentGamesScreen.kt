package trif.novica.spoilerchecker.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import trif.novica.spoilerchecker.data.model.Game
import trif.novica.spoilerchecker.ui.components.GameCard
import trif.novica.spoilerchecker.ui.viewmodel.RecentGamesUiState

@Composable
fun RecentGamesScreen(
    uiState: RecentGamesUiState,
    onFilterSport: (String?) -> Unit,
    onGameClick: (Game) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Spacer(modifier = Modifier.height(8.dp)) }

        item {
            Text(
                text = "Upcoming Games",
                style = MaterialTheme.typography.headlineSmall
            )
        }

        item {
            Text(
                text = "Tap a game to quickly clean related notifications",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Sport Filter
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    FilterChip(
                        selected = uiState.selectedSport == null,
                        onClick = { onFilterSport(null) },
                        label = { Text("All") }
                    )
                }

                items(uiState.availableSports) { sport ->
                    FilterChip(
                        selected = sport == uiState.selectedSport,
                        onClick = { onFilterSport(sport) },
                        label = { Text(sport) }
                    )
                }
            }
        }

        items(uiState.games) { game ->
            GameCard(
                game = game,
                onClick = { onGameClick(game) }
            )
        }

        if (uiState.games.isEmpty()) {
            item {
                Text(
                    text = "No games found",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 32.dp)
                )
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}
