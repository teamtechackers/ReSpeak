/*
 * re:speak — real-time audio loopback for speech awareness
 * Copyright (C) 2026 Awais
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.respeak.app.ui.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.respeak.app.R

@Composable
fun OnboardingScreen(onNext: () -> Unit) {
    val isDark = isSystemInDarkTheme()
    var currentSlide by remember { mutableStateOf(0) }

    val lightImages = listOf(R.drawable.onboarding_1, R.drawable.onboarding_2, R.drawable.onboarding_3)
    val darkImages = listOf(R.drawable.onboarding_1_dark, R.drawable.onboarding_2_dark, R.drawable.onboarding_3_dark)
    val images = if (isDark) darkImages else lightImages

    val headings = listOf(
        "Hear Yourself\nin Real Time",
        "Use Earphones to\nAvoid Feedback",
        "Reflect . Improve\n. Grow"
    )
    val descriptions = listOf(
        "re:speak plays your voice back to you instantly so you can become more aware of your speech.",
        "Wearing earphones prevents the mic from picking up the playback and ensures a clear, feedback-free experience.",
        "Listen to your patterns, build confidence, and communicate with more clarity."
    )

    val bgColor = if (isDark) Color.Black else Color.White
    val textColor = if (isDark) Color.White else Color.Black
    val textDescColor = if (isDark) Color.LightGray.copy(alpha = 0.8f) else Color.Gray
    val buttonColor = Color(0xFF042C34)
    val buttonTextColor = Color.White
    val indicatorActiveColor = if (isDark) Color(0xFF00F5D4) else Color(0xFF042C34)
    val indicatorInactiveColor = if (isDark) Color.Gray.copy(alpha = 0.5f) else Color.LightGray

    Box(
        modifier = Modifier.fillMaxSize().background(bgColor).padding(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            Image(
                painter = painterResource(id = images[currentSlide]),
                contentDescription = "Onboarding Image",
                modifier = Modifier.weight(1f).fillMaxWidth(0.9f)
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(text = headings[currentSlide], color = textColor, fontSize = 28.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, lineHeight = 36.sp)
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = descriptions[currentSlide], color = textDescColor, fontSize = 15.sp, textAlign = TextAlign.Center, lineHeight = 22.sp, modifier = Modifier.padding(horizontal = 16.dp))
            }
            Spacer(modifier = Modifier.height(32.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    for (i in 0..2) {
                        val isActive = i == currentSlide
                        Box(modifier = Modifier.padding(horizontal = 4.dp).size(if (isActive) 10.dp else 8.dp).clip(CircleShape).background(if (isActive) indicatorActiveColor else indicatorInactiveColor))
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
                Button(
                    onClick = { if (currentSlide < 2) currentSlide++ else onNext() },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = buttonColor, contentColor = buttonTextColor),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(text = if (currentSlide == 2) "Get Started" else "Next", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
