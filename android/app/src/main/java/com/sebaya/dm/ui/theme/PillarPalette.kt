package com.sebaya.dm.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * The five-pillar accent colors, carried alongside the Material [ColorScheme]
 * (which has no room for them). Access via [LocalSebayaColors] or the
 * `MaterialTheme.sebaya` extension in Theme.kt.
 */
data class SebayaColors(
    val obat: Color = PillarObat,
    val aktivitas: Color = PillarAktivitas,
    val diet: Color = PillarDiet,
    val gula: Color = PillarGula,
    val edukasi: Color = PillarEdukasi,
    val success: Color = Success,
    val warning: Color = Warning,
    // Podium accents for the leaderboard top three.
    val gold: Color = Color(0xFFF59E0B),
    val silver: Color = Color(0xFF94A3B8),
    val bronze: Color = Color(0xFFB45309),
)

val LocalSebayaColors = staticCompositionLocalOf { SebayaColors() }
