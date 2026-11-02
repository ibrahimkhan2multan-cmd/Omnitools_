package com.omnitools.app

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
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
import com.omnitools.app.ui.theme.OmniToolsTheme
import com.omnitools.app.ui.tools.ToolHost
import kotlinx.coroutines.launch

private const val ROUTE_HOME = "home"
private const val ROUTE_SETTINGS = "settings"
private const val ROUTE_ABOUT = "about"
private const val ROUTE_CATEGORY = "category/{name}"
private const val ROUTE_TOOL = "tool/{id}"

@Composable
fun OmniApp(prefs: Prefs) {
    val themeMode by prefs.themeMode.collectAsStateWithLifecycle(initialValue = "system")
    OmniToolsTheme(themeMode = themeMode) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            AppNav(prefs, themeMode)
        }
    }
}

@Composable
private fun AppNav(prefs: Prefs, themeMode: String) {
    val nav = rememberNavController()
    val scope = rememberCoroutineScope()
    val favorites by prefs.favorites.collectAsStateWithLifecycle(initialValue = emptySet())
    val recents by prefs.recents.collectAsStateWithLifecycle(initialValue = emptyList())

    fun openTool(id: String) {
        scope.launch { prefs.pushRecent(id) }
        nav.navigate("tool/$id")
    }

    NavHost(navController = nav, startDestination = ROUTE_HOME) {
        composable(ROUTE_HOME) {
            HomeScreen(
                favorites = favorites,
                recents = recents,
                onOpenTool = ::openTool,
                onOpenCategory = { nav.navigate("category/${it.name}") },
                onToggleFavorite = { id -> scope.launch { prefs.toggleFavorite(id) } },
                onOpenSettings = { nav.navigate(ROUTE_SETTINGS) },
            )
        }
        composable(ROUTE_CATEGORY) { entry ->
            val category = Category.values().firstOrNull { it.name == entry.arguments?.getString("name") }
            if (category == null) {
                nav.popBackStack()
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
