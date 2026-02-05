package trif.novica.spoilerchecker.ui.screens

import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import coil.compose.AsyncImage
import androidx.lifecycle.repeatOnLifecycle
import trif.novica.spoilerchecker.data.model.Game
import trif.novica.spoilerchecker.data.model.Team
import trif.novica.spoilerchecker.service.SpoilerNotificationListenerService
import trif.novica.spoilerchecker.ui.components.CleaningResultCard
import trif.novica.spoilerchecker.ui.viewmodel.HomeUiState
import trif.novica.spoilerchecker.ui.viewmodel.TimeFilter
import trif.novica.spoilerchecker.data.model.CleaningResult
import trif.novica.spoilerchecker.data.model.RemovedNotification
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    uiState: HomeUiState,
    toastEvents: Flow<String>,
    onCleanGame: (Game) -> Unit,
    onCleanTeam: (Team) -> Unit,
    onRemoveFavorite: (Int) -> Unit,
    onTimeFilterChanged: (TimeFilter) -> Unit,
    onRefreshGames: () -> Unit,
    onDismissResult: () -> Unit,
    onDismissError: () -> Unit,
    onNavigateToTeams: () -> Unit,
    onGetRemovedNotifications: (CleaningResult) -> List<RemovedNotification>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasNotificationPermission by remember {
        mutableStateOf(SpoilerNotificationListenerService.isPermissionGranted(context))
    }

    // Toast handling
    LaunchedEffect(Unit) {
        toastEvents.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    // Dialog state for showing removed notifications
    var showDetailsDialog by remember { mutableStateOf<CleaningResult?>(null) }
    var confirmShown by remember { mutableStateOf(false) }

    // Re-check permission when returning to app
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            hasNotificationPermission = SpoilerNotificationListenerService.isPermissionGranted(context)
        }
    }

    // Confirm dialog before showing spoilers
    if (showDetailsDialog != null && !confirmShown) {
        AlertDialog(
            onDismissRequest = { showDetailsDialog = null },
            title = { Text("Warning") },
            text = { Text("This will show the content of removed notifications which may contain spoilers. Are you sure?") },
            confirmButton = {
                TextButton(onClick = { confirmShown = true }) {
                    Text("Show Details")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDetailsDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Details dialog showing removed notifications
    if (showDetailsDialog != null && confirmShown) {
        val result = showDetailsDialog!!
        val notifications = onGetRemovedNotifications(result)

        AlertDialog(
            onDismissRequest = {
                showDetailsDialog = null
                confirmShown = false
            },
            title = { Text("Removed Notifications") },
            text = {
                if (notifications.isEmpty()) {
                    Text("No details available for this cleaning.")
                } else {
                    LazyColumn {
                        items(notifications.size) { index ->
                            val notif = notifications[index]
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Text(
                                    text = notif.title,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = notif.text,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = notif.packageName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showDetailsDialog = null
                    confirmShown = false
                }) {
                    Text("Close")
                }
            }
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Spacer(modifier = Modifier.height(8.dp)) }

        // Header
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Filled.Shield,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Spoiler Shield",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Text(
                        text = "Tap Clean to remove game spoilers",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Permission Required Card
        if (!hasNotificationPermission) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.NotificationsOff,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Permission Required",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Text(
                                    text = "Enable notification access to detect spoilers",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                context.startActivity(
                                    Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Enable Notification Access")
                        }
                    }
                }
            }
        }

        // Result Card
        item {
            CleaningResultCard(
                result = uiState.lastCleaningResult,
                onDismiss = onDismissResult
            )
        }

        // Error Card
        item {
            AnimatedVisibility(visible = uiState.errorMessage != null) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = uiState.errorMessage ?: "",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = onDismissError) {
                            Icon(Icons.Filled.Close, contentDescription = "Dismiss")
                        }
                    }
                }
            }
        }

        // Recent Games Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Games",
                    style = MaterialTheme.typography.titleMedium
                )
                IconButton(
                    onClick = onRefreshGames,
                    enabled = !uiState.isLoadingGames
                ) {
                    if (uiState.isLoadingGames) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
                    }
                }
            }
        }

        // Time Filter Chips
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TimeFilter.entries.forEach { filter ->
                    FilterChip(
                        selected = uiState.selectedTimeFilter == filter,
                        onClick = { onTimeFilterChanged(filter) },
                        label = { Text(filter.label) }
                    )
                }
            }
        }

        // Games List
        if (uiState.isLoadingGames && uiState.recentGames.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        } else if (uiState.recentGames.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No games found",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            val favoriteTeamIds = uiState.favoriteTeams.map { it.id }.toSet()

            items(uiState.recentGames) { game ->
                GameCleanCard(
                    game = game,
                    isLoading = uiState.cleaningGameId == game.id,
                    enabled = hasNotificationPermission && uiState.cleaningGameId == null,
                    isHomeFavorite = game.homeTeam.id in favoriteTeamIds,
                    isAwayFavorite = game.awayTeam.id in favoriteTeamIds,
                    onClean = { onCleanGame(game) }
                )
            }
        }

        // Favorite Teams Section
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Favorite Teams",
                    style = MaterialTheme.typography.titleMedium
                )
                FilledTonalButton(onClick = onNavigateToTeams) {
                    Text("Browse Teams")
                }
            }
        }

        item {
            if (uiState.favoriteTeams.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Add favorite teams for quick access",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    uiState.favoriteTeams.forEach { team ->
                        InputChip(
                            selected = false,
                            onClick = { onCleanTeam(team) },
                            label = { Text(team.name) },
                            trailingIcon = {
                                IconButton(
                                    onClick = { onRemoveFavorite(team.id) },
                                    modifier = Modifier.size(18.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.Close,
                                        contentDescription = "Remove",
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }

        // Recent Activity
        if (uiState.recentResults.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Recent Activity",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            items(uiState.recentResults.take(5)) { result ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = result.notificationsRemoved > 0) {
                            showDetailsDialog = result
                            confirmShown = false
                        },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = result.queryText,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = formatTimestamp(result.timestamp),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${result.notificationsRemoved} cleaned",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            if (result.notificationsRemoved > 0) {
                                Icon(
                                    imageVector = Icons.Filled.ChevronRight,
                                    contentDescription = "View details",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun GameCleanCard(
    game: Game,
    isLoading: Boolean,
    enabled: Boolean,
    isHomeFavorite: Boolean,
    isAwayFavorite: Boolean,
    onClean: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isFavoriteGame = isHomeFavorite || isAwayFavorite

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isFavoriteGame) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            } else {
                MaterialTheme.colorScheme.surfaceContainerLow
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Away team logo with star
                TeamLogoWithStar(
                    logoUrl = game.awayTeam.logoUrl,
                    teamName = game.awayTeam.name,
                    size = 32,
                    isFavorite = isAwayFavorite
                )

                Text(
                    text = "@",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 6.dp)
                )

                // Home team logo with star
                TeamLogoWithStar(
                    logoUrl = game.homeTeam.logoUrl,
                    teamName = game.homeTeam.name,
                    size = 32,
                    isFavorite = isHomeFavorite
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = "${game.awayTeam.name} @ ${game.homeTeam.name}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = formatGameTime(game.scheduledTime),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Button(
                onClick = onClean,
                enabled = enabled && !isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Clean")
                }
            }
        }
    }
}

@Composable
private fun TeamLogoWithStar(
    logoUrl: String?,
    teamName: String,
    size: Int,
    isFavorite: Boolean,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        if (logoUrl != null) {
            AsyncImage(
                model = logoUrl,
                contentDescription = "$teamName logo",
                modifier = Modifier
                    .size(size.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Fit
            )
        } else {
            Box(
                modifier = Modifier
                    .size(size.dp)
                    .clip(CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = teamName.take(3).uppercase(),
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }

        // Star indicator for favorites
        if (isFavorite) {
            Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = "Favorite",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(14.dp)
                    .align(Alignment.TopEnd)
            )
        }
    }
}

private fun formatGameTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("HH:mm dd.MM", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

private fun formatTimestamp(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    return when {
        diff < 60000 -> "Just now"
        diff < 3600000 -> "${diff / 60000}m ago"
        diff < 86400000 -> "${diff / 3600000}h ago"
        else -> {
            val sdf = SimpleDateFormat("MMM d", Locale.getDefault())
            sdf.format(Date(timestamp))
        }
    }
}
