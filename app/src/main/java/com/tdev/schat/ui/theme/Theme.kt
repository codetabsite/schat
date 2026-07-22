package com.tdev.schat.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ── Palette ──────────────────────────────────────────────────────────────────
val Black       = Color(0xFF0A0A0A)
val Surface     = Color(0xFF111111)
val SurfaceVar  = Color(0xFF1A1A1A)
val Outline     = Color(0xFF2A2A2A)
val OutlineVar  = Color(0xFF333333)
val OnSurface   = Color(0xFFEEEEEE)
val OnSurfaceDim = Color(0xFF888888)
val Accent      = Color(0xFFE8FF47)   // sharp yellow-green accent
val AccentDim   = Color(0xFF9FAD30)
val BubbleMe    = Color(0xFF1E2A00)
val BubbleThem  = Color(0xFF1A1A1A)
val OnlineGreen = Color(0xFF4CAF50)

private val SChatColorScheme = darkColorScheme(
    primary          = Accent,
    onPrimary        = Black,
    secondary        = AccentDim,
    onSecondary      = Black,
    background       = Black,
    onBackground     = OnSurface,
    surface          = Surface,
    onSurface        = OnSurface,
    surfaceVariant   = SurfaceVar,
    onSurfaceVariant = OnSurfaceDim,
    outline          = Outline,
    outlineVariant   = OutlineVar,
)

// ── Typography ────────────────────────────────────────────────────────────────
val SChatTypography = androidx.compose.material3.Typography(
    headlineLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        letterSpacing = (-0.5).sp,
        color = OnSurface
    ),
    headlineMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        letterSpacing = (-0.3).sp,
        color = OnSurface
    ),
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        color = OnSurface
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 19.sp,
        color = OnSurfaceDim
    ),
    labelSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        letterSpacing = 0.3.sp,
        color = OnSurfaceDim
    )
)

@Composable
fun SChatTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = SChatColorScheme,
        typography  = SChatTypography,
        content     = content
    )
}
