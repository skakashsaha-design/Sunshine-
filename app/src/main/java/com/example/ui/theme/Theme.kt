package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

enum class ThemeAppearance {
    SYSTEM,
    LIGHT,
    DARK,
    DYNAMIC_WEATHER,
    MINIMAL,
    GLASS,
    AMOLED_DARK
}

private val LightColors = lightColorScheme(
    primary = SkyCobalt,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDBEAFE),
    onPrimaryContainer = Color(0xFF0F295C),
    secondary = SkyCyan,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCFFAFE),
    onSecondaryContainer = Color(0xFF083344),
    tertiary = SkyAmber,
    onTertiary = Color.Black,
    background = SkySurfaceLight,
    onBackground = Color(0xFF0F172A),
    surface = SkyCardLight,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFE2E8F0),
    onSurfaceVariant = Color(0xFF475569),
    outline = Color(0xFFCBD5E1),
    error = SeverityExtreme,
    onError = Color.White
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF60A5FA),
    onPrimary = Color(0xFF081B3A),
    primaryContainer = Color(0xFF1E3A8A),
    onPrimaryContainer = Color(0xFFDBEAFE),
    secondary = Color(0xFF22D3EE),
    onSecondary = Color(0xFF062A36),
    secondaryContainer = Color(0xFF164E63),
    onSecondaryContainer = Color(0xFFCFFAFE),
    tertiary = SkyAmber,
    onTertiary = Color(0xFF291800),
    background = SkyDeepNavy,
    onBackground = Color(0xFFF1F5F9),
    surface = SkySurfaceDark,
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = SkyCardDark,
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = Color(0xFF334155),
    error = Color(0xFFF87171),
    onError = Color(0xFF450A0A)
)

private val AmoledColors = DarkColors.copy(
    background = Color(0xFF000000),
    surface = Color(0xFF090D14),
    surfaceVariant = Color(0xFF111827),
    outline = Color(0xFF1F2937)
)

private val GlassColors = DarkColors.copy(
    background = Color(0xFF081224),
    surface = Color(0xCC112240),
    surfaceVariant = Color(0x991E3557),
    outline = Color(0x5594A3B8)
)

private val MinimalLightColors = LightColors.copy(
    background = Color(0xFFFFFFFF),
    surface = Color(0xFFF8FAFC),
    surfaceVariant = Color(0xFFF1F5F9),
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
    appearance: ThemeAppearance = ThemeAppearance.SYSTEM,
    isNight: Boolean = false,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val colorScheme = when (appearance) {
        ThemeAppearance.SYSTEM -> if (systemDark) DarkColors else LightColors
        ThemeAppearance.LIGHT -> LightColors
        ThemeAppearance.DARK -> DarkColors
        ThemeAppearance.DYNAMIC_WEATHER -> if (isNight) DarkColors else LightColors
        ThemeAppearance.MINIMAL -> if (systemDark) DarkColors else MinimalLightColors
        ThemeAppearance.GLASS -> GlassColors
        ThemeAppearance.AMOLED_DARK -> AmoledColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = SkyPulseTypography,
        shapes = SkyPulseShapes,
        content = content
    )
}
