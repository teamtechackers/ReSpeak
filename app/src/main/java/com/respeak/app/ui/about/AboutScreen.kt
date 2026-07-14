/*
 * re:speak — real-time audio loopback for speech awareness
 * Copyright (C) 2026 Awais
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.respeak.app.ui.about

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.respeak.app.R

@Composable
fun AboutScreen(onBack: () -> Unit) {
    val isDark = isSystemInDarkTheme()
    val uriHandler = LocalUriHandler.current
    val bgColor = if (isDark) Color.Black else Color.White
    val textColor = if (isDark) Color.White else Color.Black
    val textSecondary = if (isDark) Color.LightGray else Color.Gray
    val containerBg = if (isDark) Color(0xFF1E222B) else Color(0xFFF8F9FA)
    val borderCol = if (isDark) Color(0xFF00F5D4).copy(alpha = 0.15f) else Color.LightGray.copy(alpha = 0.3f)
    val brandTagColor = if (isDark) Color(0xFF00F5D4) else Color(0xFF042C34)

    Box(modifier = Modifier.fillMaxSize().background(bgColor).padding(24.dp)) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), contentAlignment = Alignment.CenterStart) {
                IconButton(onClick = onBack) { Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = textColor) }
                Text(text = "re:speak", color = textColor, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            }
            Spacer(modifier = Modifier.height(32.dp))
            val logoRes = if (isDark) R.drawable.logo_horizontal_dark else R.drawable.logo_horizontal
            Image(painter = painterResource(id = logoRes), contentDescription = "re:speak logo horizontal", modifier = Modifier.width(220.dp).height(56.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Hear yourself . Improve yourself", color = brandTagColor, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(32.dp))
            Card(colors = CardDefaults.cardColors(containerColor = containerBg), shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, borderCol), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "About re:speak", color = textColor, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(text = "re:speak is a real-time low-latency audio loopback app built for speech pattern awareness.", color = textSecondary, fontSize = 14.sp, lineHeight = 20.sp)
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(containerBg).padding(vertical = 8.dp)) {
                AboutListRow(label = "Version", value = "1.0.0", isDark = isDark)
                HorizontalDivider(color = borderCol)
                AboutListRow(label = "License", value = "GNU GPL v3", isDark = isDark)
                HorizontalDivider(color = borderCol)
                AboutListRow(label = "GitHub", clickable = true, isDark = isDark, onClick = { uriHandler.openUri("https://github.com/GhulamShahbazali/ReSpeak") })
                HorizontalDivider(color = borderCol)
                AboutListRow(label = "Report an issue", clickable = true, isDark = isDark, onClick = { uriHandler.openUri("https://github.com/GhulamShahbazali/ReSpeak/issues") })
                HorizontalDivider(color = borderCol)
                AboutListRow(label = "Privacy", clickable = true, isDark = isDark, onClick = { uriHandler.openUri("https://github.com/GhulamShahbazali/ReSpeak/blob/main/PRIVACY.md") })
            }
            Spacer(modifier = Modifier.height(48.dp))
            Text(text = "© 2026 re:speak\nLicensed under GNU GPL v3", color = textSecondary.copy(alpha = 0.7f), fontSize = 12.sp, textAlign = TextAlign.Center, lineHeight = 18.sp, modifier = Modifier.padding(bottom = 24.dp))
        }
    }
}

@Composable
fun AboutListRow(label: String, value: String? = null, clickable: Boolean = false, isDark: Boolean = false, onClick: () -> Unit = {}) {
    val textColor = if (isDark) Color.White else Color.Black
    val textSecondary = if (isDark) Color.LightGray else Color.Gray
    Row(modifier = Modifier.fillMaxWidth().clickable(enabled = clickable, onClick = onClick).padding(horizontal = 16.dp, vertical = 14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(text = label, color = textColor, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        if (value != null) { Text(text = value, color = textSecondary, fontSize = 14.sp) }
        else { Icon(imageVector = Icons.Default.Share, contentDescription = "Link", tint = textSecondary, modifier = Modifier.size(16.dp)) }
    }
}
