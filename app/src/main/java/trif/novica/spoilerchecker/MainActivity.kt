package trif.novica.spoilerchecker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import trif.novica.spoilerchecker.ui.navigation.Screen
import trif.novica.spoilerchecker.ui.navigation.bottomNavItems
import trif.novica.spoilerchecker.ui.screens.HomeScreen
import trif.novica.spoilerchecker.ui.screens.RecentGamesScreen
import trif.novica.spoilerchecker.ui.screens.SettingsScreen
import trif.novica.spoilerchecker.ui.theme.SpoilerCheckerTheme
import trif.novica.spoilerchecker.ui.viewmodel.HomeViewModel
import trif.novica.spoilerchecker.ui.viewmodel.RecentGamesViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as SpoilerShieldApp

        setContent {
            SpoilerCheckerTheme {
                MainScreen(app)
            }
        }
    }
}

@Composable
private fun MainScreen(app: SpoilerShieldApp) {
    val navController = rememberNavController()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                bottomNavItems.forEach { screen ->
                    val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true

                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = if (selected) screen.selectedIcon else screen.unselectedIcon,
                                contentDescription = screen.title
                            )
                        },
                        label = { Text(screen.title) },
                        selected = selected,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                val viewModel: HomeViewModel = viewModel(
                    factory = HomeViewModel.Factory(
                        app.appModule.repository,
                        app.appModule.spoilerDetector,
                        app.appModule.embeddingModel
                    )
                )
                val uiState by viewModel.uiState.collectAsState()

                HomeScreen(
                    uiState = uiState,
                    onQueryChanged = viewModel::onQueryChanged,
                    onCleanSpoilers = viewModel::cleanSpoilers,
                    onAddTeam = viewModel::addFavoriteTeam,
                    onRemoveTeam = viewModel::removeFavoriteTeam,
                    onTeamClick = viewModel::useTeamAsQuery,
                    onDismissResult = viewModel::dismissLastResult,
                    onDismissError = viewModel::dismissError
                )
            }

            composable(Screen.Games.route) {
                val viewModel: RecentGamesViewModel = viewModel(
                    factory = RecentGamesViewModel.Factory(app.appModule.repository)
                )
                val uiState by viewModel.uiState.collectAsState()

                val homeViewModel: HomeViewModel = viewModel(
                    factory = HomeViewModel.Factory(
                        app.appModule.repository,
                        app.appModule.spoilerDetector,
                        app.appModule.embeddingModel
                    )
                )

                RecentGamesScreen(
                    uiState = uiState,
                    onFilterSport = viewModel::filterBySport,
                    onGameClick = { game ->
                        homeViewModel.onQueryChanged(game.matchDescription)
                        navController.navigate(Screen.Home.route)
                    }
                )
            }

            composable(Screen.Settings.route) {
                SettingsScreen()
            }
        }
    }
}
