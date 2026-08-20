package com.sebaya.dm.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.sebaya.dm.R

// Bundled variable fonts (offline — no CDN dependency, matching the clinical
// context). Each weight pins the `wght` axis of the variable .ttf.
@OptIn(ExperimentalTextApi::class)
private fun soraFont(weight: FontWeight) =
    Font(R.font.sora, weight = weight, variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)))

@OptIn(ExperimentalTextApi::class)
private fun jakartaFont(weight: FontWeight) =
    Font(R.font.plus_jakarta_sans, weight = weight, variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)))

// Sora — display & headings (tight, confident).
val Sora = FontFamily(
    soraFont(FontWeight.Normal),
    soraFont(FontWeight.Medium),
    soraFont(FontWeight.SemiBold),
    soraFont(FontWeight.Bold),
)

// Plus Jakarta Sans — body & labels (humanist, legible).
val Jakarta = FontFamily(
    jakartaFont(FontWeight.Normal),
    jakartaFont(FontWeight.Medium),
    jakartaFont(FontWeight.SemiBold),
    jakartaFont(FontWeight.Bold),
)

val SebayaTypography = Typography().run {
    copy(
        displaySmall = displaySmall.copy(fontFamily = Sora, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
        headlineLarge = headlineLarge.copy(fontFamily = Sora, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
        headlineMedium = headlineMedium.copy(fontFamily = Sora, fontWeight = FontWeight.Bold, letterSpacing = (-0.4).sp),
        headlineSmall = headlineSmall.copy(fontFamily = Sora, fontWeight = FontWeight.Bold, letterSpacing = (-0.3).sp),
        titleLarge = titleLarge.copy(fontFamily = Sora, fontWeight = FontWeight.Bold),
        titleMedium = titleMedium.copy(fontFamily = Sora, fontWeight = FontWeight.Bold),
        titleSmall = titleSmall.copy(fontFamily = Jakarta, fontWeight = FontWeight.Bold),
        bodyLarge = bodyLarge.copy(fontFamily = Jakarta),
        bodyMedium = bodyMedium.copy(fontFamily = Jakarta),
        bodySmall = bodySmall.copy(fontFamily = Jakarta, color = SlateMuted),
        labelLarge = labelLarge.copy(fontFamily = Jakarta, fontWeight = FontWeight.SemiBold),
        labelMedium = labelMedium.copy(fontFamily = Jakarta, fontWeight = FontWeight.SemiBold, letterSpacing = 0.4.sp),
        labelSmall = labelSmall.copy(fontFamily = Jakarta, fontWeight = FontWeight.SemiBold, letterSpacing = 0.6.sp),
    )
}

// Reusable style for the small uppercase eyebrow label used across cards.
val EyebrowStyle = TextStyle(
    fontFamily = Jakarta,
    fontWeight = FontWeight.SemiBold,
    fontSize = 11.sp,
    letterSpacing = 0.8.sp,
)
