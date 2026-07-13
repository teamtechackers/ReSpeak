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

package com.respeak.app.data.audio

import android.content.Context
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AudioDeviceManager(private val context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private val _isHeadsetConnected = MutableStateFlow(false)
    val isHeadsetConnected: StateFlow<Boolean> = _isHeadsetConnected.asStateFlow()

    private val _connectedDeviceName = MutableStateFlow("Speaker")
    val connectedDeviceName: StateFlow<String> = _connectedDeviceName.asStateFlow()

    private val deviceCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {
            updateDeviceState()
        }

        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
            updateDeviceState()
        }
    }

    init {
        audioManager.registerAudioDeviceCallback(deviceCallback, null)
        updateDeviceState()
    }

    fun release() {
        audioManager.unregisterAudioDeviceCallback(deviceCallback)
    }

    fun updateDeviceState() {
        val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        var hasHeadphones = false
        var deviceName = "Speaker"

        for (device in devices) {
            val type = device.type
            if (isHeadsetType(type)) {
                hasHeadphones = true
                deviceName = getReadableDeviceName(device)
                break
            }
        }

        _isHeadsetConnected.value = hasHeadphones
        _connectedDeviceName.value = deviceName
    }

    private fun isHeadsetType(type: Int): Boolean {
        return type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                type == AudioDeviceInfo.TYPE_USB_HEADSET ||
                type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                type == AudioDeviceInfo.TYPE_BLE_HEADSET ||
                type == AudioDeviceInfo.TYPE_BLE_BROADCAST ||
                type == AudioDeviceInfo.TYPE_HEARING_AID
    }

    private fun getReadableDeviceName(device: AudioDeviceInfo): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && !device.productName.isNullOrBlank()) {
            device.productName.toString()
        } else {
            when (device.type) {
                AudioDeviceInfo.TYPE_WIRED_HEADPHONES, AudioDeviceInfo.TYPE_WIRED_HEADSET -> "Wired Headphones"
                AudioDeviceInfo.TYPE_USB_HEADSET -> "USB Headset"
                AudioDeviceInfo.TYPE_BLUETOOTH_A2DP, AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> "Bluetooth Headset"
                AudioDeviceInfo.TYPE_BLE_HEADSET -> "BLE Headset"
                AudioDeviceInfo.TYPE_BLE_BROADCAST -> "BLE Broadcast"
                AudioDeviceInfo.TYPE_HEARING_AID -> "Hearing Aid"
                else -> "Headset"
            }
        }
    }
}
