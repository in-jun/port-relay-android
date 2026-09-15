package dev.injun.portrelay.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * The app keeps its own colours rather than following the wallpaper.
 *
 * The icon is white on navy, and the app opens in the same navy, so it looks like the
 * thing that was tapped rather than like whatever the wallpaper is this week. The
 * error colour stays the one red on screen, so a relay that has stopped is findable
 * at a glance.
 */
private val LightColors = lightColorScheme(
    primary = Steel40,
    onPrimary = Color.White,
    primaryContainer = Steel90,
    onPrimaryContainer = Navy10,
    secondary = SteelGrey30,
    onSecondary = Color.White,
    secondaryContainer = SteelGrey90,
    onSecondaryContainer = Navy10,
    tertiary = Steel30,
    onTertiary = Color.White,
    tertiaryContainer = Steel90,
    onTertiaryContainer = Navy10,
    background = Neutral99,
    onBackground = Navy10,
    surface = Neutral99,
    onSurface = Navy10,
    surfaceVariant = SteelGrey90,
    onSurfaceVariant = SteelGrey30,
    outline = SteelGrey60,
    outlineVariant = SteelGrey80,
)

private val DarkColors = darkColorScheme(
    primary = Steel80,
    onPrimary = Navy10,
    primaryContainer = Steel30,
    onPrimaryContainer = Steel90,
    secondary = SteelGrey80,
    onSecondary = Navy10,
    secondaryContainer = SteelGrey30,
    onSecondaryContainer = SteelGrey90,
    tertiary = Steel80,
    onTertiary = Navy10,
    tertiaryContainer = Steel30,
    onTertiaryContainer = Steel90,
    background = Navy10,
    onBackground = Neutral90,
    surface = Navy10,
    onSurface = Neutral90,
    surfaceVariant = SteelGrey30,
    onSurfaceVariant = SteelGrey80,
    outline = SteelGrey60,
    outlineVariant = SteelGrey30,
)

@Composable
fun PortRelayTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = Typography,
        content = content,
    )
}
