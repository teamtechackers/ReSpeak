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

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.respeak.app.R
import com.respeak.app.domain.model.LoopbackState
import com.respeak.app.ui.components.AudioButton
import com.respeak.app.ui.components.AudioButtonState
import com.respeak.app.ui.main.MainViewModel

@Composable
fun MainDashboard(
    viewModel: MainViewModel, loopbackState: LoopbackState, isHeadsetConnected: Boolean,
    onOpenAbout: () -> Unit, wasActiveBeforeDisconnect: Boolean, setWasActiveBeforeDisconnect: (Boolean) -> Unit
) {
    val isSystemDark = isSystemInDarkTheme()
    val isFocusLost = loopbackState is LoopbackState.FocusLost
    val durationSeconds by viewModel.durationSeconds.collectAsStateWithLifecycle()

    if (!isHeadsetConnected && !wasActiveBeforeDisconnect) {
        StateWarningScreen(title = "No Headphones Detected", badgeColor = Color(0xFFFF9F1C), badgeBgColor = Color(0xFFFFF5EB), onContinue = { viewModel.startLoopback(bypassWarning = true) })
    } else if (!isHeadsetConnected && wasActiveBeforeDisconnect) {
        StateWarningScreen(title = "Earphones disconnected", badgeColor = Color(0xFFFF4D4D), badgeBgColor = Color(0xFFFFEBEB), isStateFive = true, onContinue = { viewModel.startLoopback(bypassWarning = true) }, onCancel = { viewModel.stopLoopback(); setWasActiveBeforeDisconnect(false) })
    } else if (isFocusLost) {
        StateFocusLostScreen(durationSeconds = durationSeconds)
    } else {
        DashboardScreen(viewModel = viewModel, loopbackState = loopbackState, isHeadsetConnected = isHeadsetConnected, isDark = isSystemDark, onOpenAbout = onOpenAbout)
    }
}

@Composable
fun DashboardScreen(
    viewModel: MainViewModel, loopbackState: LoopbackState, isHeadsetConnected: Boolean, isDark: Boolean, onOpenAbout: () -> Unit
) {
    val durationSeconds by viewModel.durationSeconds.collectAsStateWithLifecycle()
    val amplitude by viewModel.audioAmplitude.collectAsStateWithLifecycle()
    val usePhoneMic by viewModel.usePhoneMic.collectAsStateWithLifecycle()
    val connectedDeviceName by viewModel.connectedDeviceName.collectAsStateWithLifecycle()
    val isActive = loopbackState is LoopbackState.Active

    val primaryAccent = if (isDark) Color(0xFF00F5D4) else Color(0xFF042C34)
    val textPrimary = if (isDark) Color.White else Color.Black
    val textSecondary = if (isDark) Color.LightGray.copy(alpha = 0.8f) else Color.DarkGray
    val pillBg = if (isDark) Color(0xFF1E222B).copy(alpha = 0.5f) else Color(0xFFE8F1F2)
    val pillBorder = if (isDark) primaryAccent.copy(alpha = 0.35f) else Color(0xFF042C34).copy(alpha = 0.15f)
    val backgroundBrush = if (isDark) Brush.verticalGradient(colors = listOf(Color(0xFF1E222B), Color(0xFF0B0D10))) else Brush.verticalGradient(colors = listOf(Color(0xFFF2F4F7), Color(0xFFE4E7EC)))

    val pulseScale by animateFloatAsState(targetValue = if (isActive) 1f + amplitude * 0.7f else 1.0f, label = "pulse_scale")

    Box(modifier = Modifier.fillMaxSize().background(backgroundBrush).padding(24.dp)) {
        // Top: Logo + Status Pill
        Column(modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter), horizontalAlignment = Alignment.CenterHorizontally) {
            Image(painter = painterResource(id = R.drawable.logo_horizontal), contentDescription = "re:speak horizontal logo", modifier = Modifier.width(220.dp).height(56.dp).padding(top = 8.dp))
            Spacer(modifier = Modifier.height(16.dp))
            val pillText = when {
                !isHeadsetConnected -> "Speaker Mode"
                isActive -> if (connectedDeviceName.isNotEmpty()) "$connectedDeviceName · Live" else "Earphones · Live"
                else -> if (connectedDeviceName.isNotEmpty()) connectedDeviceName else "Earphones Connected"
            }
            Surface(shape = RoundedCornerShape(100), color = pillBg, border = BorderStroke(1.dp, pillBorder)) {
                Row(modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "🎧", fontSize = 14.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = pillText, color = textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }
            }
        }

        // Center: Play/Pause Button
        Column(modifier = Modifier.align(Alignment.Center).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(320.dp)) {
                // Outer pulse glow
                Canvas(modifier = Modifier.size(280.dp)) {
                    drawCircle(
                        color = primaryAccent.copy(alpha = if (isActive) 0.12f else 0.04f),
                        radius = (size.minDimension / 2f) * pulseScale
                    )
                    if (isActive) {
                        drawCircle(
                            color = primaryAccent.copy(alpha = 0.04f * (2f - pulseScale)),
                            radius = (size.minDimension / 1.7f) * pulseScale,
                            style = Stroke(width = 1.5.dp.toPx())
                        )
                    }
                }

                // Custom drawn button (no PNG white square)
                AudioButton(
                    state = if (isActive) AudioButtonState.PAUSE else AudioButtonState.PLAY,
                    accentColor = primaryAccent,
                    onClick = { if (isActive) viewModel.stopLoopback() else viewModel.startLoopback() }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Status text
            if (isActive) {
                val minutes = durationSeconds / 60; val seconds = durationSeconds % 60
                Text(text = String.format("%02d:%02d", minutes, seconds), color = textPrimary, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Text(text = "Listening", color = textSecondary, fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp))
            } else {
                Text(text = "Tap to Start Listening", color = textSecondary, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                Text(text = "Your voice will play back instantly\nthrough your connected earphones.", color = textSecondary.copy(alpha = 0.7f), fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center, lineHeight = 18.sp)
            }
        }

        // Bottom: About icon only (Mic selector hidden for now)
        Column(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            // TODO: Mic selector toggle hidden — uncomment when Headset Mic support is needed
            // Surface(shape = RoundedCornerShape(14.dp), color = pillBg, border = BorderStroke(1.dp, primaryAccent.copy(alpha = 0.15f)), modifier = Modifier.fillMaxWidth().height(52.dp)) {
            //     Row(modifier = Modifier.fillMaxSize().padding(3.dp), verticalAlignment = Alignment.CenterVertically) {
            //         Box(modifier = Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(11.dp)).background(if (usePhoneMic) primaryAccent.copy(alpha = 0.15f) else Color.Transparent).clickable(enabled = !isActive) { viewModel.setUsePhoneMic(true) }, contentAlignment = Alignment.Center) {
            //             Text(text = "Phone Mic", color = if (usePhoneMic) primaryAccent else textSecondary, fontSize = 13.sp, fontWeight = if (usePhoneMic) FontWeight.Bold else FontWeight.Normal)
            //         }
            //         Box(modifier = Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(11.dp)).background(if (!usePhoneMic) primaryAccent.copy(alpha = 0.15f) else Color.Transparent).clickable(enabled = !isActive) { viewModel.setUsePhoneMic(false) }, contentAlignment = Alignment.Center) {
            //             Text(text = "Headset Mic", color = if (!usePhoneMic) primaryAccent else textSecondary, fontSize = 13.sp, fontWeight = if (!usePhoneMic) FontWeight.Bold else FontWeight.Normal)
            //         }
            //     }
            // }
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.Center) {
                IconButton(onClick = onOpenAbout) { Icon(imageVector = Icons.Default.Info, contentDescription = "About", tint = primaryAccent.copy(alpha = 0.7f), modifier = Modifier.size(28.dp)) }
            }
        }
    }
}
