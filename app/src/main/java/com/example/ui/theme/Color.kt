package com.example.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Obsidian Dark Palette (Primary Studio Aesthetic)
val ObsidianBg = Color(0xFF060911)
val ObsidianSurface = Color(0xFF0D1422)
val ObsidianSurface2 = Color(0xFF131D30)
val ObsidianSurface3 = Color(0xFF1A2740)
val ObsidianBorder = Color(0xFF23334E)
val ObsidianText = Color(0xFFECF3FA)
val ObsidianTextMuted = Color(0xFF8CA4BE)

// Alabaster Light Palette
val AlabasterBg = Color(0xFFF3F6FA)
val AlabasterSurface = Color(0xFFFFFFFF)
val AlabasterSurface2 = Color(0xFFEBF0F7)
val AlabasterSurface3 = Color(0xFFDDE5F0)
val AlabasterBorder = Color(0xFFC9D4E3)
val AlabasterText = Color(0xFF0C1424)
val AlabasterTextMuted = Color(0xFF51647A)

// Brand & Semantic Signal Tokens
val MintPulseDark = Color(0xFF2DD4BF)
val MintPulseLight = Color(0xFF0D9488)
val CyanSignalDark = Color(0xFF22D3EE)
val CyanSignalLight = Color(0xFF0284C7)
val GoldSignalDark = Color(0xFFFBBF24)
val GoldSignalLight = Color(0xFFD97706)
val EmeraldWinDark = Color(0xFF34D399)
val EmeraldWinLight = Color(0xFF059669)
val CoralLossDark = Color(0xFFF87171)
val CoralLossLight = Color(0xFFE11D48)
val VioletFlareDark = Color(0xFFA78BFA)
val VioletFlareLight = Color(0xFF7C3AED)

@Immutable
data class MixonaPalette(
    val isDark: Boolean,
    val bgDeep: Color,
    val cardSurface: Color,
    val cardElevated: Color,
    val borderSubtle: Color,
    val borderStrong: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val mint: Color,
    val smallCyan: Color,
    val bigGold: Color,
    val winEmerald: Color,
    val lossCoral: Color,
    val guardAmber: Color,
    val violet: Color,
)

val DarkMixonaPalette = MixonaPalette(
    isDark = true,
    bgDeep = ObsidianBg,
    cardSurface = ObsidianSurface,
    cardElevated = ObsidianSurface2,
    borderSubtle = Color(0x245EEAD4),
    borderStrong = Color(0x4D5EEAD4),
    textPrimary = ObsidianText,
    textSecondary = ObsidianTextMuted,
    textTertiary = Color(0xFF607894),
    mint = MintPulseDark,
    smallCyan = CyanSignalDark,
    bigGold = GoldSignalDark,
    winEmerald = EmeraldWinDark,
    lossCoral = CoralLossDark,
    guardAmber = GoldSignalDark,
    violet = VioletFlareDark,
)

val LightMixonaPalette = MixonaPalette(
    isDark = false,
    bgDeep = AlabasterBg,
    cardSurface = AlabasterSurface,
    cardElevated = AlabasterSurface2,
    borderSubtle = Color(0x1A0F172A),
    borderStrong = Color(0x330F172A),
    textPrimary = AlabasterText,
    textSecondary = AlabasterTextMuted,
    textTertiary = Color(0xFF64748B),
    mint = MintPulseLight,
    smallCyan = CyanSignalLight,
    bigGold = GoldSignalLight,
    winEmerald = EmeraldWinLight,
    lossCoral = CoralLossLight,
    guardAmber = GoldSignalLight,
    violet = VioletFlareLight,
)

val LocalMixonaPalette = staticCompositionLocalOf { DarkMixonaPalette }
