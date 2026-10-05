package com.pineapple.downloader.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightScheme = lightColorScheme(
    primary = PineappleGreen,
    onPrimary = PineappleWhite,
    primaryContainer = PineappleGreenLight,
    onPrimaryContainer = PineappleWhite,
    secondary = PineappleYellow,
    onSecondary = PineappleInk,
    secondaryContainer = PineappleOrangeDark,
    onSecondaryContainer = PineappleWhite,
    background = PineappleWhite,
    onBackground = PineappleInk,
    surface = PineappleWhite,
    onSurface = PineappleInk,
    surfaceVariant = PineappleSurface,
    onSurfaceVariant = PineappleInk,
    outline = PineappleOutline,
    outlineVariant = PineappleOutline,
    error = PineappleError,
    onError = PineappleWhite,
)

private val DarkScheme = darkColorScheme(
    primary = PineappleGreen,
    onPrimary = PineappleWhite,
    primaryContainer = PineappleGreenLight,
    onPrimaryContainer = PineappleInk,
    secondary = PineappleYellow,
    onSecondary = PineappleInk,
    secondaryContainer = PineappleOrangeDark,
    onSecondaryContainer = PineappleInk,
    background = PineappleDarkBg,
    onBackground = PineappleDarkText,
    surface = PineappleDarkSurface,
    onSurface = PineappleDarkText,
    surfaceVariant = PineappleDarkSurface,
    onSurfaceVariant = PineappleDarkSubtext,
    outline = Color(0xFF333333),
    outlineVariant = Color(0xFF333333),
    error = PineappleError,
    onError = PineappleWhite,
)

/** الثيم: فاتح/داكن/تلقائي يُتحكم به من شاشة الإعدادات. */
@Composable
fun PineappleTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit,
) {
    val dark = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    MaterialTheme(
        colorScheme = if (dark) DarkScheme else LightScheme,
        typography = PineappleTypography,
        shapes = PineappleShapes,
        content = content,
    )
}

enum class ThemeMode { LIGHT, DARK, SYSTEM }
