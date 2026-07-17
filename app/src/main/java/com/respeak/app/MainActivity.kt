/*
 * re:speak — real-time audio loopback for speech awareness
 * Copyright (C) 2026 Awais
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.respeak.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.respeak.app.domain.model.LoopbackState
import com.respeak.app.ui.about.AboutScreen
import com.respeak.app.ui.dashboard.MainDashboard
import com.respeak.app.ui.main.MainViewModel
import com.respeak.app.ui.onboarding.OnboardingScreen
import com.respeak.app.ui.permission.PermissionScreen
import com.respeak.app.ui.splash.SplashScreen
import com.respeak.app.ui.theme.ReSpeakTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ReSpeakTheme {
                ReSpeakApp()
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

    val sharedPrefs = remember { context.getSharedPreferences("respeak_prefs", android.content.Context.MODE_PRIVATE) }
    var showSplash by remember { mutableStateOf(true) }
    var showOnboarding by remember { mutableStateOf(sharedPrefs.getBoolean("show_onboarding", true)) }
    var showAbout by remember { mutableStateOf(false) }
    var wasActiveBeforeDisconnect by remember { mutableStateOf(false) }

    val permissionsToRequest = remember {
        val list = mutableListOf(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            list.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            list.add(Manifest.permission.BLUETOOTH_CONNECT)
        }
        list.toTypedArray()
    }

    val requestPermissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val audioGranted = permissions[Manifest.permission.RECORD_AUDIO] ?: false
        viewModel.updatePermissionState(audioGranted)
        if (!audioGranted) {
            Toast.makeText(context, "Microphone permission is required.", Toast.LENGTH_LONG).show()
        }
    }

    val micPermissionGranted = ContextCompat.checkSelfPermission(
        context, Manifest.permission.RECORD_AUDIO
    ) == PackageManager.PERMISSION_GRANTED

    LaunchedEffect(micPermissionGranted) {
        viewModel.updatePermissionState(micPermissionGranted)
    }

    LaunchedEffect(loopbackState) {
        if (loopbackState is LoopbackState.Active) {
            wasActiveBeforeDisconnect = true
        }
    }

    val bluetoothPermissionGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        ContextCompat.checkSelfPermission(
            context, Manifest.permission.BLUETOOTH_CONNECT
        ) == PackageManager.PERMISSION_GRANTED
    } else {
        true
    }

    LaunchedEffect(bluetoothPermissionGranted) {
        if (!bluetoothPermissionGranted) {
            requestPermissionsLauncher.launch(permissionsToRequest)
        }
    }

    val isDark = isSystemInDarkTheme()
    val view = LocalView.current
    if (!view.isInEditMode && !showSplash) {
        LaunchedEffect(isDark) {
            val window = (view.context as? android.app.Activity)?.window
            if (window != null) {
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = !isDark
                insetsController.isAppearanceLightNavigationBars = !isDark
            }
        }
    }

    if (showSplash) {
        SplashScreen(onTimeout = { showSplash = false })
    } else {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                if (showOnboarding) {
                    OnboardingScreen(onNext = {
                        sharedPrefs.edit().putBoolean("show_onboarding", false).apply()
                        showOnboarding = false
                    })
                } else if (showAbout) {
                    BackHandler { showAbout = false }
                    AboutScreen(onBack = { showAbout = false })
                } else if (!micPermissionGranted) {
                    PermissionScreen(onRequestPermission = {
                        requestPermissionsLauncher.launch(permissionsToRequest)
                    })
                } else {
                    MainDashboard(
                        viewModel = viewModel,
                        loopbackState = loopbackState,
                        isHeadsetConnected = isHeadsetConnected,
                        onOpenAbout = { showAbout = true },
                        wasActiveBeforeDisconnect = wasActiveBeforeDisconnect,
                        setWasActiveBeforeDisconnect = { wasActiveBeforeDisconnect = it }
                    )
                }
            }
        }
    }
}