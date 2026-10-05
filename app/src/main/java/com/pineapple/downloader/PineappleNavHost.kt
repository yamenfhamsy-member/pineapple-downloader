package com.pineapple.downloader

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.pineapple.downloader.ui.screens.DownloadsScreen
import com.pineapple.downloader.ui.screens.HomeScreen
import com.pineapple.downloader.ui.screens.SettingsScreen
import com.pineapple.downloader.ui.screens.SplashScreen
import com.pineapple.downloader.ui.theme.PineappleTheme
import com.pineapple.downloader.ui.theme.ThemeMode
import com.pineapple.downloader.util.SettingsStore

private sealed interface Route {
    data object Splash : Route
    data object Home : Route
    data object Downloads : Route
    data object Settings : Route
}

/** التنقل بين الشاشات الأربع (بدون مكتبة تنقل — بسيط ومناسب لحجم MVP). */
@Composable
fun PineappleNavHost(initialSharedUrl: String) {
    val context = LocalContext.current
    val themeMode by remember { SettingsStore.themeMode(context) }
        .collectAsState(initial = ThemeMode.SYSTEM)

    var route: Route by remember { mutableStateOf(if (initialSharedUrl.isBlank()) Route.Splash else Route.Home) }

    PineappleTheme(themeMode = themeMode) {
        when (route) {
            Route.Splash -> SplashScreen(onDone = { route = Route.Home })
            Route.Home -> HomeScreen(
                onOpenSettings = { route = Route.Settings },
                onOpenDownloads = { route = Route.Downloads },
            )
            Route.Downloads -> DownloadsScreen(
                onOpenSettings = { route = Route.Settings },
                onBackToHome = { route = Route.Home },
            )
            Route.Settings -> SettingsScreen(onBack = { route = Route.Home })
        }
    }
}
