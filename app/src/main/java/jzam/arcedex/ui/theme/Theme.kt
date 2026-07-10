package jzam.arcedex.ui.theme

import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.runtime.Composable

private val DarkColorPalette = darkColors(
    primary = AccentRed,
    primaryVariant = AccentRedMuted,
    secondary = AccentBlue,
    secondaryVariant = AccentGreen,
    background = Background,
    surface = Surface,
    onPrimary = TextPrimary,
    onSecondary = Background,
    onBackground = TextPrimary,
    onSurface = TextPrimary
)

// Arcedex always ships a modern dark theme regardless of system setting - the app was
// specifically redesigned around this palette.
@Composable
fun ArcedexTheme(darkTheme: Boolean = true, content: @Composable () -> Unit) {
    MaterialTheme(
        colors = DarkColorPalette,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
