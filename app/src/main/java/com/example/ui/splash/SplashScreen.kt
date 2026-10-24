package com.example.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DeepIndigo
import com.example.ui.theme.ElectricLime
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.LuminousCyan
import com.example.ui.theme.MichromaFontFamily
import com.example.ui.theme.MidnightNavy
import com.example.ui.theme.MutedBlueGrey
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SoraFontFamily
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit
) {
    // Animatable states
    val logoAlpha = remember { Animatable(0f) }
    val logoProgress = remember { Animatable(0f) }
    val particleAlpha = remember { Animatable(0f) }
    val wordmarkAlpha = remember { Animatable(0f) }
    val taglineAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        // Step 1: 0 - 500ms: Logo fades in and book geometry opens outward
        launch {
            logoAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 500, easing = LinearEasing)
            )
        }
        launch {
            logoProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = 650,
                    easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
                )
            )
        }

        // Step 2: 300 - 850ms: Luminous particles and connection lines emerge
        delay(300)
        launch {
            particleAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 550, easing = FastOutSlowInEasing)
            )
        }

        // Step 3: 600 - 1100ms: LECTRA wordmark in Michroma fades in
        delay(300)
        launch {
            wordmarkAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
            )
        }

        // Step 4: 800 - 1300ms: Tagline in Sora fades in
        delay(200)
        launch {
            taglineAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
            )
        }

        // Hold composition static and polished, then complete launch
        delay(600)
        onSplashFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        DeepIndigo.copy(alpha = 0.85f),
                        MidnightNavy
                    ),
                    radius = 1200f
                )
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                // Allow tapping to proceed immediately without delay
                onSplashFinished()
            }
            .systemBarsPadding()
            .testTag("splash_screen_container"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Central Abstract "Knowledge in Motion" Logo
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .graphicsLayer {
                        alpha = logoAlpha.value
                        scaleX = 0.9f + (logoProgress.value * 0.1f)
                        scaleY = 0.9f + (logoProgress.value * 0.1f)
                    },
                contentAlignment = Alignment.Center
            ) {
                KnowledgeInMotionSymbol(
                    drawProgress = logoProgress.value,
                    particleProgress = particleAlpha.value
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Brand Wordmark: Michroma uppercase with controlled letter spacing
            Text(
                text = "LECTRA",
                style = MaterialTheme.typography.displaySmall.copy(
                    fontFamily = MichromaFontFamily,
                    fontWeight = FontWeight.Normal,
                    letterSpacing = 6.sp,
                    color = PureWhite
                ),
                modifier = Modifier
                    .graphicsLayer { alpha = wordmarkAlpha.value }
                    .testTag("splash_wordmark")
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Tagline: "Learn it. Link it. Remember it." in Sora
            Text(
                text = "Learn it. Link it. Remember it.",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = SoraFontFamily,
                    fontWeight = FontWeight.Normal,
                    letterSpacing = 0.6.sp,
                    color = MutedBlueGrey
                ),
                modifier = Modifier
                    .graphicsLayer { alpha = taglineAlpha.value }
                    .testTag("splash_tagline")
            )
        }
    }
}

@Composable
fun KnowledgeInMotionSymbol(
    drawProgress: Float,
    particleProgress: Float,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val centerX = w / 2f
        val centerY = h / 2f + 8f

        // 1. Spine / Central Axis
        val spineTop = centerY - 14f * drawProgress
        val spineBottom = centerY + 30f * drawProgress
        drawLine(
            color = LuminousCyan,
            start = Offset(centerX, spineTop),
            end = Offset(centerX, spineBottom),
            strokeWidth = 3f * drawProgress,
            cap = StrokeCap.Round
        )

        // 2. Left Page / Knowledge Ribbon
        val leftPath = Path().apply {
            moveTo(centerX, spineBottom)
            cubicTo(
                centerX - (20f * drawProgress), spineBottom,
                centerX - (46f * drawProgress), centerY + 18f,
                centerX - (56f * drawProgress), centerY + 8f
            )
            cubicTo(
                centerX - (56f * drawProgress), centerY - 12f,
                centerX - (38f * drawProgress), centerY - 20f,
                centerX - (18f * drawProgress), centerY - 18f
            )
            lineTo(centerX, spineTop)
            close()
        }

        // Fill Left Ribbon with Electric Violet
        drawPath(
            path = leftPath,
            brush = Brush.linearGradient(
                colors = listOf(ElectricViolet, ElectricViolet.copy(alpha = 0.75f)),
                start = Offset(centerX, centerY),
                end = Offset(centerX - 56f, centerY)
            )
        )

        // Left Ribbon Outline Highlight
        drawPath(
            path = leftPath,
            color = LuminousCyan.copy(alpha = 0.5f * drawProgress),
            style = Stroke(width = 1.5f)
        )

        // 3. Right Page / Knowledge Ribbon (Symmetrical outward flow)
        val rightPath = Path().apply {
            moveTo(centerX, spineBottom)
            cubicTo(
                centerX + (20f * drawProgress), spineBottom,
                centerX + (46f * drawProgress), centerY + 18f,
                centerX + (56f * drawProgress), centerY + 8f
            )
            cubicTo(
                centerX + (56f * drawProgress), centerY - 12f,
                centerX + (38f * drawProgress), centerY - 20f,
                centerX + (18f * drawProgress), centerY - 18f
            )
            lineTo(centerX, spineTop)
            close()
        }

        // Fill Right Ribbon with Electric Violet
        drawPath(
            path = rightPath,
            brush = Brush.linearGradient(
                colors = listOf(ElectricViolet, ElectricViolet.copy(alpha = 0.75f)),
                start = Offset(centerX, centerY),
                end = Offset(centerX + 56f, centerY)
            )
        )

        // Right Ribbon Outline Highlight
        drawPath(
            path = rightPath,
            color = LuminousCyan.copy(alpha = 0.5f * drawProgress),
            style = Stroke(width = 1.5f)
        )

        // 4. Emerging Connected Knowledge Lines & Particles
        if (particleProgress > 0.05f) {
            val pAlpha = particleProgress.coerceIn(0f, 1f)

            // Ascending Connection Rays
            val rayCenterY = centerY - 38f * particleProgress
            val rayLeftX = centerX - 26f * particleProgress
            val rayLeftY = centerY - 52f * particleProgress
            val rayRightX = centerX + 26f * particleProgress
            val rayRightY = centerY - 52f * particleProgress

            // Fine connection rays
            drawLine(
                color = LuminousCyan.copy(alpha = 0.6f * pAlpha),
                start = Offset(centerX, spineTop),
                end = Offset(centerX, rayCenterY),
                strokeWidth = 2f,
                cap = StrokeCap.Round
            )
            drawLine(
                color = ElectricViolet.copy(alpha = 0.5f * pAlpha),
                start = Offset(centerX, rayCenterY),
                end = Offset(rayLeftX, rayLeftY),
                strokeWidth = 1.5f,
                cap = StrokeCap.Round
            )
            drawLine(
                color = LuminousCyan.copy(alpha = 0.5f * pAlpha),
                start = Offset(centerX, rayCenterY),
                end = Offset(rayRightX, rayRightY),
                strokeWidth = 1.5f,
                cap = StrokeCap.Round
            )

            // Central Luminous Nodes
            // Center Particle (Cyan)
            drawCircle(
                color = LuminousCyan.copy(alpha = pAlpha),
                radius = 5f * particleProgress,
                center = Offset(centerX, rayCenterY)
            )
            drawCircle(
                color = LuminousCyan.copy(alpha = 0.35f * pAlpha),
                radius = 10f * particleProgress,
                center = Offset(centerX, rayCenterY)
            )

            // Left Particle (Violet)
            drawCircle(
                color = ElectricViolet.copy(alpha = pAlpha),
                radius = 4.5f * particleProgress,
                center = Offset(rayLeftX, rayLeftY)
            )

            // Right Particle (Cyan)
            drawCircle(
                color = LuminousCyan.copy(alpha = pAlpha),
                radius = 4.5f * particleProgress,
                center = Offset(rayRightX, rayRightY)
            )

            // Top Apex Sparkle (Lime Restrained Accent)
            val apexSparkleY = centerY - 62f * particleProgress
            drawCircle(
                color = ElectricLime.copy(alpha = 0.9f * pAlpha),
                radius = 2.5f * particleProgress,
                center = Offset(centerX, apexSparkleY)
            )
        }
    }
}
