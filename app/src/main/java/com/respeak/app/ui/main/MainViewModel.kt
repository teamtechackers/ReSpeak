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

package com.respeak.app.ui.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.respeak.app.data.repository.AudioRepositoryImpl
import com.respeak.app.data.service.AudioLoopbackService
import com.respeak.app.domain.model.LoopbackState
import com.respeak.app.domain.repository.AudioRepository
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: AudioRepository = AudioRepositoryImpl(application)

    val loopbackState: StateFlow<LoopbackState> = repository.loopbackState
    val isHeadsetConnected: StateFlow<Boolean> = repository.isHeadsetConnected
    val connectedDeviceName: StateFlow<String> = repository.connectedDeviceName

    val durationSeconds: StateFlow<Long> = AudioLoopbackService.durationSeconds
    val audioAmplitude: StateFlow<Float> = AudioLoopbackService.amplitude

    fun startLoopback() {
        viewModelScope.launch {
            repository.startLoopback()
        }
    }

    fun stopLoopback() {
        viewModelScope.launch {
            repository.stopLoopback()
        }
    }

    fun updatePermissionState(granted: Boolean) {
        repository.updatePermissionState(granted)
    }
}
