package com.omnitools.app

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.omnitools.app.data.Category
import com.omnitools.app.data.Prefs
import com.omnitools.app.ui.home.CategoryScreen
import com.omnitools.app.ui.home.HomeScreen
import com.omnitools.app.ui.settings.AboutScreen
import com.omnitools.app.ui.settings.SettingsScreen
import com.omnitools.app.ui.settings.ThemeChoiceScreen
import com.omnitools.app.ui.settings.PrivacyGateScreen
import com.omnitools.app.ui.theme.OmniToolsTheme
import com.omnitools.app.ui.tools.ToolHost
import com.omnitools.app.ui.feedback.Feedback
import com.omnitools.app.ui.feedback.WithClickSounds
import kotlinx.coroutines.launch

private const val ROUTE_HOME = "home"
private const val ROUTE_SETTINGS = "settings"
private const val ROUTE_ABOUT = "about"
private const val ROUTE_CATEGORY = "category/{name}"
private const val ROUTE_TOOL = "tool/{id}"

@Composable
fun OmniApp(prefs: Prefs) {
    val scope = rememberCoroutineScope()
    // null = still loading from disk. Waiting for both values avoids flashing the wrong theme.
    val themeMode by prefs.themeMode.collectAsStateWithLifecycle<String?>(initialValue = null)
    val onboarded by prefs.onboarded.collectAsStateWithLifecycle<Boolean?>(initialValue = null)
    val privacy by prefs.privacyAccepted.collectAsStateWithLifecycle<Boolean?>(initialValue = null)

    val mode = themeMode
    val done = onboarded
    val accepted = privacy
    if (mode == null || done == null || accepted == null) {
        Box(Modifier.fillMaxSize().background(Color(0xFF0E1220)))
        return
    }

    val sounds by prefs.sounds.collectAsStateWithLifecycle(initialValue = true)
    val haptics by prefs.haptics.collectAsStateWithLifecycle(initialValue = true)
    androidx.compose.runtime.SideEffect { Feedback.soundOn = sounds; Feedback.hapticsOn = haptics }

    OmniToolsTheme(themeMode = mode) {
      WithClickSounds {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            if (!accepted) {
                PrivacyGateScreen(onAccept = { scope.launch { prefs.acceptPrivacy() } })
            } else if (!done) {
                ThemeChoiceScreen(onChosen = { choice -> scope.launch { prefs.completeOnboarding(choice) } })
            } else {
                AppNav(prefs, mode)
            }
        }
      }
    }
}

@Composable
private fun AppNav(prefs: Prefs, themeMode: String) {
    val nav = rememberNavController()
    val scope = rememberCoroutineScope()
    val favorites by prefs.favorites.collectAsStateWithLifecycle(initialValue = emptySet())
    val recents by prefs.recents.collectAsStateWithLifecycle(initialValue = emptyList())
    val showRecents by prefs.showRecents.collectAsStateWithLifecycle(initialValue = true)

    fun openTool(id: String) {
        scope.launch { prefs.pushRecent(id) }
        nav.navigate("tool/$id")
    }

    val designOrNull by prefs.musicDesign.collectAsStateWithLifecycle<com.omnitools.app.tools.music.MusicDesign?>(initialValue = null)
    val design = designOrNull ?: com.omnitools.app.tools.music.MusicDesign()
    val backEntry by nav.currentBackStackEntryAsState()
    val onMusicScreen = backEntry?.destination?.route == ROUTE_TOOL && backEntry?.arguments?.getString("id") == "music_player"
    val showMini = designOrNull != null && design.miniPlayer && com.omnitools.app.tools.music.MusicController.state != com.omnitools.app.tools.music.MusicState.EMPTY && !onMusicScreen

    androidx.compose.foundation.layout.Column(Modifier.fillMaxSize()) {
    Box(
        Modifier.weight(1f).then(
            if (showMini) Modifier.consumeWindowInsets(WindowInsets.navigationBars) else Modifier,
        ),
    ) {
    NavHost(
        navController = nav,
        startDestination = ROUTE_HOME,
        enterTransition = { slideInHorizontally(tween(340, easing = FastOutSlowInEasing)) { it / 5 } + fadeIn(tween(300)) },
        exitTransition = { slideOutHorizontally(tween(340, easing = FastOutSlowInEasing)) { -it / 8 } + fadeOut(tween(220)) },
        popEnterTransition = { slideInHorizontally(tween(340, easing = FastOutSlowInEasing)) { -it / 8 } + fadeIn(tween(300)) },
        popExitTransition = { slideOutHorizontally(tween(320, easing = FastOutSlowInEasing)) { it / 5 } + fadeOut(tween(240)) },
    ) {
        composable(ROUTE_HOME) {
            HomeScreen(
                favorites = favorites,
                recents = recents,
                showRecents = showRecents,
                onOpenTool = ::openTool,
                onOpenCategory = { nav.navigate("category/${it.name}") },
                onToggleFavorite = { id -> scope.launch { prefs.toggleFavorite(id) } },
                onOpenSettings = { nav.navigate(ROUTE_SETTINGS) },
            )
        }
        composable(ROUTE_CATEGORY) { entry ->
            val category = Category.values().firstOrNull { it.name == entry.arguments?.getString("name") }
            if (category == null) {
                androidx.compose.runtime.LaunchedEffect(Unit) { nav.popBackStack() }
            } else {
                CategoryScreen(
                    category = category,
                    favorites = favorites,
                    onBack = { nav.popBackStack() },
                    onOpenTool = ::openTool,
                    onToggleFavorite = { id -> scope.launch { prefs.toggleFavorite(id) } },
                )
            }
        }
        composable(ROUTE_TOOL) { entry ->
            val id = entry.arguments?.getString("id").orEmpty()
            ToolHost(toolId = id, prefs = prefs, onBack = { nav.popBackStack() })
        }
        composable(ROUTE_SETTINGS) {
            SettingsScreen(
                prefs = prefs,
                themeMode = themeMode,
                onBack = { nav.popBackStack() },
                onOpenAbout = { nav.navigate(ROUTE_ABOUT) },
            )
        }
        composable(ROUTE_ABOUT) { AboutScreen(onBack = { nav.popBackStack() }) }
    }
    }
    androidx.compose.animation.AnimatedVisibility(
        visible = showMini,
        enter = androidx.compose.animation.slideInVertically(tween(260)) { it } + fadeIn(tween(200)),
        exit = androidx.compose.animation.slideOutVertically(tween(220)) { it } + fadeOut(tween(160)),
    ) {
        com.omnitools.app.tools.music.MiniPlayer(design, onOpen = { nav.navigate("tool/music_player") })
    }
    }
}
