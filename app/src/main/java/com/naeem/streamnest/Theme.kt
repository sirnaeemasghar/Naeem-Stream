package com.naeem.streamnest

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Lime = Color(0xFF2FE69A)
val Forest = Color(0xFF051B10)

private val DarkPalette = darkColorScheme(
    primary = Color(0xFF2FE69A),
    onPrimary = Color(0xFF031A0F),
    primaryContainer = Color(0xFF133A26),
    onPrimaryContainer = Color(0xFF6DF7BA),
    secondary = Color(0xFFA5D8BF),
    onSecondary = Color(0xFF0E3825),
    secondaryContainer = Color(0xFF1C3A2D),
    onSecondaryContainer = Color(0xFFBCEBD4),
    surfaceContainerLowest = Color(0xFF080C0A),
    surfaceContainerLow = Color(0xFF0E1411),
    surfaceContainer = Color(0xFF131A16),
    surfaceContainerHigh = Color(0xFF1A241F),
    surfaceContainerHighest = Color(0xFF222F29),
    background = Color(0xFF0B100E),
    onBackground = Color(0xFFF1F5F2),
    surface = Color(0xFF131A16),
    onSurface = Color(0xFFF1F5F2),
    surfaceVariant = Color(0xFF1B2420),
    onSurfaceVariant = Color(0xFF9CB0A5),
    outline = Color(0xFF33463C),
    outlineVariant = Color(0xFF222F28),
    error = Color(0xFFFFB4AB),
    errorContainer = Color(0xFF492520),
    onErrorContainer = Color(0xFFFFDAD6)
)

private val LightPalette = lightColorScheme(
    primary = Color(0xFF0C784A),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6F6E5),
    onPrimaryContainer = Color(0xFF032D1B),
    secondary = Color(0xFF4C6A5A),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDEEDE4),
    onSecondaryContainer = Color(0xFF0D281C),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF3F7F4),
    surfaceContainer = Color(0xFFEBF2EE),
    surfaceContainerHigh = Color(0xFFE3ECE7),
    surfaceContainerHighest = Color(0xFFDBE5E0),
    background = Color(0xFFF6FAF8),
    onBackground = Color(0xFF0F1A14),
    surface = Color.White,
    onSurface = Color(0xFF0F1A14),
    surfaceVariant = Color(0xFFEEF5F1),
    onSurfaceVariant = Color(0xFF53665C),
    outline = Color(0xFFB8C8BF),
    outlineVariant = Color(0xFFDFE8E2),
    error = Color(0xFFBA1A1A),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002)
)

private val AppTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 38.sp,
        letterSpacing = (-0.8).sp
    ),
    headlineMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.5).sp
    ),
    titleLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 19.sp,
        lineHeight = 25.sp,
        letterSpacing = (-0.3).sp
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 21.sp,
        letterSpacing = (-0.1).sp
    ),
    titleSmall = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.1.sp
    ),
    bodyLarge = TextStyle(
        fontSize = 15.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.sp
    ),
    bodyMedium = TextStyle(
        fontSize = 13.5.sp,
        lineHeight = 19.sp,
        letterSpacing = 0.1.sp
    ),
    bodySmall = TextStyle(
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.2.sp
    ),
    labelLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.5.sp,
        lineHeight = 19.sp,
        letterSpacing = 0.2.sp
    ),
    labelMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.3.sp
    ),
    labelSmall = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 10.5.sp,
        lineHeight = 15.sp,
        letterSpacing = 0.8.sp
    )
)

@Composable fun StreamNestTheme(mode: String, content: @Composable () -> Unit) {
    val dark = mode == "Dark" || (mode == "System" && isSystemInDarkTheme())
    MaterialTheme(
        colorScheme = if (dark) DarkPalette else LightPalette,
        typography = AppTypography,
        content = content
    )
}
