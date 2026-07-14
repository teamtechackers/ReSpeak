/*
 * re:speak — real-time audio loopback for speech awareness
 * Copyright (C) 2026 Awais
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.respeak.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

enum class AudioButtonState { PLAY, PAUSE, DISABLED }

@Composable
fun AudioButton(
    state: AudioButtonState,
    accentColor: Color = Color(0xFF042C34),
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    // Teal glow color matching the mockup
    val glowColor = Color(0xFF00F5D4)
    val iconColor = if (state == AudioButtonState.DISABLED) Color(0xFFB0B8C1) else accentColor

    Canvas(
        modifier = modifier
            .size(320.dp, 220.dp)
            .then(if (state != AudioButtonState.DISABLED) Modifier.clickable { onClick() } else Modifier)
    ) {
        val centerX = size.width / 2f
        val centerY = size.height / 2f
        val buttonRadius = 65.dp.toPx()

        // === OUTER GLOW LAYERS (large teal aura like mockup) ===

        // Outermost faint glow
        drawCircle(
            brush = Brush.radialGradient(
                colors = if (state == AudioButtonState.DISABLED) listOf(
                    Color.Gray.copy(alpha = 0.03f), Color.Transparent
                ) else listOf(
                    glowColor.copy(alpha = 0.12f),
                    glowColor.copy(alpha = 0.06f),
                    glowColor.copy(alpha = 0.02f),
                    Color.Transparent
                ),
                center = Offset(centerX, centerY),
                radius = buttonRadius * 2.2f
            ),
            radius = buttonRadius * 2.2f,
            center = Offset(centerX, centerY)
        )

        // Outer dotted/dashed circle rings
        val ringColor = if (state == AudioButtonState.DISABLED) Color.Gray.copy(alpha = 0.06f) else glowColor.copy(alpha = 0.12f)
        drawCircle(
            color = ringColor,
            radius = buttonRadius * 1.8f,
            center = Offset(centerX, centerY),
            style = Stroke(width = 0.8.dp.toPx())
        )
        drawCircle(
            color = ringColor.copy(alpha = ringColor.alpha * 0.6f),
            radius = buttonRadius * 1.55f,
            center = Offset(centerX, centerY),
            style = Stroke(width = 0.5.dp.toPx())
        )

        // Inner bright glow ring (strong teal like mockup)
        if (state != AudioButtonState.DISABLED) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        glowColor.copy(alpha = 0.25f),
                        glowColor.copy(alpha = 0.12f),
                        Color.Transparent
                    ),
                    center = Offset(centerX, centerY),
                    radius = buttonRadius * 1.3f
                ),
                radius = buttonRadius * 1.3f,
                center = Offset(centerX, centerY)
            )
        }

        // === MAIN CIRCLE BUTTON (bright white, not gray) ===
        // Shadow behind button
        drawCircle(
            color = Color.Black.copy(alpha = 0.04f),
            radius = buttonRadius + 2.dp.toPx(),
            center = Offset(centerX, centerY + 2.dp.toPx())
        )

        // Main button circle - bright white gradient
        drawCircle(
            brush = Brush.verticalGradient(
                colors = listOf(Color.White, Color(0xFFF0F2F5)),
                startY = centerY - buttonRadius,
                endY = centerY + buttonRadius
            ),
            radius = buttonRadius,
            center = Offset(centerX, centerY)
        )

        // Teal glow ring on the button edge
        if (state != AudioButtonState.DISABLED) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.Transparent,
                        glowColor.copy(alpha = 0.3f),
                        glowColor.copy(alpha = 0.15f)
                    ),
                    center = Offset(centerX, centerY),
                    radius = buttonRadius * 1.1f
                ),
                radius = buttonRadius,
                center = Offset(centerX, centerY),
                style = Stroke(width = 4.dp.toPx())
            )
        } else {
            drawCircle(
                color = Color.Gray.copy(alpha = 0.15f),
                radius = buttonRadius,
                center = Offset(centerX, centerY),
                style = Stroke(width = 2.dp.toPx())
            )
        }

        // Inner subtle white highlight
        drawCircle(
            color = Color.White.copy(alpha = 0.7f),
            radius = buttonRadius - 5.dp.toPx(),
            center = Offset(centerX, centerY - 2.dp.toPx()),
            style = Stroke(width = 1.dp.toPx())
        )

        // === SOUNDWAVE BARS (teal, tall, matching mockup) ===
        val barWidth = 4.dp.toPx()
        val barGap = 6.dp.toPx()
        val barHeights = listOf(28f, 44f, 18f, 40f, 32f)
        val waveColor = if (state == AudioButtonState.DISABLED) Color(0xFFB0B8C1) else accentColor

        // Left side bars
        for (i in barHeights.indices) {
            val x = centerX - buttonRadius - 22.dp.toPx() - (i * (barWidth + barGap))
            val h = barHeights[i].dp.toPx() / 2f
            drawRoundRect(
                color = waveColor.copy(alpha = 0.75f - (i * 0.1f)),
                topLeft = Offset(x - barWidth / 2, centerY - h),
                size = Size(barWidth, h * 2),
                cornerRadius = CornerRadius(barWidth / 2, barWidth / 2)
            )
        }

        // Right side bars (mirror)
        for (i in barHeights.indices) {
            val x = centerX + buttonRadius + 22.dp.toPx() + (i * (barWidth + barGap))
            val h = barHeights[i].dp.toPx() / 2f
            drawRoundRect(
                color = waveColor.copy(alpha = 0.75f - (i * 0.1f)),
                topLeft = Offset(x - barWidth / 2, centerY - h),
                size = Size(barWidth, h * 2),
                cornerRadius = CornerRadius(barWidth / 2, barWidth / 2)
            )
        }

        // === INDICATOR DOT on top ===
        drawCircle(
            color = if (state == AudioButtonState.DISABLED) Color(0xFFB0B8C1).copy(alpha = 0.4f) else glowColor,
            radius = 3.5.dp.toPx(),
            center = Offset(centerX, centerY - buttonRadius - 16.dp.toPx())
        )

        // === PLAY / PAUSE ICON ===
        if (state == AudioButtonState.PLAY || state == AudioButtonState.DISABLED) {
            val triPath = Path().apply {
                moveTo(centerX - 12.dp.toPx(), centerY - 20.dp.toPx())
                lineTo(centerX + 20.dp.toPx(), centerY)
                lineTo(centerX - 12.dp.toPx(), centerY + 20.dp.toPx())
                close()
            }
            drawPath(triPath, color = iconColor)
        } else {
            val pBarW = 9.dp.toPx()
            val pBarH = 32.dp.toPx()
            val pGap = 6.dp.toPx()
            drawRoundRect(
                color = iconColor,
                topLeft = Offset(centerX - pGap - pBarW, centerY - pBarH / 2),
                size = Size(pBarW, pBarH),
                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
            )
            drawRoundRect(
                color = iconColor,
                topLeft = Offset(centerX + pGap, centerY - pBarH / 2),
                size = Size(pBarW, pBarH),
                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
            )
        }
    }
}
