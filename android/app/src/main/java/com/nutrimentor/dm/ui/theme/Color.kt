package com.nutrimentor.dm.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// ---------------------------------------------------------------------------
// NutriMentor brand tokens — mirrored from the admin panel design system
// (static/css/admin.src.css): emerald primary, teal secondary, amber accent,
// on a soft clinical neutral surface family.
// ---------------------------------------------------------------------------

val Emerald = Color(0xFF059669)
val EmeraldDark = Color(0xFF047857)
val EmeraldContainer = Color(0xFFD1FAE5)
val EmeraldOnContainer = Color(0xFF04372B)

val Teal = Color(0xFF0D9488)
val TealContainer = Color(0xFFCCFBF1)

val Amber = Color(0xFFD97706)
val AmberContainer = Color(0xFFFEF3C7)

val Ink = Color(0xFF12332A)         // deep brand ink for headings
val Slate = Color(0xFF1F2933)       // body text
val SlateMuted = Color(0xFF64748B)  // secondary text

val Base100 = Color(0xFFFFFFFF)     // surface
val Base200 = Color(0xFFF3F6F5)     // app background
val Base300 = Color(0xFFE3EAE7)     // borders / outline variant

val Danger = Color(0xFFDC2626)
val DangerContainer = Color(0xFFFEE2E2)
val Success = Color(0xFF16A34A)
val Warning = Color(0xFFD97706)

// Five-pillar accent palette (carried outside ColorScheme via NutriMentorColors).
val PillarObat = Color(0xFF6366F1)
val PillarAktivitas = Color(0xFFF59E0B)
val PillarDiet = Color(0xFFF43F5E)
val PillarGula = Color(0xFF0EA5E9)
val PillarEdukasi = Color(0xFF059669)

// Dark-scheme brand shifts.
private val EmeraldLight = Color(0xFF34D399)
private val TealLight = Color(0xFF2DD4BF)
private val DarkSurface = Color(0xFF10221C)
private val DarkBackground = Color(0xFF0B1713)
private val DarkOutline = Color(0xFF25453B)

val NutriMentorLightColors = lightColorScheme(
    primary = Emerald,
    onPrimary = Color.White,
    primaryContainer = EmeraldContainer,
    onPrimaryContainer = EmeraldOnContainer,
    secondary = Teal,
    onSecondary = Color.White,
    secondaryContainer = TealContainer,
    onSecondaryContainer = Color(0xFF04302C),
    tertiary = Amber,
    onTertiary = Color.White,
    tertiaryContainer = AmberContainer,
    onTertiaryContainer = Color(0xFF432B04),
    background = Base200,
    onBackground = Slate,
    surface = Base100,
    onSurface = Slate,
    surfaceVariant = Base200,
    onSurfaceVariant = SlateMuted,
    outline = Base300,
    outlineVariant = Base300,
    error = Danger,
    onError = Color.White,
    errorContainer = DangerContainer,
    onErrorContainer = Color(0xFF450A0A),
)

val NutriMentorDarkColors = darkColorScheme(
    primary = EmeraldLight,
    onPrimary = Color(0xFF04372B),
    primaryContainer = EmeraldDark,
    onPrimaryContainer = EmeraldContainer,
    secondary = TealLight,
    onSecondary = Color(0xFF04302C),
    tertiary = Color(0xFFFBBF24),
    onTertiary = Color(0xFF432B04),
    background = DarkBackground,
    onBackground = Color(0xFFE2E8E4),
    surface = DarkSurface,
    onSurface = Color(0xFFE2E8E4),
    surfaceVariant = Color(0xFF17302A),
    onSurfaceVariant = Color(0xFF9DB3AB),
    outline = DarkOutline,
    outlineVariant = DarkOutline,
    error = Color(0xFFF87171),
    onError = Color(0xFF450A0A),
)
