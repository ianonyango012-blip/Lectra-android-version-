package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val KnowledgeInMotionColorScheme = darkColorScheme(
    primary = ElectricViolet,
    onPrimary = PureWhite,
    primaryContainer = DeepIndigo,
    onPrimaryContainer = LuminousCyan,
    secondary = LuminousCyan,
    onSecondary = MidnightNavy,
    secondaryContainer = CardSurfaceVariantNavy,
    onSecondaryContainer = PureWhite,
    tertiary = ElectricLime,
    onTertiary = MidnightNavy,
    tertiaryContainer = DeepIndigo,
    onTertiaryContainer = PureWhite,
    background = MidnightNavy,
    onBackground = PureWhite,
    surface = CardSurfaceNavy,
    onSurface = PureWhite,
    surfaceVariant = CardSurfaceVariantNavy,
    onSurfaceVariant = MutedBlueGrey,
    outline = CardBorderNavy,
    outlineVariant = DeepIndigo
)

@Composable
fun LectraTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep Knowledge in Motion visual identity consistent
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = KnowledgeInMotionColorScheme,
        typography = LectraTypography,
        content = content
    )
}
