/*
 * re:speak — real-time audio loopback for speech awareness
 * Copyright (C) 2026 Awais
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.respeak.app.ui.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.respeak.app.ui.components.AudioButton
import com.respeak.app.ui.components.AudioButtonState
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.respeak.app.R

@Composable
fun StateWarningScreen(
    title: String, badgeColor: Color, badgeBgColor: Color,
    isStateFive: Boolean = false, onContinue: () -> Unit, onCancel: () -> Unit = {}
) {
    val isDark = isSystemInDarkTheme()
    
    // Dynamic theme mapping matching the mockup screenshots
    val bgColor = if (isDark) Color.Black else Color.White
    val textPrimary = if (isDark) Color.White else Color.Black
    val textSecondary = if (isDark) Color.LightGray.copy(alpha = 0.8f) else Color.Gray
    
    // Warning pill colors adjusted to match dark background contrast if in dark theme
    val finalBadgeBg = if (isDark) badgeColor.copy(alpha = 0.15f) else badgeBgColor
    val finalBadgeBorder = if (isDark) badgeColor.copy(alpha = 0.35f) else badgeColor.copy(alpha = 0.15f)

    // Button colors matching mockup (dark teal slate or bright border)
    val primaryButtonColor = Color(0xFF042C34)
    val primaryButtonTextColor = Color.White
    val outlineBtnColor = if (isDark) Color.White else Color(0xFF042C34)

    Box(modifier = Modifier.fillMaxSize().background(bgColor).padding(24.dp)) {
        Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceBetween) {
            // Header logo
            val logoRes = if (isDark) R.drawable.logo_horizontal_dark else R.drawable.logo_horizontal
            Image(painter = painterResource(id = logoRes), contentDescription = "re:speak horizontal logo", modifier = Modifier.width(220.dp).height(56.dp).padding(top = 8.dp))

            Spacer(modifier = Modifier.height(16.dp))

            // Warning Pill
            Surface(shape = RoundedCornerShape(100), color = finalBadgeBg, border = BorderStroke(1.dp, finalBadgeBorder)) {
                Row(modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Warning, contentDescription = "Warning", tint = badgeColor, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = title, color = badgeColor, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            // Disabled button - drawn with AudioButton (no white square)
            Box(contentAlignment = Alignment.Center, modifier = Modifier.weight(1f).fillMaxWidth()) {
                val waveIndicatorColor = if (isDark) Color(0xFF00F5D4) else Color(0xFF042C34)
                Canvas(modifier = Modifier.size(240.dp)) {
                    for (i in 1..3) { drawCircle(color = waveIndicatorColor.copy(alpha = 0.04f * i), radius = (size.minDimension / 2f) * (i / 3f), style = Stroke(width = 1.dp.toPx())) }
                }
                AudioButton(
                    state = AudioButtonState.DISABLED,
                    accentColor = waveIndicatorColor
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(text = "Connect Earphones", color = textPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(10.dp))
                Text(text = "Without earphones, the microphone will hear the phone speaker. This creates a horrible feedback sound.", color = textSecondary, fontSize = 14.sp, textAlign = TextAlign.Center, lineHeight = 20.sp, modifier = Modifier.padding(horizontal = 16.dp))
            }

            Spacer(modifier = Modifier.height(32.dp))

            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                if (!isStateFive) {
                    Button(onClick = onContinue, modifier = Modifier.fillMaxWidth().height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = primaryButtonColor, contentColor = primaryButtonTextColor), shape = RoundedCornerShape(14.dp)) { Text("Continue Anyway", fontSize = 16.sp, fontWeight = FontWeight.Bold) }
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(onClick = {}, modifier = Modifier.fillMaxWidth().height(56.dp), border = BorderStroke(1.5.dp, outlineBtnColor), colors = ButtonDefaults.outlinedButtonColors(contentColor = outlineBtnColor), shape = RoundedCornerShape(14.dp)) { Text("Reconnect", fontSize = 16.sp, fontWeight = FontWeight.Bold) }
                } else {
                    Button(onClick = onContinue, modifier = Modifier.fillMaxWidth().height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = primaryButtonColor, contentColor = primaryButtonTextColor), shape = RoundedCornerShape(14.dp)) { Text("Reconnect", fontSize = 16.sp, fontWeight = FontWeight.Bold) }
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth().height(56.dp), border = BorderStroke(1.5.dp, Color(0xFFFF4D4D)), colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF4D4D)), shape = RoundedCornerShape(14.dp)) { Text("Cancel Session", fontSize = 16.sp, fontWeight = FontWeight.Bold) }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
