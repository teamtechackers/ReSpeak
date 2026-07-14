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
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.respeak.app.R
import com.respeak.app.ui.components.AudioButton
import com.respeak.app.ui.components.AudioButtonState

@Composable
fun StateFocusLostScreen(durationSeconds: Long) {
    val minutes = durationSeconds / 60
    val seconds = durationSeconds % 60
    val timeString = String.format("%02d:%02d", minutes, seconds)

    Box(modifier = Modifier.fillMaxSize().background(Color.White).padding(24.dp)) {
        Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceBetween) {
            // Header logo
            Image(painter = painterResource(id = R.drawable.logo_horizontal), contentDescription = "re:speak horizontal logo", modifier = Modifier.width(220.dp).height(56.dp).padding(top = 8.dp))

            Spacer(modifier = Modifier.height(16.dp))

            // Warning pill
            Surface(shape = RoundedCornerShape(100), color = Color(0xFFFFF5EB), border = BorderStroke(1.dp, Color(0xFFFF9F1C).copy(alpha = 0.15f))) {
                Row(modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Warning, contentDescription = "Warning", tint = Color(0xFFFF9F1C), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Audio Interrupted", color = Color(0xFFFF9F1C), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            // Disabled button - drawn with AudioButton (no white square)
            Box(contentAlignment = Alignment.Center, modifier = Modifier.weight(1f).fillMaxWidth()) {
                Canvas(modifier = Modifier.size(240.dp)) {
                    for (i in 1..3) { drawCircle(color = Color(0xFF042C34).copy(alpha = 0.04f * i), radius = (size.minDimension / 2f) * (i / 3f), style = Stroke(width = 1.dp.toPx())) }
                }
                AudioButton(
                    state = AudioButtonState.DISABLED,
                    accentColor = Color(0xFF042C34)
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(text = timeString, color = Color(0xFFFF9F1C), fontSize = 32.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "Paused", color = Color(0xFFFF9F1C), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "Another app is using audio", color = Color.Gray, fontSize = 14.sp)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Info card
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF5EB).copy(alpha = 0.5f)), shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, Color(0xFFFF9F1C).copy(alpha = 0.1f)), modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Info, contentDescription = "Info", tint = Color(0xFFFF9F1C), modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Audio Focus lost", color = Color(0xFFFF9F1C), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(text = "re:speak is paused because another app needs audio right now.", color = Color(0xFFFF9F1C).copy(alpha = 0.8f), fontSize = 12.sp, lineHeight = 16.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
