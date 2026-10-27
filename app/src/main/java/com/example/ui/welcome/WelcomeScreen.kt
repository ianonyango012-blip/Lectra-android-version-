package com.example.ui.welcome

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R

/**
 * SCREEN B — FIRST-INSTALL WELCOME SCREEN
 *
 * Appears only on first launch after installation.
 * Displays:
 * 1. Deep navy background with large curved crescent wave in lower half.
 * 2. Lectra open-book logo (white left page, violet right page).
 * 3. App name "Lectra" directly beneath the logo.
 * 4. Main tagline: "Your notes. Your structure.\nYour success."
 * 5. Wide pill-shaped violet gradient "Get Started" button.
 * 6. Bottom tagline: "Study smarter, not harder."
 */
@Composable
fun WelcomeScreen(
    onGetStarted: () -> Unit
) {
    // Subtle entrance animations
    val logoAlpha = remember { Animatable(0f) }
    val textAlpha = remember { Animatable(0f) }
    val buttonAlpha = remember { Animatable(0f) }

    var isProceeding by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        logoAlpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
        )
        textAlpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 450, easing = FastOutSlowInEasing)
        )
        buttonAlpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("welcome_screen_container")
    ) {
        // Deep navy background with curved indigo-violet wave
        WelcomeBackground(modifier = Modifier.fillMaxSize())

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            val totalHeight = maxHeight
            val topLogoPadding = totalHeight * 0.28f

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top spacer positioning open-book logo at ~28% from top
                Spacer(modifier = Modifier.height(topLogoPadding))

                // 1. Open-Book Logo (White left page, violet right page)
                Box(
                    modifier = Modifier
                        .size(width = 112.dp, height = 84.dp)
                        .graphicsLayer { alpha = logoAlpha.value }
                        .testTag("welcome_book_logo"),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_lectra_open_book),
                        contentDescription = "Lectra Open Book Logo",
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 2. App Name: "Lectra"
                Text(
                    text = "Lectra",
                    style = TextStyle(
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 44.sp,
                        letterSpacing = 0.5.sp,
                        color = Color.White
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .graphicsLayer { alpha = textAlpha.value }
                        .testTag("welcome_app_title")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // 3. Main Tagline: "Your notes. Your structure.\nYour success."
                Text(
                    text = "Your notes. Your structure.\nYour success.",
                    style = TextStyle(
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Normal,
                        fontSize = 15.sp,
                        lineHeight = 22.sp,
                        color = Color(0xFFD5D8F5)
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .graphicsLayer { alpha = textAlpha.value }
                        .testTag("welcome_main_tagline")
                )

                Spacer(modifier = Modifier.weight(1f))

                // 4. "Get Started" Pill Button
                val buttonGradient = Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xFF7565F6),
                        Color(0xFF8273FF)
                    )
                )

                Button(
                    onClick = {
                        if (!isProceeding) {
                            isProceeding = true
                            onGetStarted()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth(0.86f)
                        .widthIn(max = 420.dp)
                        .height(56.dp)
                        .graphicsLayer { alpha = buttonAlpha.value }
                        .clip(CircleShape)
                        .background(buttonGradient)
                        .testTag("welcome_get_started_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        contentColor = Color.White
                    ),
                    shape = CircleShape
                ) {
                    Text(
                        text = "Get Started",
                        style = TextStyle(
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp,
                            color = Color.White
                        )
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 5. Bottom Tagline: "Study smarter, not harder."
                Text(
                    text = "Study smarter, not harder.",
                    style = TextStyle(
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Normal,
                        fontSize = 13.5.sp,
                        color = Color(0xFFD5D8F5).copy(alpha = 0.78f)
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .graphicsLayer { alpha = buttonAlpha.value }
                        .padding(bottom = 18.dp)
                        .testTag("welcome_bottom_tagline")
                )
            }
        }
    }
}

/**
 * Background decoration for Screen B:
 * Main background #0B1028, dark central area #0A1026, lower decorative area #25245F, violet accent #8273FF.
 * Large curved crescent wave in lower half (upper edge dips toward centre and rises toward sides).
 */
@Composable
private fun WelcomeBackground(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // 1. Dark navy background base
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF0B1028),
                    Color(0xFF0A1026),
                    Color(0xFF0E1333)
                ),
                startY = 0f,
                endY = h
            )
        )

        // 2. Large curved wave/crescent in lower half
        // Upper edge dips toward center and rises toward sides
        val crescentWavePath = Path().apply {
            moveTo(0f, h * 0.58f)
            cubicTo(
                w * 0.25f, h * 0.65f, // dips toward center
                w * 0.75f, h * 0.65f, // dips toward center
                w, h * 0.56f          // rises toward right
            )
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }

        drawPath(
            path = crescentWavePath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF25245F).copy(alpha = 0.90f),
                    Color(0xFF141738)
                ),
                startY = h * 0.56f,
                endY = h
            )
        )

        // 3. Subtle violet highlight layer along lower crescent
        val highlightWavePath = Path().apply {
            moveTo(0f, h * 0.68f)
            cubicTo(
                w * 0.35f, h * 0.74f,
                w * 0.65f, h * 0.73f,
                w, h * 0.66f
            )
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }

        drawPath(
            path = highlightWavePath,
            brush = Brush.linearGradient(
                colors = listOf(
                    Color(0xFF8273FF).copy(alpha = 0.22f),
                    Color(0xFF25245F).copy(alpha = 0.70f)
                ),
                start = Offset(0f, h * 0.66f),
                end = Offset(w, h)
            )
        )
    }
}
