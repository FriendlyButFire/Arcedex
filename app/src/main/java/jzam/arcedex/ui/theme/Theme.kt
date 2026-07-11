package jzam.arcedex.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val ArcedexColorScheme = darkColorScheme(
    primary = AccentRed,
    onPrimary = TextPrimary,
    primaryContainer = AccentRedMuted,
    onPrimaryContainer = AccentRed,
    secondary = AccentBlue,
    onSecondary = Background,
    tertiary = AccentGreen,
    onTertiary = Background,
    background = Background,
    onBackground = TextPrimary,
    surface = Surface,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceElevated,
    onSurfaceVariant = TextSecondary,
    outline = SurfaceBorder,
    outlineVariant = SurfaceBorder,
    error = AccentRed,
    onError = TextPrimary
)

// Arcedex always ships this modern dark theme regardless of system setting - it was
// specifically redesigned around this palette rather than dynamic/system color.
@Composable
fun ArcedexTheme(darkTheme: Boolean = true, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ArcedexColorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
