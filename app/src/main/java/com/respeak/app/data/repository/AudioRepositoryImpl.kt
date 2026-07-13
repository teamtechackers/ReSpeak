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

package com.respeak.app.data.repository

import android.content.Context
import android.content.Intent
import android.os.Build
import com.respeak.app.data.audio.AudioDeviceManager
import com.respeak.app.data.service.AudioLoopbackService
import com.respeak.app.domain.model.LoopbackState
import com.respeak.app.domain.repository.AudioRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class AudioRepositoryImpl(private val context: Context) : AudioRepository {

    private val audioDeviceManager = AudioDeviceManager(context)
    private val scope = CoroutineScope(Dispatchers.Default)

    private val _hasPermission = MutableStateFlow(true)

    override val isHeadsetConnected: StateFlow<Boolean> = audioDeviceManager.isHeadsetConnected
    override val connectedDeviceName: StateFlow<String> = audioDeviceManager.connectedDeviceName

    override val loopbackState: StateFlow<LoopbackState> = combine(
        AudioLoopbackService.isRunning,
        isHeadsetConnected,
        connectedDeviceName,
        _hasPermission
    ) { isRunning, isHeadset, deviceName, hasPermission ->
        when {
            !hasPermission -> LoopbackState.PermissionDenied
            !isHeadset -> LoopbackState.NoHeadphones
            isRunning -> LoopbackState.Active(System.currentTimeMillis(), deviceName)
            else -> LoopbackState.Idle
        }
    }.stateIn(scope, SharingStarted.Eagerly, LoopbackState.Idle)

    override fun startLoopback() {
        val intent = Intent(context, AudioLoopbackService::class.java).apply {
            action = AudioLoopbackService.ACTION_START
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    override fun stopLoopback() {
        val intent = Intent(context, AudioLoopbackService::class.java).apply {
            action = AudioLoopbackService.ACTION_STOP
        }
        context.startService(intent)
    }

    override fun updatePermissionState(granted: Boolean) {
        _hasPermission.value = granted
    }
}
