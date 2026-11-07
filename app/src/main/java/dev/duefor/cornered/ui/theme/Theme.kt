package dev.duefor.cornered.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val ArenaBackground = Color(0xFF0B0F14)
val ArenaSurface = Color(0xFF121A22)
val AccentCyan = Color(0xFF4DECC8)
val AccentRed = Color(0xFFFF6B6B)
val Ink = Color(0xFFE8F1F2)
val InkMuted = Color(0xFF9DB2BF)

private val CorneredColors = darkColorScheme(
    primary = AccentCyan,
    onPrimary = Color(0xFF06231F),
    secondary = AccentRed,
    onSecondary = Color(0xFF2B0A0A),
    background = ArenaBackground,
    onBackground = Ink,
    surface = ArenaSurface,
    onSurface = Ink,
    surfaceVariant = Color(0xFF1B2630),
    onSurfaceVariant = InkMuted,
    outline = Color(0xFF2A3A47),
)

@Composable
fun CorneredTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = CorneredColors, typography = Typography(), content = content)
}
