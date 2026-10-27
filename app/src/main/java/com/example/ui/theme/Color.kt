package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// ============================================================================
// LECTRA DUAL-THEME DESIGN SYSTEM COLOR PALETTE
// ============================================================================

// ----------------------------------------------------------------------------
// 1. Light Theme — Default Appearance (White & Violet)
// ----------------------------------------------------------------------------
val LectraLightPrimaryBg = Color(0xFFFFFFFF)        // Primary background: clean white
val LectraLightSecondaryBg = Color(0xFFF8F9FC)      // Secondary background
val LectraLightCardBg = Color(0xFFFFFFFF)           // Card background: white
val LectraLightPrimaryAccent = Color(0xFF7565F6)    // Primary accent: violet
val LectraLightAccentPressed = Color(0xFF6252E8)    // Accent pressed/active: deeper violet
val LectraLightSoftLavender = Color(0xFFF5F3FF)     // Soft lavender background
val LectraLightPrimaryText = Color(0xFF1F2937)      // Primary text: dark grey
val LectraLightSecondaryText = Color(0xFF64748B)    // Secondary text: slate grey
val LectraLightBorder = Color(0xFFE8EAF0)           // Borders and dividers: soft grey
val LectraLightDisabled = Color(0xFFCBD5E1)         // Disabled controls
val LectraLightSuccess = Color(0xFF16A34A)          // Semantic success
val LectraLightWarning = Color(0xFFD97706)          // Semantic warning
val LectraLightError = Color(0xFFDC2626)            // Semantic error

// ----------------------------------------------------------------------------
// 2. Dark Theme — Night-friendly Charcoal / Navy & Violet
// ----------------------------------------------------------------------------
val LectraDarkPrimaryBg = Color(0xFF0B1020)         // Primary background: deep charcoal/navy
val LectraDarkSecondaryBg = Color(0xFF10172A)       // Secondary background
val LectraDarkCardBg = Color(0xFF151D32)            // Card background
val LectraDarkCardElevated = Color(0xFF1B2640)      // Elevated card background
val LectraDarkPrimaryAccent = Color(0xFF806BFF)     // Primary accent: bright violet
val LectraDarkAccentPressed = Color(0xFF9587FF)     // Accent pressed/active: lighter violet
val LectraDarkSoftViolet = Color(0xFF27234A)        // Soft violet background
val LectraDarkPrimaryText = Color(0xFFF1F5F9)       // Primary text: light off-white
val LectraDarkSecondaryText = Color(0xFFA5B4CF)     // Secondary text: muted blue-grey
val LectraDarkBorder = Color(0xFF29344D)            // Borders and dividers
val LectraDarkDisabled = Color(0xFF64748B)          // Disabled controls
val LectraDarkSuccess = Color(0xFF4ADE80)           // Semantic success (dark mode)
val LectraDarkWarning = Color(0xFFFBBF24)           // Semantic warning (dark mode)
val LectraDarkError = Color(0xFFF87171)             // Semantic error (dark mode)

// ----------------------------------------------------------------------------
// 3. Status Badges & Semantic Accents
// ----------------------------------------------------------------------------
val CampusGreen = Color(0xFF16A34A)
val CampusGreenContainer = Color(0xFFDCFCE7)
val CampusGreenDark = Color(0xFF4ADE80)
val CampusGreenContainerDark = Color(0xFF064E2E)

val CampusBlue = Color(0xFF38BDF8)
val CampusBlueContainer = Color(0xFFE0F2FE)
val CampusBlueDark = Color(0xFF38BDF8)
val CampusBlueContainerDark = Color(0xFF0C4A6E)

val CampusRed = Color(0xFFDC2626)
val CampusRedContainer = Color(0xFFFEE2E2)
val CampusRedDark = Color(0xFFF87171)
val CampusRedContainerDark = Color(0xFF450A0A)

val CampusPurple = Color(0xFF7565F6)
val CampusPurpleContainer = Color(0xFFF5F3FF)
val CampusPurpleDark = Color(0xFF806BFF)
val CampusPurpleContainerDark = Color(0xFF27234A)

// ----------------------------------------------------------------------------
// 4. Backward Compatibility Aliases
// ----------------------------------------------------------------------------
val MidnightNavy = LectraDarkPrimaryBg
val DeepIndigo = LectraDarkSecondaryBg
val ElectricViolet = LectraLightPrimaryAccent
val LuminousCyan = Color(0xFF4DE8E0)
val ElectricLime = Color(0xFFD7FF72)
val PureWhite = Color(0xFFFFFFFF)
val MutedBlueGrey = LectraLightSecondaryText
val CardSurfaceNavy = LectraDarkCardBg
val CardSurfaceVariantNavy = LectraDarkCardElevated
val CardBorderNavy = LectraDarkBorder
