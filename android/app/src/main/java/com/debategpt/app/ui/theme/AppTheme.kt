package com.debategpt.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.Typography
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape

private val LightColorScheme = lightColorScheme(
    primary = TealPrimary,
    onPrimary = OnPrimaryLight,
    primaryContainer = Color(0xFFB8E8E8),
    onPrimaryContainer = Navy,
    secondary = Accent,
    onSecondary = OnPrimaryLight,
    secondaryContainer = Color(0xFFFFE5D4),
    onSecondaryContainer = Color(0xFF5C2E0A),
    tertiary = Coral,
    onTertiary = OnPrimaryLight,
    surface = SurfaceLight,
    onSurface = Navy,
    onSurfaceVariant = OnSurfaceVariantLight,
    surfaceVariant = SurfaceLightVariant,
    error = ErrorRed,
    onError = OnPrimaryLight,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    outline = Color(0xFF73777B),
    outlineVariant = Color(0xFFDEE2E6)
)

private val DarkColorScheme = darkColorScheme(
    primary = TealLight,
    onPrimary = NavyDark,
    primaryContainer = TealPrimary,
    onPrimaryContainer = Color(0xFFB8E8E8),
    secondary = AccentLight,
    onSecondary = NavyDark,
    secondaryContainer = Color(0xFF6B3D14),
    onSecondaryContainer = Color(0xFFFFE5D4),
    tertiary = Coral,
    onTertiary = OnPrimaryLight,
    surface = SurfaceDark,
    onSurface = Color(0xFFE9ECEF),
    onSurfaceVariant = OnSurfaceVariantDark,
    surfaceVariant = SurfaceDarkVariant,
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    outline = Color(0xFF8D9196),
    outlineVariant = Color(0xFF3A3F44)
)

private val AppTypography = Typography(
    headlineLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        letterSpacing = (-0.5).sp
    ),
    headlineMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp
    ),
    titleLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp
    ),
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    labelLarge = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp
    )
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun DebateGPTTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}
