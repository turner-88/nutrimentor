package com.sebaya.dm.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable

/**
 * Root theme for the SebayaDM patient app. Wires the brand ColorScheme,
 * Sora/Plus Jakarta typography, soft shapes, and the five-pillar palette.
 * Brand-first: no Material-You dynamic color.
 */
@Composable
fun SebayaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) SebayaDarkColors else SebayaLightColors
    CompositionLocalProvider(LocalSebayaColors provides SebayaColors()) {
        MaterialTheme(
            colorScheme = colors,
            typography = SebayaTypography,
            shapes = SebayaShapes,
            content = content,
        )
    }
}

/** Convenient access to the pillar palette: `MaterialTheme.sebaya.diet`. */
val MaterialTheme.sebaya: SebayaColors
    @Composable
    @ReadOnlyComposable
    get() = LocalSebayaColors.current
