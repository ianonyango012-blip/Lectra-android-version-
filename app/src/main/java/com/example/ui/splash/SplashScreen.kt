package com.example.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * SCREEN A — MINIMALIST BRANDED SPLASH SCREEN
 *
 * Displays:
 * 1. The word "Lectra" (white bold sans-serif ~66sp at ~42-45% height).
 * 2. The exact two-line tagline "Your notes. Your structure.\nYour success." (#C7C9FF ~22sp).
 * 3. Deep navy background with overlapping curved violet waves along bottom.
 *
 * Strictly NO book logo, NO buttons, NO extra text.
 */
@Composable
fun SplashScreen(
    autoDismiss: Boolean = true,
    splashDurationMillis: Long = 1000L,
    onSplashFinished: () -> Unit
) {
    LaunchedEffect(autoDismiss) {
        if (autoDismiss) {
            delay(splashDurationMillis)
            onSplashFinished()
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // 1. Background: Deep navy with subtle gradients and overlapping curved waves
        SplashBackground(modifier = Modifier.fillMaxSize())

        // 2. Full-screen tap surface to allow tapping to proceed immediately
        Box(
            modifier = Modifier
                .fillMaxSize()
                .testTag("splash_screen_container")
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    onSplashFinished()
                }
        )

        // 3. Foreground: Wordmark + Tagline positioned responsively at ~42-45% height
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
        ) {
            val totalHeight = maxHeight
            val totalWidth = maxWidth
            val topWordmarkPadding = totalHeight * 0.40f

            // Responsively adjust wordmark font size (64-70sp on typical phone, scaled for compact screens)
            val wordmarkFontSize = when {
                totalWidth < 340.dp -> 44.sp
                totalWidth < 380.dp -> 54.sp
                else -> 66.sp
            }

            val taglineFontSize = when {
                totalWidth < 340.dp -> 16.sp
                totalWidth < 380.dp -> 19.sp
                else -> 22.sp
            }

            val taglineLineHeight = when {
                totalWidth < 340.dp -> 24.sp
                totalWidth < 380.dp -> 28.sp
                else -> 32.sp
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = if (totalWidth < 360.dp) 12.dp else 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(topWordmarkPadding))

                Text(
                    text = "Lectra",
                    style = TextStyle(
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Bold,
                        fontSize = wordmarkFontSize,
                        letterSpacing = 1.sp,
                        color = Color.White
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.testTag("splash_wordmark")
                )

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = "Your notes. Your structure.\nYour success.",
                    style = TextStyle(
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = taglineFontSize,
                        lineHeight = taglineLineHeight,
                        color = Color(0xFFC7C9FF)
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.testTag("splash_tagline")
                )
            }
        }
    }
}

/**
 * Background decoration for Screen A:
 * Main navy #050B25, secondary navy #080D2C, deep indigo #17165A, violet wave highlight #29247D.
 * Overlapping curved waves along the bottom 65-70% height with no outlines.
 */
@Composable
private fun SplashBackground(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // 1. Smooth dark navy base gradient
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF050B25),
                    Color(0xFF080D2C),
                    Color(0xFF0A102E)
                ),
                startY = 0f,
                endY = h
            )
        )

        // 2. Wave 1 (Upper curve): begins around 66% height forming a broad, shallow U-shaped curve
        val wave1Path = Path().apply {
            moveTo(0f, h * 0.67f)
            cubicTo(
                w * 0.28f, h * 0.72f,
                w * 0.68f, h * 0.73f,
                w, h * 0.65f
            )
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }
        drawPath(
            path = wave1Path,
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF29247D).copy(alpha = 0.85f),
                    Color(0xFF17165A)
                ),
                startY = h * 0.65f,
                endY = h
            )
        )

        // 3. Wave 2 (Middle curve): deep indigo to violet
        val wave2Path = Path().apply {
            moveTo(0f, h * 0.75f)
            cubicTo(
                w * 0.35f, h * 0.71f,
                w * 0.72f, h * 0.80f,
                w, h * 0.74f
            )
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }
        drawPath(
            path = wave2Path,
            brush = Brush.linearGradient(
                colors = listOf(
                    Color(0xFF17165A).copy(alpha = 0.95f),
                    Color(0xFF29247D).copy(alpha = 0.75f)
                ),
                start = Offset(0f, h * 0.72f),
                end = Offset(w, h)
            )
        )

        // 4. Wave 3 (Lowest curve): base wave anchoring the bottom
        val wave3Path = Path().apply {
            moveTo(0f, h * 0.84f)
            cubicTo(
                w * 0.32f, h * 0.88f,
                w * 0.68f, h * 0.82f,
                w, h * 0.86f
            )
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }
        drawPath(
            path = wave3Path,
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF17165A),
                    Color(0xFF080D2C)
                ),
                startY = h * 0.82f,
                endY = h
            )
        )
    }
}
