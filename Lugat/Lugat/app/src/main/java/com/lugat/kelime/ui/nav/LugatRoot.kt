package com.lugat.kelime.ui.nav

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Style
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Style
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.lugat.kelime.ui.decks.DeckScreen
import com.lugat.kelime.ui.decks.DecksScreen
import com.lugat.kelime.ui.home.HomeScreen
import com.lugat.kelime.ui.onboarding.OnboardingScreen
import com.lugat.kelime.ui.settings.ChooseDecksScreen
import com.lugat.kelime.ui.settings.SettingsScreen
import com.lugat.kelime.ui.stats.StatsScreen
import com.lugat.kelime.ui.study.StudyScreen
import com.lugat.kelime.ui.theme.LugatTheme
import com.lugat.kelime.ui.wotd.WordOfDayScreen

private data class Tab(val route: String, val label: String, val icon: ImageVector, val selectedIcon: ImageVector)

private val TABS = listOf(
    Tab(Routes.HOME, "Bugün", Icons.Outlined.AutoStories, Icons.Rounded.AutoStories),
    Tab(Routes.DECKS, "Desteler", Icons.Outlined.Style, Icons.Rounded.Style),
    Tab(Routes.STATS, "İlerleme", Icons.Outlined.Insights, Icons.Rounded.Insights),
    Tab(Routes.SETTINGS, "Ayarlar", Icons.Outlined.Tune, Icons.Rounded.Tune),
)

@Composable
fun LugatRoot(
    openWordOfDay: Boolean,
    onWordOfDayHandled: () -> Unit,
    vm: MainViewModel = hiltViewModel(),
) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val s = settings ?: return

    LugatTheme(mode = s.theme) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            // Tanıtım bitince (onboardingDone değişince) gezinme grafiği "Bugün" ekranından yeniden kurulur.
            key(s.onboardingDone) {
                val nav = rememberNavController()
                LaunchedEffect(openWordOfDay) {
                    if (openWordOfDay && s.onboardingDone) {
                        nav.navigate(Routes.WOTD) { launchSingleTop = true }
                        onWordOfDayHandled()
                    }
                }
                AppScaffold(nav, startDestination = if (s.onboardingDone) Routes.HOME else Routes.ONBOARDING)
            }
        }
    }
}

@Composable
private fun AppScaffold(nav: NavHostController, startDestination: String) {
    val entry by nav.currentBackStackEntryAsState()
    val destination = entry?.destination
    val showBar = TABS.any { t -> destination?.hierarchy?.any { it.route == t.route } == true }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (showBar) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
                    TABS.forEach { tab ->
                        val selected = destination?.hierarchy?.any { it.route == tab.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                nav.navigate(tab.route) {
                                    popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(if (selected) tab.selectedIcon else tab.icon, contentDescription = null) },
                            label = { Text(tab.label) },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                                selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            ),
                        )
                    }
                }
            }
        },
    ) { padding ->
        val bottom = PaddingValues(bottom = padding.calculateBottomPadding())
        NavHost(
            navController = nav,
            startDestination = startDestination,
            modifier = Modifier.padding(bottom).consumeWindowInsets(bottom),
            enterTransition = { fadeIn() },
            exitTransition = { fadeOut() },
        ) {
            val openStudy: (String, String) -> Unit = { mode, source -> nav.navigate(Routes.study(mode, source)) }
            val openDeck: (String) -> Unit = { id -> nav.navigate(Routes.deck(id)) }

            composable(Routes.ONBOARDING) {
                OnboardingScreen() // tamamlanınca ayar değişir ve LugatRoot "Bugün" ekranına geçer
            }
            composable(Routes.HOME) {
                HomeScreen(
                    onStudy = openStudy,
                    onOpenDeck = openDeck,
                    onOpenWordOfDay = { nav.navigate(Routes.WOTD) },
                    onChooseDecks = { nav.navigate(Routes.CHOOSE_DECKS) },
                )
            }
            composable(Routes.DECKS) { DecksScreen(onOpenDeck = openDeck) }
            composable(Routes.STATS) { StatsScreen(onStudy = openStudy) }
            composable(Routes.SETTINGS) { SettingsScreen(onChooseDecks = { nav.navigate(Routes.CHOOSE_DECKS) }) }
            composable(Routes.CHOOSE_DECKS) { ChooseDecksScreen(onBack = { nav.popBackStack() }) }
            composable(Routes.WOTD) { WordOfDayScreen(onBack = { nav.popBackStack() }) }
            composable(Routes.DECK) { DeckScreen(onBack = { nav.popBackStack() }, onStudy = openStudy) }
            composable(Routes.STUDY) {
                StudyScreen(
                    onClose = { nav.popBackStack() },
                    onRestart = { mode, source ->
                        nav.navigate(Routes.study(mode, source)) { popUpTo(Routes.STUDY) { inclusive = true } }
                    },
                )
            }
        }
    }
}
