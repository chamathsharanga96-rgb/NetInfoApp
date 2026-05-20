package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// ==========================================
// PROFESSIONAL POLISH STYLE COLORS (M3 SPEC)
// ==========================================

// Primary Brand Color Theme - Classic Professional MD3 Blue
val ProfPrimary = Color(0xFF0061A4)                // Clean Blue
val ProfOnPrimary = Color(0xFFFFFFFF)
val ProfPrimaryContainer = Color(0xFFD1E4FF)         // Ice/Sky Blue Accent
val ProfOnPrimaryContainer = Color(0xFF001D36)       // Dark Blue Charcoal

// Secondary Accent theme
val ProfSecondary = Color(0xFF535F70)
val ProfOnSecondary = Color(0xFFFFFFFF)
val ProfSecondaryContainer = Color(0xFFD7E3F7)
val ProfOnSecondaryContainer = Color(0xFF101C2B)

// Background Canvas (Highly eye-comfortable Light Theme)
val ProfBackground = Color(0xFFFDFBFF)               // Cool Light Gray/White
val ProfOnBackground = Color(0xFF1C1B1F)             // Near Black
val ProfSurface = Color(0xFFFFFFFF)                  // Elegant White for cards
val ProfOnSurface = Color(0xFF1C1B1F)
val ProfSurfaceVariant = Color(0xFFF2F0F4)           // Cool Gray container/sub-cards
val ProfOnSurfaceVariant = Color(0xFF44474E)         // Secondary text gray
val ProfOutline = Color(0xFFC3C7CF)                  // Subtle outline/separator gray

// ==========================================
// DARK MODE COMPANION COLORS
// ==========================================
val ProfDarkPrimary = Color(0xFF9ECAFF)
val ProfDarkOnPrimary = Color(0xFF003258)
val ProfDarkPrimaryContainer = Color(0xFF00497D)
val ProfDarkOnPrimaryContainer = Color(0xFFD1E4FF)

val ProfDarkBackground = Color(0xFF1A1C1E)           // Dark charcoal slate
val ProfDarkOnBackground = Color(0xFFE2E2E6)
val ProfDarkSurface = Color(0xFF222427)              // Dark elevated cards
val ProfDarkOnSurface = Color(0xFFE2E2E6)
val ProfDarkSurfaceVariant = Color(0xFF43474E)
val ProfDarkOnSurfaceVariant = Color(0xFFC3C7CF)
val ProfDarkOutline = Color(0xFF8D9199)

// Diagnostics and Telemetry Colors
val SignalGreen = Color(0xFF2E7D32)                  // Soft green (excellent)
val TechTurquoise = Color(0xFF00838F)                // Slate turquoise (speed indicator)
val SignalOrange = Color(0xFFD84315)                 // Rich orange (moderate)
val SignalRed = Color(0xFFC62828)                    // Professional red (weak)

// Legacy compatibility fallbacks mapped to professional colors to prevent compilation errors
val LionGoldPrimary = ProfPrimary
val LionGoldSecondary = Color(0xFF00497D)
val RoyalCrimson = ProfPrimary
val RoyalCrimsonDark = ProfPrimaryContainer

val DarkCanvasBg = ProfBackground
val GlassCardSurface = ProfSurface
val TonalM3Surface = ProfSurfaceVariant
val BorderGoldMuted = ProfOutline

val WhiteText = ProfOnBackground
val TextMuted = ProfOnSurfaceVariant
val TextGold = ProfPrimary
