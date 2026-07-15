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
import android.util.Log
import android.app.AlarmManager
import android.os.SystemClock
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
        const val EXTRA_USE_PHONE_MIC = "com.respeak.app.extra.USE_PHONE_MIC"
        const val EXTRA_BYPASS_WARNING = "com.respeak.app.extra.BYPASS_WARNING"
        private const val NOTIFICATION_ID = 101
        private const val CHANNEL_ID = "audio_loopback_channel"

        private val _isRunning = MutableStateFlow(false)
        val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

        private val _durationSeconds = MutableStateFlow(0L)
        val durationSeconds: StateFlow<Long> = _durationSeconds.asStateFlow()

        private val _amplitude = MutableStateFlow(0f)
        val amplitude: StateFlow<Float> = _amplitude.asStateFlow()

        private val _audioFocusLost = MutableStateFlow(false)
        val audioFocusLost: StateFlow<Boolean> = _audioFocusLost.asStateFlow()
    }

    private var audioEngine: AudioEngine? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var audioManager: AudioManager? = null
    private var audioDeviceManager: AudioDeviceManager? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var timerJob: Job? = null
    private var amplitudeJob: Job? = null
    private var startTimeMillis = 0L
    private var usePhoneMic = true
    private var bypassWarning = false
    private var focusRequest: android.media.AudioFocusRequest? = null

    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS -> {
                Log.d("ReSpeakService", "Permanent audio focus loss. Stopping loopback.")
                stopLoopback(byFocusLoss = false)
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                Log.d("ReSpeakService", "Transient audio focus lost. Pausing loopback.")
                pauseLoopback()
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                Log.d("ReSpeakService", "Audio focus gained. Resuming loopback.")
                resumeLoopback()
            }
        }
    }

    private fun requestAudioFocus(): Boolean {
        val am = audioManager ?: return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val request = android.media.AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                .setAudioAttributes(
                    android.media.AudioAttributes.Builder()
                        .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setAcceptsDelayedFocusGain(false)
                .setOnAudioFocusChangeListener(audioFocusChangeListener)
                .build()
            focusRequest = request
            am.requestAudioFocus(request) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        } else {
            @Suppress("DEPRECATION")
            am.requestAudioFocus(
                audioFocusChangeListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
            ) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }
    }

    private fun abandonAudioFocus() {
        val am = audioManager ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            focusRequest?.let { am.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            am.abandonAudioFocus(audioFocusChangeListener)
        }
    }

    override fun onCreate() {
        super.onCreate()
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioDeviceManager = AudioDeviceManager(this)
        audioEngine = AudioEngine(this)

        serviceScope.launch {
            audioDeviceManager?.isHeadsetConnected?.collect { isConnected ->
                if (_isRunning.value && !isConnected && !bypassWarning) {
                    // Debounce check for 1.5 seconds to prevent transient background disconnect triggers
                    delay(1500)
                    if (audioDeviceManager?.isHeadsetConnected?.value == false && !bypassWarning) {
                        Log.d("ReSpeakService", "Headset disconnection confirmed. Stopping loopback.")
                        stopLoopback(byFocusLoss = false)
                    }
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val usePhone = intent.getBooleanExtra(EXTRA_USE_PHONE_MIC, true)
                bypassWarning = intent.getBooleanExtra(EXTRA_BYPASS_WARNING, false)
                startLoopback(usePhone)
            }
            ACTION_STOP -> {
                stopLoopback()
            }
        }
        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        Log.d("ReSpeakService", "App task removed (swiped away from recents). Scheduling service restart...")
        if (_isRunning.value) {
            val restartServiceIntent = Intent(applicationContext, this.javaClass).apply {
                action = ACTION_START
                putExtra(EXTRA_USE_PHONE_MIC, usePhoneMic)
            }
            val restartServicePendingIntent = PendingIntent.getService(
                applicationContext, 1, restartServiceIntent,
                PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
            )
            val alarmService = applicationContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmService.set(
                AlarmManager.ELAPSED_REALTIME,
                SystemClock.elapsedRealtime() + 1000,
                restartServicePendingIntent
            )
        }
        super.onTaskRemoved(rootIntent)
    }

    private fun startLoopback(usePhoneMic: Boolean) {
        if (_isRunning.value) {
            if (_audioFocusLost.value) {
                resumeLoopback()
            }
            return
        }

        this.usePhoneMic = usePhoneMic

        if (!requestAudioFocus()) {
            Log.d("ReSpeakService", "startLoopback failed: could not obtain audio focus.")
            _audioFocusLost.value = true
            return
        }
        _audioFocusLost.value = false

        createNotificationChannel()
        val notification = createNotification("00:00")
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK or
                        android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        acquireWakeLock()
        
        if (usePhoneMic) {
            audioManager?.mode = AudioManager.MODE_NORMAL
        } else {
            audioManager?.mode = AudioManager.MODE_IN_COMMUNICATION
            setupAudioRouting()
        }

        _isRunning.value = true
        startTimeMillis = System.currentTimeMillis()
        
        Log.d("ReSpeakService", "startLoopback: service running in foreground. usePhoneMic=$usePhoneMic")
        audioEngine?.start(usePhoneMic)
        startTimer()
        startAmplitudeMonitoring()
    }

    private fun stopLoopback(byFocusLoss: Boolean = false) {
        if (!_isRunning.value) return
        bypassWarning = false

        audioEngine?.stop()
        _amplitude.value = 0f

        timerJob?.cancel()
        amplitudeJob?.cancel()
        _durationSeconds.value = 0L

        releaseWakeLock()
        abandonAudioFocus()

        if (!usePhoneMic) {
            clearAudioRouting()
        }
        audioManager?.mode = AudioManager.MODE_NORMAL

        _isRunning.value = false
        _audioFocusLost.value = false
        Log.d("ReSpeakService", "stopLoopback: service stopped loopback.")
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
    }

    private fun pauseLoopback() {
        if (!_isRunning.value) return
        audioEngine?.stop()
        _amplitude.value = 0f
        timerJob?.cancel()
        amplitudeJob?.cancel()
        _audioFocusLost.value = true
        
        val elapsed = (System.currentTimeMillis() - startTimeMillis) / 1000
        updateNotification("Paused · ${formatDuration(elapsed)}")
        Log.d("ReSpeakService", "pauseLoopback: service paused loopback.")
    }

    private fun resumeLoopback() {
        if (!_isRunning.value || !_audioFocusLost.value) return
        if (!requestAudioFocus()) {
            Log.d("ReSpeakService", "resumeLoopback failed: could not regain audio focus.")
            return
        }
        _audioFocusLost.value = false
        startTimeMillis = System.currentTimeMillis() - (_durationSeconds.value * 1000)
        
        if (usePhoneMic) {
            audioManager?.mode = AudioManager.MODE_NORMAL
        } else {
            audioManager?.mode = AudioManager.MODE_IN_COMMUNICATION
            setupAudioRouting()
        }
        
        audioEngine?.start(usePhoneMic)
        startTimer()
        startAmplitudeMonitoring()
        Log.d("ReSpeakService", "resumeLoopback: service resumed loopback.")
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
