package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LectraLightPrimaryAccent

/**
 * Renders the Lectra open-book logo monogram matching the visual reference screenshot.
 */
@Composable
fun LectraMonogram(
    modifier: Modifier = Modifier.size(36.dp)
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Colors
        val leftPageGrad = Brush.verticalGradient(
            colors = listOf(Color(0xFF9080FF), Color(0xFF7565F6)),
            startY = 0f,
            endY = h
        )
        val rightPageGrad = Brush.verticalGradient(
            colors = listOf(Color(0xFF6B58F5), Color(0xFF5541E8)),
            startY = 0f,
            endY = h
        )
        val centerSpineColor = Color(0xFF4330D0)

        // Center spine X
        val midX = w * 0.5f

        // Left Page Path (Curving upward and outward)
        val leftPath = Path().apply {
            moveTo(midX - w * 0.04f, h * 0.88f)
            // Left bottom curve
            cubicTo(
                midX - w * 0.22f, h * 0.94f,
                w * 0.16f, h * 0.88f,
                w * 0.08f, h * 0.82f
            )
            // Left outer edge upward
            lineTo(w * 0.08f, h * 0.22f)
            // Left top curve toward center
            cubicTo(
                w * 0.16f, h * 0.28f,
                midX - w * 0.22f, h * 0.34f,
                midX - w * 0.04f, h * 0.28f
            )
            close()
        }

        // Right Page Path (Curving upward and outward)
        val rightPath = Path().apply {
            moveTo(midX + w * 0.04f, h * 0.88f)
            // Right bottom curve
            cubicTo(
                midX + w * 0.22f, h * 0.94f,
                w * 0.84f, h * 0.88f,
                w * 0.92f, h * 0.82f
            )
            // Right outer edge upward
            lineTo(w * 0.92f, h * 0.22f)
            // Right top curve toward center
            cubicTo(
                w * 0.84f, h * 0.28f,
                midX + w * 0.22f, h * 0.34f,
                midX + w * 0.04f, h * 0.28f
            )
            close()
        }

        // Inside left bookmark/shadow fold
        val leftFoldPath = Path().apply {
            moveTo(midX - w * 0.04f, h * 0.28f)
            lineTo(midX - w * 0.04f, h * 0.88f)
            lineTo(midX - w * 0.18f, h * 0.91f)
            lineTo(midX - w * 0.18f, h * 0.31f)
            close()
        }

        drawPath(leftPath, leftPageGrad)
        drawPath(leftFoldPath, Brush.verticalGradient(listOf(Color(0x33000000), Color(0x11000000))))

        drawPath(rightPath, rightPageGrad)

        // Subtle highlights
        drawPath(
            leftPath,
            color = Color.White.copy(alpha = 0.25f),
            style = Stroke(width = w * 0.025f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        drawPath(
            rightPath,
            color = Color.White.copy(alpha = 0.2f),
            style = Stroke(width = w * 0.025f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

/**
 * Top branding row with Monogram + "Lectra" text.
 */
@Composable
fun LectraBrandHeader(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        LectraMonogram(modifier = Modifier.size(36.dp))
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = "Lectra",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                letterSpacing = (-0.5).sp
            ),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
