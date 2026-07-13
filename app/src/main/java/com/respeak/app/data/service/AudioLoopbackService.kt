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

package com.respeak.app.data.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.AudioDeviceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.respeak.app.MainActivity
import com.respeak.app.data.audio.AudioDeviceManager
import com.respeak.app.data.audio.AudioEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AudioLoopbackService : Service() {

    companion object {
        const val ACTION_START = "com.respeak.app.action.START"
        const val ACTION_STOP = "com.respeak.app.action.STOP"
        private const val NOTIFICATION_ID = 101
        private const val CHANNEL_ID = "audio_loopback_channel"

        private val _isRunning = MutableStateFlow(false)
        val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

        private val _durationSeconds = MutableStateFlow(0L)
        val durationSeconds: StateFlow<Long> = _durationSeconds.asStateFlow()

        private val _amplitude = MutableStateFlow(0f)
        val amplitude: StateFlow<Float> = _amplitude.asStateFlow()
    }

    private var audioEngine: AudioEngine? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var audioManager: AudioManager? = null
    private var audioDeviceManager: AudioDeviceManager? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var timerJob: Job? = null
    private var amplitudeJob: Job? = null
    private var startTimeMillis = 0L

    override fun onCreate() {
        super.onCreate()
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioDeviceManager = AudioDeviceManager(this)
        audioEngine = AudioEngine(this)

        serviceScope.launch {
            audioDeviceManager?.isHeadsetConnected?.collect { isConnected ->
                if (_isRunning.value && !isConnected) {
                    stopLoopback()
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                startLoopback()
            }
            ACTION_STOP -> {
                stopLoopback()
            }
        }
        return START_NOT_STICKY
    }

    private fun startLoopback() {
        if (_isRunning.value) return

        createNotificationChannel()
        val notification = createNotification("00:00")
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        acquireWakeLock()
        
        audioManager?.mode = AudioManager.MODE_IN_COMMUNICATION
        setupAudioRouting()

        _isRunning.value = true
        startTimeMillis = System.currentTimeMillis()
        
        audioEngine?.start()
        startTimer()
        startAmplitudeMonitoring()
    }

    private fun stopLoopback() {
        if (!_isRunning.value) return

        audioEngine?.stop()
        _amplitude.value = 0f

        timerJob?.cancel()
        amplitudeJob?.cancel()
        _durationSeconds.value = 0L

        releaseWakeLock()

        clearAudioRouting()
        audioManager?.mode = AudioManager.MODE_NORMAL

        _isRunning.value = false
        
        stopForeground(true)
        stopSelf()
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = serviceScope.launch {
            while (_isRunning.value) {
                val elapsed = (System.currentTimeMillis() - startTimeMillis) / 1000
                _durationSeconds.value = elapsed
                updateNotification(formatDuration(elapsed))
                delay(1000)
            }
        }
    }

    private fun startAmplitudeMonitoring() {
        amplitudeJob?.cancel()
        amplitudeJob = serviceScope.launch {
            audioEngine?.amplitude?.collect { amp ->
                _amplitude.value = amp
            }
        }
    }

    private fun formatDuration(seconds: Long): String {
        val mins = seconds / 60
        val secs = seconds % 60
        return String.format("%02d:%02d", mins, secs)
    }

    private fun acquireWakeLock() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "re:speak::AudioLoopbackWakeLock").apply {
            acquire()
        }
    }

    private fun releaseWakeLock() {
        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
        }
        wakeLock = null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "re:speak Audio Loopback",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(serviceChannel)
        }
    }

    private fun createNotification(timeString: String): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val pauseIntent = Intent(this, AudioLoopbackService::class.java).apply {
            action = ACTION_STOP
        }
        val pausePendingIntent = PendingIntent.getService(
            this, 1, pauseIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("re:speak")
            .setContentText("Listening · $timeString")
            .setSmallIcon(android.R.drawable.presence_audio_online)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_media_pause, "Pause", pausePendingIntent)
            .build()
    }

    private fun updateNotification(timeString: String) {
        val notification = createNotification(timeString)
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, notification)
    }

    override fun onDestroy() {
        stopLoopback()
        audioDeviceManager?.release()
        super.onDestroy()
    }

    private fun setupAudioRouting() {
        val am = audioManager ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val devices = am.availableCommunicationDevices
            val targetDevice = devices.firstOrNull {
                it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                        it.type == AudioDeviceInfo.TYPE_BLE_HEADSET
            } ?: devices.firstOrNull {
                it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                        it.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                        it.type == AudioDeviceInfo.TYPE_USB_HEADSET
            }
            if (targetDevice != null) {
                am.setCommunicationDevice(targetDevice)
            }
        } else {
            @Suppress("DEPRECATION")
            if (am.isBluetoothScoAvailableOffCall) {
                am.startBluetoothSco()
                am.isBluetoothScoOn = true
            }
        }
    }

    private fun clearAudioRouting() {
        val am = audioManager ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            am.clearCommunicationDevice()
        } else {
            @Suppress("DEPRECATION")
            if (am.isBluetoothScoOn) {
                am.stopBluetoothSco()
                am.isBluetoothScoOn = false
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
