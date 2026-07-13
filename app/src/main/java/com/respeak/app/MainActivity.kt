/*
 * re:speak — real-time audio loopback for speech awareness
 * Copyright (C) 2026 Awais
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.respeak.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.respeak.app.domain.model.LoopbackState
import com.respeak.app.ui.main.MainViewModel
import com.respeak.app.ui.theme.ReSpeakTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ReSpeakTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    ReSpeakApp(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun ReSpeakApp(
    modifier: Modifier = Modifier,
    viewModel: MainViewModel = viewModel()
) {
    val context = LocalContext.current

    val loopbackState by viewModel.loopbackState.collectAsStateWithLifecycle()
    val isHeadsetConnected by viewModel.isHeadsetConnected.collectAsStateWithLifecycle()
    val connectedDeviceName by viewModel.connectedDeviceName.collectAsStateWithLifecycle()
    val durationSeconds by viewModel.durationSeconds.collectAsStateWithLifecycle()
    val amplitude by viewModel.audioAmplitude.collectAsStateWithLifecycle()

    var showAboutDialog by remember { mutableStateOf(false) }

    val micPermissionGranted = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.RECORD_AUDIO
    ) == PackageManager.PERMISSION_GRANTED

    LaunchedEffect(micPermissionGranted) {
        viewModel.updatePermissionState(micPermissionGranted)
    }

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        viewModel.updatePermissionState(isGranted)
        if (!isGranted) {
            Toast.makeText(context, "Microphone permission is required.", Toast.LENGTH_LONG).show()
        }
    }

    // Premium Color Palette
    val primaryAccent = Color(0xFF00F5D4) // Glowing neon teal
    val warningColor = Color(0xFFFF9F1C) // Amber warning

    // Vertical gradient matching mockups
    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(Color(0xFF222831), Color(0xFF11141A))
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundBrush)
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Title: re:speak (single colon, elegant clean typography)
            Text(
                text = "re:speak",
                color = Color.White,
                fontSize = 42.sp,
                fontWeight = FontWeight.Light,
                letterSpacing = 1.5.sp,
                modifier = Modifier.padding(top = 32.dp, bottom = 42.dp)
            )

            // Dynamic Status Pill (Glassmorphic layout with back glow)
            val pillText = when {
                !micPermissionGranted -> "Permission Missing"
                !isHeadsetConnected -> "Speaker Mode (Warning)"
                loopbackState is LoopbackState.Active -> "AirPods Live"
                else -> "AirPods Connected"
            }
            
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                // Soft neon back glow behind status pill
                Canvas(modifier = Modifier.size(width = 172.dp, height = 36.dp)) {
                    drawRoundRect(
                        color = primaryAccent.copy(alpha = 0.12f),
                        size = size,
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(100f, 100f)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(100),
                    color = Color(0xFF1E222B).copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, primaryAccent.copy(alpha = 0.35f)),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🎧",
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = pillText,
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Warning Card
            if (!isHeadsetConnected && micPermissionGranted) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = warningColor.copy(alpha = 0.06f)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, warningColor.copy(alpha = 0.15f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Warning",
                            tint = warningColor,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "Please connect earphones. Without them, your mic will pick up the speaker audio and cause feedback.",
                            color = Color.LightGray.copy(alpha = 0.85f),
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }

        // Center Area: Play/Pause Button and Pulsing waveform
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            val isActive = loopbackState is LoopbackState.Active
            val pulseScale by animateFloatAsState(
                targetValue = if (isActive) 1f + amplitude * 0.7f else 1.0f,
                label = "pulse_scale"
            )

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(320.dp)
            ) {
                // Soft Cyan Glow Behind the Button (Radial/Soft back glow)
                Canvas(modifier = Modifier.size(250.dp)) {
                    drawCircle(
                        color = primaryAccent.copy(alpha = if (isActive) 0.18f else 0.08f),
                        radius = size.minDimension / 2.2f
                    )
                }

                // Concentric audio-reactive wave rings (if active)
                if (isActive) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawCircle(
                            color = primaryAccent.copy(alpha = 0.08f * (2f - pulseScale)),
                            radius = (size.minDimension / 2.1f) * pulseScale
                        )
                        drawCircle(
                            color = primaryAccent.copy(alpha = 0.03f * (2f - pulseScale)),
                            radius = (size.minDimension / 1.7f) * (pulseScale * 1.15f)
                        )
                    }
                }

                // Spherical Glassmorphic Play/Pause Button
                Surface(
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.03f),
                    border = BorderStroke(
                        width = 1.5.dp,
                        brush = Brush.sweepGradient(
                            colors = listOf(primaryAccent, primaryAccent.copy(alpha = 0.1f), primaryAccent)
                        )
                    ),
                    modifier = Modifier
                        .size(180.dp)
                        .clickable(enabled = micPermissionGranted) {
                            if (isActive) {
                                viewModel.stopLoopback()
                            } else {
                                viewModel.startLoopback()
                            }
                        }
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        // Drawing glossy highlight and inner neon ring inside button
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val buttonRadius = size.minDimension / 2f

                            // Dark glass base gradient
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(Color(0xFF3E4756).copy(alpha = 0.45f), Color(0xFF161A23).copy(alpha = 0.85f)),
                                    center = center,
                                    radius = buttonRadius
                                ),
                                radius = buttonRadius
                            )

                            // Thin white glass outer border highlight
                            drawCircle(
                                color = Color.White.copy(alpha = 0.1f),
                                radius = buttonRadius - 1.dp.toPx(),
                                style = Stroke(width = 1.dp.toPx())
                            )

                            // Inner Neon Ring
                            drawCircle(
                                color = primaryAccent.copy(alpha = if (isActive) 0.85f else 0.5f),
                                radius = buttonRadius * 0.84f,
                                style = Stroke(width = 2.dp.toPx())
                            )

                            // Glassy reflection arc highlight
                            drawCircle(
                                brush = Brush.verticalGradient(
                                    colors = listOf(Color.White.copy(alpha = 0.18f), Color.Transparent),
                                    startY = 0f,
                                    endY = buttonRadius * 0.8f
                                ),
                                radius = buttonRadius
                            )
                        }

                        // Icon in Center (Pause / Play)
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            if (isActive) {
                                Row(
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(width = 8.dp, height = 34.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color.White)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(width = 8.dp, height = 34.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color.White)
                                    )
                                }
                            } else {
                                Canvas(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .offset(x = 5.dp)
                                ) {
                                    val path = androidx.compose.ui.graphics.Path().apply {
                                        moveTo(0f, 0f)
                                        lineTo(size.width, size.height / 2f)
                                        lineTo(0f, size.height)
                                        close()
                                    }
                                    drawPath(
                                        path = path,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            if (isActive) {
                val minutes = durationSeconds / 60
                val seconds = durationSeconds % 60
                Text(
                    text = String.format("%02d:%02d", minutes, seconds),
                    color = Color.White,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Listening",
                    color = Color.Gray,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            } else {
                Text(
                    text = if (micPermissionGranted) "Tap to start listening" else "Permission required",
                    color = Color.LightGray.copy(alpha = 0.8f),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
                if (!micPermissionGranted) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = { requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                        colors = ButtonDefaults.buttonColors(containerColor = primaryAccent)
                    ) {
                        Text("Grant Mic Permission", color = Color.Black)
                    }
                }
            }
        }

        IconButton(
            onClick = { showAboutDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = "About",
                tint = Color.LightGray,
                modifier = Modifier.size(28.dp)
            )
        }
    }

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = {
                Text("About re:speak", color = Color.White, fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text(
                        text = "re:speak is a real-time low-latency audio loopback app built for speech pattern awareness.",
                        color = Color.LightGray,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Licensed under GNU GPL v3. Free and open-source.",
                        color = Color.Gray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text("Close", color = primaryAccent)
                }
            },
            containerColor = Color(0xFF1E222B),
            titleContentColor = Color.White,
            textContentColor = Color.LightGray
        )
    }
}