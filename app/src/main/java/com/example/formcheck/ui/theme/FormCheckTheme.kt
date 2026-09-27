package com.example.formcheck.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ---------------------------------------------------------------------------
// COLOR
// Neutral interface, one distinctive accent. The accent (electric indigo)
// signals "precision tracking" without leaning on cliché fitness red/green.
// Good/Bad are reserved strictly for rep-judgement feedback, never decoration.
// ---------------------------------------------------------------------------

val Background = Color(0xFFFAFAFA)
val Surface = Color(0xFFFFFFFF)
val OnSurface = Color(0xFF1A1A1E)
val OnSurfaceMuted = Color(0xFF6B6B72)
val Accent = Color(0xFF5B5FEF)
val AccentMuted = Color(0xFFEEEFFC)
val Good = Color(0xFF2E9E5B)
val Bad = Color(0xFFD64545)
val Divider = Color(0xFFECECEF)

private val FormCheckColorScheme = lightColorScheme(
    primary = Accent,
    onPrimary = Color.White,
    primaryContainer = AccentMuted,
    onPrimaryContainer = Accent,
    background = Background,
    onBackground = OnSurface,
    surface = Surface,
    onSurface = OnSurface,
    surfaceVariant = AccentMuted,
    onSurfaceVariant = OnSurfaceMuted,
    outline = Divider,
    error = Bad,
)

// ---------------------------------------------------------------------------
// TYPE
// One family (Manrope) carries the whole hierarchy through weight.
// Fallback to SansSerif as font resources are missing.
// ---------------------------------------------------------------------------

private val Manrope = FontFamily.SansSerif

val FormCheckTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = Manrope,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 32.sp,
        lineHeight = 38.sp,
        letterSpacing = (-0.3).sp,
    ),
    titleLarge = TextStyle(
        fontFamily = Manrope,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = Manrope,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = Manrope,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = Manrope,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        color = OnSurfaceMuted,
    ),
    labelLarge = TextStyle(
        fontFamily = Manrope,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        letterSpacing = 0.sp,
    ),
)

val FormCheckShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
)

@Composable
fun FormCheckTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = FormCheckColorScheme,
        typography = FormCheckTypography,
        shapes = FormCheckShapes,
        content = content,
    )
}