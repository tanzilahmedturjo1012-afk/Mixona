package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val MixonaDarkColorScheme = darkColorScheme(
    primary = MintPulseDark,
    onPrimary = Color(0xFF041412),
    primaryContainer = Color(0xFF103736),
    onPrimaryContainer = MintPulseDark,
    secondary = CyanSignalDark,
    onSecondary = Color(0xFF031924),
    secondaryContainer = Color(0xFF103042),
    onSecondaryContainer = CyanSignalDark,
    tertiary = GoldSignalDark,
    onTertiary = Color(0xFF231802),
    tertiaryContainer = Color(0xFF36280B),
    onTertiaryContainer = GoldSignalDark,
    background = ObsidianBg,
    onBackground = ObsidianText,
    surface = ObsidianSurface,
    onSurface = ObsidianText,
    surfaceVariant = ObsidianSurface2,
    onSurfaceVariant = ObsidianTextMuted,
    outline = ObsidianBorder,
    outlineVariant = Color(0x1E5EEAD4),
    error = CoralLossDark,
    onError = Color(0xFF2D0505),
)

private val MixonaLightColorScheme = lightColorScheme(
    primary = MintPulseLight,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCCFBF1),
    onPrimaryContainer = Color(0xFF0F3D38),
    secondary = CyanSignalLight,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0F2FE),
    onSecondaryContainer = Color(0xFF073A57),
    tertiary = GoldSignalLight,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFEF3C7),
    onTertiaryContainer = Color(0xFF452403),
    background = AlabasterBg,
    onBackground = AlabasterText,
    surface = AlabasterSurface,
    onSurface = AlabasterText,
    surfaceVariant = AlabasterSurface2,
    onSurfaceVariant = AlabasterTextMuted,
    outline = AlabasterBorder,
    outlineVariant = Color(0x1F0F172A),
    error = CoralLossLight,
    onError = Color.White,
)

private val MixonaShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

object MixonaTheme {
    val palette: MixonaPalette
        @Composable
        @ReadOnlyComposable
        get() = LocalMixonaPalette.current
}

@Composable
fun MixonaTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) MixonaDarkColorScheme else MixonaLightColorScheme
    val palette = if (darkTheme) DarkMixonaPalette else LightMixonaPalette

    CompositionLocalProvider(LocalMixonaPalette provides palette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = MixonaTypography,
            shapes = MixonaShapes,
            content = content,
        )
    }
}
