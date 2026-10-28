package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// ============================================================================
// LECTRA DUAL-THEME DESIGN SYSTEM COLOR PALETTE
// ============================================================================

// ----------------------------------------------------------------------------
// 1. Light Theme — Exact White & Violet Specification
// ----------------------------------------------------------------------------
val LectraLightPrimaryBg = Color(0xFFFFFFFF)        // Main background: #FFFFFF
val LectraLightPrimaryText = Color(0xFF0B1020)      // Primary text: #0B1020
val LectraLightSecondaryText = Color(0xFF64748B)    // Secondary text: #64748B
val LectraLightPrimaryAccent = Color(0xFF7565F6)    // Primary violet: #7565F6
val LectraLightSoftLavender = Color(0xFFF5F3FF)     // Pale lavender surfaces: #F5F3FF
val LectraLightBorder = Color(0xFFE3E7F2)           // Borders: #E3E7F2
val LectraLightCardBg = Color(0xFFFFFFFF)           // White cards: #FFFFFF
val LectraLightSecondaryBg = Color(0xFFF5F3FF)      // Secondary surfaces
val LectraLightAccentPressed = Color(0xFF6252E8)    // Pressed violet
val LectraLightDisabled = Color(0xFFCBD5E1)         // Disabled controls
val LectraLightSuccess = Color(0xFF16A34A)          // Semantic success
val LectraLightWarning = Color(0xFFD97706)          // Semantic warning
val LectraLightError = Color(0xFFDC2626)            // Semantic error

// ----------------------------------------------------------------------------
// 2. Dark Theme — Exact Dark Navy & Violet Specification
// ----------------------------------------------------------------------------
val LectraDarkPrimaryBg = Color(0xFF0B1020)         // Main background: #0B1020
val LectraDarkCardBg = Color(0xFF111B36)            // Card background: #111B36
val LectraDarkSecondaryCardBg = Color(0xFF17244A)   // Secondary card background: #17244A
val LectraDarkSecondaryBg = Color(0xFF17244A)       // Secondary background: #17244A
val LectraDarkCardElevated = Color(0xFF17244A)      // Elevated card: #17244A
val LectraDarkPrimaryText = Color(0xFFF8FAFC)       // Primary text: #F8FAFC
val LectraDarkSecondaryText = Color(0xFFB7C3E0)     // Secondary text: #B7C3E0
val LectraDarkPrimaryAccent = Color(0xFF806BFF)     // Primary violet: #806BFF
val LectraDarkBorder = Color(0xFF293966)            // Borders: #293966
val LectraDarkAccentPressed = Color(0xFF9587FF)     // Pressed violet
val LectraDarkSoftViolet = Color(0xFF1E1B4B)        // Soft violet container
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
