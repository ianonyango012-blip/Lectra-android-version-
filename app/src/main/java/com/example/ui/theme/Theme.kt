package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * 3-way Appearance selection for Lectra.
 */
enum class ThemeMode {
    LIGHT,
    DARK,
    SYSTEM
}

/**
 * Extended semantic tokens for Lectra design system.
 */
@Immutable
data class LectraExtendedColors(
    val secondaryBackground: Color,
    val cardBackground: Color,
    val elevatedCardBackground: Color,
    val primaryAccent: Color,
    val accentPressed: Color,
    val softAccentContainer: Color,
    val onSoftAccentContainer: Color,
    val primaryText: Color,
    val secondaryText: Color,
    val border: Color,
    val disabled: Color,
    val success: Color,
    val warning: Color,
    val error: Color
)

val LightExtendedColors = LectraExtendedColors(
    secondaryBackground = LectraLightSecondaryBg,
    cardBackground = LectraLightCardBg,
    elevatedCardBackground = LectraLightSecondaryBg,
    primaryAccent = LectraLightPrimaryAccent,
    accentPressed = LectraLightAccentPressed,
    softAccentContainer = LectraLightSoftLavender,
    onSoftAccentContainer = LectraLightPrimaryAccent,
    primaryText = LectraLightPrimaryText,
    secondaryText = LectraLightSecondaryText,
    border = LectraLightBorder,
    disabled = LectraLightDisabled,
    success = LectraLightSuccess,
    warning = LectraLightWarning,
    error = LectraLightError
)

val DarkExtendedColors = LectraExtendedColors(
    secondaryBackground = LectraDarkSecondaryBg,
    cardBackground = LectraDarkCardBg,
    elevatedCardBackground = LectraDarkCardElevated,
    primaryAccent = LectraDarkPrimaryAccent,
    accentPressed = LectraDarkAccentPressed,
    softAccentContainer = LectraDarkSoftViolet,
    onSoftAccentContainer = LectraDarkPrimaryAccent,
    primaryText = LectraDarkPrimaryText,
    secondaryText = LectraDarkSecondaryText,
    border = LectraDarkBorder,
    disabled = LectraDarkDisabled,
    success = LectraDarkSuccess,
    warning = LectraDarkWarning,
    error = LectraDarkError
)

val LocalLectraExtendedColors = staticCompositionLocalOf { LightExtendedColors }

val MaterialTheme.extendedColors: LectraExtendedColors
    @Composable
    @ReadOnlyComposable
    get() = LocalLectraExtendedColors.current

// Material 3 Light Color Scheme
private val LectraLightColorScheme: ColorScheme = lightColorScheme(
    primary = LectraLightPrimaryAccent,
    onPrimary = Color.White,
    primaryContainer = LectraLightSoftLavender,
    onPrimaryContainer = LectraLightPrimaryAccent,
    secondary = LectraLightPrimaryAccent,
    onSecondary = Color.White,
    secondaryContainer = LectraLightSoftLavender,
    onSecondaryContainer = LectraLightPrimaryText,
    tertiary = LectraLightAccentPressed,
    onTertiary = Color.White,
    tertiaryContainer = LectraLightSoftLavender,
    onTertiaryContainer = LectraLightAccentPressed,
    background = LectraLightPrimaryBg,
    onBackground = LectraLightPrimaryText,
    surface = LectraLightCardBg,
    onSurface = LectraLightPrimaryText,
    surfaceVariant = LectraLightSecondaryBg,
    onSurfaceVariant = LectraLightSecondaryText,
    outline = LectraLightBorder,
    outlineVariant = LectraLightBorder,
    error = LectraLightError,
    onError = Color.White
)

// Material 3 Dark Color Scheme
private val LectraDarkColorScheme: ColorScheme = darkColorScheme(
    primary = LectraDarkPrimaryAccent,
    onPrimary = Color.White,
    primaryContainer = LectraDarkSoftViolet,
    onPrimaryContainer = LectraDarkPrimaryAccent,
    secondary = LectraDarkPrimaryAccent,
    onSecondary = Color.White,
    secondaryContainer = LectraDarkSoftViolet,
    onSecondaryContainer = LectraDarkPrimaryText,
    tertiary = LectraDarkAccentPressed,
    onTertiary = Color.White,
    tertiaryContainer = LectraDarkSoftViolet,
    onTertiaryContainer = LectraDarkAccentPressed,
    background = LectraDarkPrimaryBg,
    onBackground = LectraDarkPrimaryText,
    surface = LectraDarkCardBg,
    onSurface = LectraDarkPrimaryText,
    surfaceVariant = LectraDarkCardElevated,
    onSurfaceVariant = LectraDarkSecondaryText,
    outline = LectraDarkBorder,
    outlineVariant = LectraDarkBorder,
    error = LectraDarkError,
    onError = Color(0xFF0B1020)
)

/**
 * LectraTheme provides the consistent dual-theme design system.
 * Defaults to Light mode on first launch as requested.
 */
@Composable
fun LectraTheme(
    themeMode: ThemeMode = ThemeMode.LIGHT,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val colorScheme = if (darkTheme) LectraDarkColorScheme else LectraLightColorScheme
    val extendedColors = if (darkTheme) DarkExtendedColors else LightExtendedColors

    CompositionLocalProvider(
        LocalLectraExtendedColors provides extendedColors
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = LectraTypography,
            content = content
        )
    }
}

/**
 * Backward compatibility overload accepting a boolean darkTheme flag.
 */
@Composable
fun LectraTheme(
    darkTheme: Boolean,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    LectraTheme(
        themeMode = if (darkTheme) ThemeMode.DARK else ThemeMode.LIGHT,
        dynamicColor = dynamicColor,
        content = content
    )
}
