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
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.AudioManager
import android.media.AudioDeviceInfo
import android.media.MediaRecorder
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.NoiseSuppressor
import android.media.audiofx.AutomaticGainControl
import android.os.Build
import android.os.Process
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.IOException
import kotlin.math.sqrt

class AudioEngine(private val context: Context) {

    private var audioRecord: AudioRecord? = null
    private var audioTrack: AudioTrack? = null
    private var loopThread: Thread? = null
    private var isLooping = false

    private var echoCanceler: AcousticEchoCanceler? = null
    private var noiseSuppressor: NoiseSuppressor? = null
    private var automaticGainControl: AutomaticGainControl? = null

    private val _amplitude = MutableStateFlow(0f)
    val amplitude: StateFlow<Float> = _amplitude.asStateFlow()

    private var usePhoneMic = true

    fun start(usePhoneMic: Boolean) {
        if (isLooping) return
        this.usePhoneMic = usePhoneMic
        isLooping = true
        loopThread = Thread({ runLoop() }, "AudioLoopbackEngineThread").apply {
            priority = Thread.MAX_PRIORITY
            start()
        }
    }

    fun stop() {
        isLooping = false
        loopThread?.interrupt()
        loopThread = null
        _amplitude.value = 0f
    }

    private fun runLoop() {
        Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO)

        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val nativeSampleRateStr = audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE)
        val sampleRate = nativeSampleRateStr?.toIntOrNull() ?: 48000
        val channelConfigIn = AudioFormat.CHANNEL_IN_MONO
        val channelConfigOut = AudioFormat.CHANNEL_OUT_MONO
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT

        val minBufSizeIn = AudioRecord.getMinBufferSize(sampleRate, channelConfigIn, audioFormat)
        val minBufSizeOut = AudioTrack.getMinBufferSize(sampleRate, channelConfigOut, audioFormat)
        val bufferSize = Math.max(minBufSizeIn, minBufSizeOut) * 2

        // Always use MIC source — VOICE_COMMUNICATION adds heavy system-level
        // AEC/AGC/NS processing that causes 1-3s latency. Bluetooth mic routing
        // is handled by setPreferredDevice() below.
        val audioSource = MediaRecorder.AudioSource.MIC

        try {
            audioRecord = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                AudioRecord.Builder()
                    .setAudioSource(audioSource)
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(audioFormat)
                            .setSampleRate(sampleRate)
                            .setChannelMask(channelConfigIn)
                            .build()
                    )
                    .setBufferSizeInBytes(bufferSize)
                    .build()
            } else {
                @Suppress("DEPRECATION")
                AudioRecord(
                    audioSource,
                    sampleRate,
                    channelConfigIn,
                    audioFormat,
                    bufferSize
                )
            }

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                throw IOException("AudioRecord failed to initialize")
            }

            // Explicitly route AudioRecord to Bluetooth mic when available
            if (!usePhoneMic && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                val inputDevices = am.getDevices(AudioManager.GET_DEVICES_INPUTS)
                val btInputDevice = inputDevices.firstOrNull {
                    it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                            it.type == AudioDeviceInfo.TYPE_BLE_HEADSET
                }
                if (btInputDevice != null) {
                    val routed = audioRecord?.setPreferredDevice(btInputDevice)
                    android.util.Log.d("AudioEngine", "setPreferredDevice to BT mic: type=${btInputDevice.type}, name=${btInputDevice.productName}, success=$routed")
                } else {
                    android.util.Log.w("AudioEngine", "No Bluetooth input device found in system devices list")
                }
            }

            val sessionId = audioRecord?.audioSessionId ?: 0
            if (sessionId != 0) {
                if (usePhoneMic) {
                    // Only enable AEC/NS when using phone mic (speaker feedback risk).
                    if (AcousticEchoCanceler.isAvailable()) {
                        echoCanceler = AcousticEchoCanceler.create(sessionId)
                        echoCanceler?.enabled = true
                    }
                    if (NoiseSuppressor.isAvailable()) {
                        noiseSuppressor = NoiseSuppressor.create(sessionId)
                        noiseSuppressor?.enabled = true
                    }
                } else {
                    // Enable AGC for external headsets to boost low mic input levels
                    if (AutomaticGainControl.isAvailable()) {
                        automaticGainControl = AutomaticGainControl.create(sessionId)
                        automaticGainControl?.enabled = true
                        android.util.Log.d("AudioEngine", "AutomaticGainControl enabled for BT/Headset mic")
                    }
                }
            }

            // Route audio attributes correctly based on active mic.
            // Bluetooth SCO requires VOICE_COMMUNICATION to utilize maximum hardware call volume.
            val usage = if (usePhoneMic) {
                android.media.AudioAttributes.USAGE_MEDIA
            } else {
                android.media.AudioAttributes.USAGE_VOICE_COMMUNICATION
            }

            val streamType = if (usePhoneMic) {
                AudioManager.STREAM_MUSIC
            } else {
                AudioManager.STREAM_VOICE_CALL
            }

            audioTrack = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                AudioTrack.Builder()
                    .setAudioAttributes(
                        android.media.AudioAttributes.Builder()
                            .setUsage(usage)
                            .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(audioFormat)
                            .setSampleRate(sampleRate)
                            .setChannelMask(channelConfigOut)
                            .build()
                    )
                    .setBufferSizeInBytes(bufferSize)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
                    .build()
            } else {
                @Suppress("DEPRECATION")
                AudioTrack(
                    streamType,
                    sampleRate,
                    channelConfigOut,
                    audioFormat,
                    bufferSize,
                    AudioTrack.MODE_STREAM
                )
            }

            if (audioTrack?.state != AudioTrack.STATE_INITIALIZED) {
                throw IOException("AudioTrack failed to initialize")
            }

            // Set AudioTrack volume to maximum
            audioTrack?.setVolume(AudioTrack.getMaxVolume())

            audioRecord?.startRecording()
            audioTrack?.play()

            // Query native buffer size to prevent under/overflow dropouts (missing words)
            val nativeBufferSizeStr = audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER)
            val nativeBufferSize = nativeBufferSizeStr?.toIntOrNull() ?: 256
            val chunkSize = nativeBufferSize.coerceIn(256, 512)
            val buffer = ShortArray(chunkSize)
            val gain = 5.0f // Gain multiplier (complements native AGC)

            android.util.Log.d("AudioEngine", "Starting loop with chunkSize=$chunkSize, gain=$gain")

            while (isLooping && !Thread.currentThread().isInterrupted) {
                val read = audioRecord?.read(buffer, 0, buffer.size) ?: -1
                if (read > 0) {
                    // Apply digital gain boost
                    for (i in 0 until read) {
                        val amplified = (buffer[i] * gain).toInt()
                        buffer[i] = amplified.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                    }
                    audioTrack?.write(buffer, 0, read)
                    calculateAmplitude(buffer, read)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            cleanupAudio()
        }
    }

    private fun calculateAmplitude(buffer: ShortArray, readSize: Int) {
        var sum = 0.0
        for (i in 0 until readSize) {
            sum += buffer[i] * buffer[i]
        }
        val rms = sqrt(sum / readSize)
        val maxVolumeValue = 32768.0
        val normalized = (rms / maxVolumeValue).toFloat().coerceIn(0f, 1f)
        _amplitude.value = normalized
    }

    private fun cleanupAudio() {
        try {
            echoCanceler?.release()
            echoCanceler = null
            noiseSuppressor?.release()
            noiseSuppressor = null
            automaticGainControl?.release()
            automaticGainControl = null

            audioRecord?.stop()
            audioRecord?.release()
            audioRecord = null

            audioTrack?.stop()
            audioTrack?.release()
            audioTrack = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
