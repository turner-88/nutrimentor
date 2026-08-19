package com.sebaya.dm.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// SebayaDM brand palette — teal, matching the DIACARE reference design.
private val Teal = Color(0xFF0D9488)
private val TealDark = Color(0xFF0B7C72)
private val TealContainer = Color(0xFFCCFBF1)

private val LightColors = lightColorScheme(
    primary = Teal,
    onPrimary = Color.White,
    primaryContainer = TealContainer,
    onPrimaryContainer = Color(0xFF042F2A),
    secondary = Color(0xFF3B82F6),
    background = Color(0xFFF6FAF9),
    surface = Color.White,
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF2DD4BF),
    onPrimary = Color(0xFF042F2A),
    primaryContainer = TealDark,
    secondary = Color(0xFF60A5FA),
)

@Composable
fun SebayaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content,
    )
}
