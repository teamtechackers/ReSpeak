/*
 * re:speak — real-time audio loopback for speech awareness
 * Copyright (C) 2026 Awais
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.respeak.app.ui.permission

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.respeak.app.R

private var _micIcon: ImageVector? = null
val MicIcon: ImageVector
    get() {
        if (_micIcon != null) return _micIcon!!
        _micIcon = ImageVector.Builder(
            name = "Mic", defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(12f, 14f); curveTo(13.66f, 14f, 15f, 12.66f, 15f, 11f)
                lineTo(15f, 5f); curveTo(15f, 3.34f, 13.66f, 2f, 12f, 2f)
                curveTo(10.34f, 2f, 9f, 3.34f, 9f, 5f); lineTo(9f, 11f)
                curveTo(9f, 12.66f, 10.34f, 14f, 12f, 14f); close()
            }
            path(fill = SolidColor(Color.White)) {
                moveTo(17f, 11f); curveTo(17f, 14f, 14.5f, 16.1f, 12f, 16.1f)
                curveTo(9.5f, 16.1f, 7f, 14f, 7f, 11f); lineTo(5f, 11f)
                curveTo(5f, 14.42f, 7.72f, 17.23f, 11f, 17.72f); lineTo(11f, 21f)
                lineTo(13f, 21f); lineTo(13f, 17.72f)
                curveTo(16.28f, 17.23f, 19f, 14.42f, 19f, 11f); lineTo(17f, 11f); close()
            }
        }.build()
        return _micIcon!!
    }

@Composable
fun PermissionScreen(onRequestPermission: () -> Unit) {
    val isDark = isSystemInDarkTheme()
    val bgColor = if (isDark) Color.Black else Color.White
    val textColor = if (isDark) Color.White else Color.Black
    val textDescColor = if (isDark) Color.LightGray.copy(alpha = 0.8f) else Color.Gray
    val pillBg = if (isDark) Color(0xFF1E222B).copy(alpha = 0.5f) else Color(0xFFE8F1F2)
    val pillBorder = if (isDark) Color(0xFF00F5D4).copy(alpha = 0.25f) else Color(0xFF042C34).copy(alpha = 0.15f)
    val pillTextColor = if (isDark) Color(0xFF00F5D4) else Color(0xFF042C34)
    val circleWaveColor = if (isDark) Color(0xFF00F5D4) else Color(0xFF042C34)
    val illustrationRes = if (isDark) R.drawable.mic_permission_illustration_dark else R.drawable.mic_permission_illustration
    val cardBg = if (isDark) Color(0xFF1E222B).copy(alpha = 0.5f) else Color(0xFFE8F1F2).copy(alpha = 0.5f)
    val cardBorder = if (isDark) Color(0xFF00F5D4).copy(alpha = 0.15f) else Color(0xFF042C34).copy(alpha = 0.1f)
    val cardTextTitle = if (isDark) Color(0xFF00F5D4) else Color(0xFF042C34)
    val cardTextDesc = if (isDark) Color.White.copy(alpha = 0.8f) else Color(0xFF042C34).copy(alpha = 0.8f)
    val buttonColor = Color(0xFF042C34)
    val buttonTextColor = Color.White

    Box(modifier = Modifier.fillMaxSize().background(bgColor).padding(24.dp)) {
        Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceBetween) {
            val logoRes = if (isDark) R.drawable.logo_horizontal_dark else R.drawable.logo_horizontal
            Image(painter = painterResource(id = logoRes), contentDescription = "re:speak horizontal logo", modifier = Modifier.width(220.dp).height(56.dp).padding(top = 8.dp))
            Spacer(modifier = Modifier.height(16.dp))
            Surface(shape = RoundedCornerShape(100), color = pillBg, border = BorderStroke(1.dp, pillBorder)) {
                Row(modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Lock, contentDescription = "Lock", tint = pillTextColor, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Permission Required", color = pillTextColor, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            Box(contentAlignment = Alignment.Center, modifier = Modifier.weight(1f).fillMaxWidth()) {
                Canvas(modifier = Modifier.size(240.dp)) {
                    for (i in 1..3) { drawCircle(color = circleWaveColor.copy(alpha = 0.04f * i), radius = (size.minDimension / 2f) * (i / 3f), style = Stroke(width = 1.dp.toPx())) }
                }
                Image(painter = painterResource(id = illustrationRes), contentDescription = "Microphone illustration with padlock", modifier = Modifier.size(180.dp))
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(text = "Microphone Access Required", color = textColor, fontSize = 22.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(10.dp))
                Text(text = "To play back your voice in real-time, the app requires access to your microphone.", color = textDescColor, fontSize = 14.sp, textAlign = TextAlign.Center, lineHeight = 20.sp, modifier = Modifier.padding(horizontal = 16.dp))
            }
            Spacer(modifier = Modifier.height(24.dp))
            Card(colors = CardDefaults.cardColors(containerColor = cardBg), shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, cardBorder), modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = "Shield", tint = cardTextTitle, modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Your privacy matters", color = cardTextTitle, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(text = "Your voice is processed live on your device and is never recorded or uploaded.", color = cardTextDesc, fontSize = 12.sp, lineHeight = 16.sp)
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onRequestPermission, modifier = Modifier.fillMaxWidth().height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = buttonColor, contentColor = buttonTextColor), shape = RoundedCornerShape(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                    Icon(imageVector = MicIcon, contentDescription = "Mic", tint = buttonTextColor, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Grant Permission", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
