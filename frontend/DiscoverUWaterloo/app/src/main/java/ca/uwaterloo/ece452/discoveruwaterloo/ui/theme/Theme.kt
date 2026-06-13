package ca.uwaterloo.ece452.discoveruwaterloo.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = UWGold,
    onPrimary = UWBlack,
    primaryContainer = UWGoldLight,
    onPrimaryContainer = UWBlack,
    secondary = UWGoldDark,
    onSecondary = UWBlack,
    background = UWSurface,
    surface = UWSurface,
)

private val DarkColorScheme = darkColorScheme(
    primary = UWGold,
    onPrimary = UWBlack,
    primaryContainer = UWGoldDark,
    onPrimaryContainer = UWBlack,
    secondary = UWGoldLight,
    onSecondary = UWBlack,
)

@Composable
fun DiscoverUWaterlooTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        content = content
    )
}