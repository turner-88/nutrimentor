package com.nutrimentor.dm.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable

/**
 * Root theme for the NutriMentor patient app. Wires the brand ColorScheme,
 * Sora/Plus Jakarta typography, soft shapes, and the five-pillar palette.
 * Brand-first: no Material-You dynamic color.
 */
@Composable
fun NutriMentorTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) NutriMentorDarkColors else NutriMentorLightColors
    CompositionLocalProvider(LocalNutriMentorColors provides NutriMentorColors()) {
        MaterialTheme(
            colorScheme = colors,
            typography = NutriMentorTypography,
            shapes = NutriMentorShapes,
            content = content,
        )
    }
}

/** Convenient access to the pillar palette: `MaterialTheme.nutriMentor.diet`. */
val MaterialTheme.nutriMentor: NutriMentorColors
    @Composable
    @ReadOnlyComposable
    get() = LocalNutriMentorColors.current
