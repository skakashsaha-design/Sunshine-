package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.example.domain.model.AmbientSoundType
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/**
 * Self-contained procedural ambient weather audio generator using Android AudioTrack.
 * Uses zero copyrighted recordings, defaults to OFF, respects volume and auto-stop timer.
 */
class WeatherSoundEngine {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var playJob: Job? = null
    private var timerJob: Job? = null
    private var audioTrack: AudioTrack? = null

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _activeSound = MutableStateFlow(AmbientSoundType.NONE)
    val activeSound: StateFlow<AmbientSoundType> = _activeSound.asStateFlow()

    @Volatile
    private var currentVolume: Float = 0.45f

    fun setVolume(vol: Float) {
        currentVolume = vol.coerceIn(0f, 1f)
        runCatching { audioTrack?.setVolume(currentVolume) }
    }

    fun start(type: AmbientSoundType, volume: Float, autoStopMinutes: Int) {
        stop()
        if (type == AmbientSoundType.NONE) return
        currentVolume = volume.coerceIn(0f, 1f)
        _activeSound.value = type
        _isPlaying.value = true

        playJob = scope.launch {
            val sampleRate = 22050
            val minBuf = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            ).coerceAtLeast(4096)

            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(minBuf)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            audioTrack = track
            track.setVolume(currentVolume)
            track.play()

            val buffer = ShortArray(1024)
            var lp1 = 0.0
            var lp2 = 0.0
            var phase = 0.0
            var thunderEnv = 0.0
            var sampleIdx = 0L

            while (isActive) {
                for (i in buffer.indices) {
                    val white = Random.nextDouble(-1.0, 1.0)
                    val sample = when (type) {
                        AmbientSoundType.RAIN -> {
                            lp1 = lp1 * 0.85 + white * 0.15
                            val droplet = if (Random.nextFloat() < 0.002f) white * 0.6 else 0.0
                            (lp1 * 0.7 + droplet) * 0.55
                        }
                        AmbientSoundType.THUNDER -> {
                            lp1 = lp1 * 0.88 + white * 0.12
                            lp2 = lp2 * 0.994 + white * 0.006
                            if (sampleIdx % (sampleRate * 9L) == 0L) thunderEnv = 1.0
                            thunderEnv *= 0.99992
                            (lp1 * 0.45 + lp2 * 3.8 * thunderEnv) * 0.55
                        }
                        AmbientSoundType.WIND -> {
                            phase += 0.00018
                            val gust = 0.45 + 0.55 * (0.5 + 0.5 * sin(phase))
                            lp1 = lp1 * 0.94 + white * 0.06
                            lp1 * gust * 0.95
                        }
                        AmbientSoundType.WINTER -> {
                            phase += 0.00025
                            val shimmer = 0.6 + 0.4 * sin(phase)
                            lp1 = lp1 * 0.91 + white * 0.09
                            lp1 * shimmer * 0.45
                        }
                        AmbientSoundType.CALM -> {
                            phase += 2.0 * PI * 174.0 / sampleRate
                            val tone = 0.12 * sin(phase) + 0.08 * sin(phase * 1.5)
                            lp1 = lp1 * 0.96 + white * 0.04
                            tone + lp1 * 0.18
                        }
                        AmbientSoundType.NONE -> 0.0
                    }
                    buffer[i] = (sample.coerceIn(-1.0, 1.0) * Short.MAX_VALUE).toInt().toShort()
                    sampleIdx++
                }
                track.write(buffer, 0, buffer.size)
            }
        }

        if (autoStopMinutes > 0) {
            timerJob = scope.launch {
                delay(autoStopMinutes * 60_000L)
                stop()
            }
        }
    }

    fun stop() {
        timerJob?.cancel()
        timerJob = null
        playJob?.cancel()
        playJob = null
        runCatching {
            audioTrack?.pause()
            audioTrack?.flush()
            audioTrack?.release()
        }
        audioTrack = null
        _isPlaying.value = false
        _activeSound.value = AmbientSoundType.NONE
    }
}
