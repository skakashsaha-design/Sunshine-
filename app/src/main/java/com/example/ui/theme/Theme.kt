package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.domain.model.FontStylePreset

enum class ThemeAppearance(val label: String) {
    SYSTEM("System"),
    LIGHT("Daylight"),
    DARK("Midnight"),
    DYNAMIC_WEATHER("Dynamic Sky"),
    AURORA_MIDNIGHT("Aurora"),
    OCEAN_BREEZE("Ocean"),
    GLASS("Frosted Glass"),
    AMOLED_DARK("AMOLED"),
    MINIMAL("Minimal")
}

private val LightColors = lightColorScheme(
    primary = Color(0xFF1D4ED8),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDBEAFE),
    onPrimaryContainer = Color(0xFF0F295C),
    secondary = Color(0xFF0284C7),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0F2FE),
    onSecondaryContainer = Color(0xFF0C4A6E),
    tertiary = Color(0xFFD97706),
    onTertiary = Color.White,
    background = Color(0xFFF1F5F9),
    onBackground = Color(0xFF0F172A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFE2E8F0),
    onSurfaceVariant = Color(0xFF334155),
    outline = Color(0xFFCBD5E1),
    error = Color(0xFFDC2626),
    onError = Color.White
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF60A5FA),
    onPrimary = Color(0xFF061530),
    primaryContainer = Color(0xFF1E3A8A),
    onPrimaryContainer = Color(0xFFEFF6FF),
    secondary = Color(0xFF38BDF8),
    onSecondary = Color(0xFF042436),
    secondaryContainer = Color(0xFF0E4A6F),
    onSecondaryContainer = Color(0xFFE0F2FE),
    tertiary = Color(0xFFFBBF24),
    onTertiary = Color(0xFF291800),
    background = Color(0xFF091324),
    onBackground = Color(0xFFF8FAFC),
    surface = Color(0xFF102038),
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = Color(0xFF1A2F4D),
    onSurfaceVariant = Color(0xFFCBD5E1),
    outline = Color(0xFF334155),
    error = Color(0xFFF87171),
    onError = Color(0xFF450A0A)
)

private val AmoledColors = DarkColors.copy(
    background = Color(0xFF000000),
    surface = Color(0xFF0B1018),
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = Color(0xFF141C2B),
    onSurfaceVariant = Color(0xFFCBD5E1),
    outline = Color(0xFF263042)
)

private val GlassColors = DarkColors.copy(
    background = Color(0xFF071426),
    surface = Color(0xE610233F),
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = Color(0xCC193357),
    onSurfaceVariant = Color(0xFFD6E2F0),
    outline = Color(0x887DD3FC)
)

private val AuroraMidnightColors = DarkColors.copy(
    primary = Color(0xFF34D399),
    onPrimary = Color(0xFF032419),
    primaryContainer = Color(0xFF064E3B),
    onPrimaryContainer = Color(0xFFD1FAE5),
    secondary = Color(0xFF818CF8),
    secondaryContainer = Color(0xFF312E81),
    onSecondaryContainer = Color(0xFFE0E7FF),
    background = Color(0xFF070F1E),
    surface = Color(0xFF0E1B30),
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = Color(0xFF172844),
    onSurfaceVariant = Color(0xFFCBD5E1)
)

private val OceanBreezeColors = DarkColors.copy(
    primary = Color(0xFF2DD4BF),
    onPrimary = Color(0xFF042F2E),
    primaryContainer = Color(0xFF115E59),
    onPrimaryContainer = Color(0xFFCCFBF1),
    secondary = Color(0xFF38BDF8),
    background = Color(0xFF061A26),
    surface = Color(0xFF0C2636),
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = Color(0xFF14354A),
    onSurfaceVariant = Color(0xFFCBD5E1)
)

private val MinimalLightColors = LightColors.copy(
    background = Color(0xFFFFFFFF),
    surface = Color(0xFFF8FAFC),
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF334155),
    primary = Color(0xFF0F172A),
    secondary = Color(0xFF0284C7)
)

val SkyPulseShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun SkyPulseTheme(
    appearance: ThemeAppearance = ThemeAppearance.DYNAMIC_WEATHER,
    fontStyle: FontStylePreset = FontStylePreset.MODERN_GEOMETRIC,
    isNight: Boolean = false,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val colorScheme = when (appearance) {
        ThemeAppearance.SYSTEM -> if (systemDark) DarkColors else LightColors
        ThemeAppearance.LIGHT -> LightColors
        ThemeAppearance.DARK -> DarkColors
        ThemeAppearance.DYNAMIC_WEATHER -> DarkColors
        ThemeAppearance.AURORA_MIDNIGHT -> AuroraMidnightColors
        ThemeAppearance.OCEAN_BREEZE -> OceanBreezeColors
        ThemeAppearance.MINIMAL -> if (systemDark) DarkColors else MinimalLightColors
        ThemeAppearance.GLASS -> GlassColors
        ThemeAppearance.AMOLED_DARK -> AmoledColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = skyPulseTypographyFor(fontStyle),
        shapes = SkyPulseShapes
    ) {
        CompositionLocalProvider(LocalContentColor provides colorScheme.onBackground) {
            content()
        }
    }
}
