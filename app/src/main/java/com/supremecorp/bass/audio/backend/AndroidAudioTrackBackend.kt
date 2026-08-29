package com.supremecorp.bass.audio.backend

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import com.supremecorp.bass.core.logging.AppLogger
import com.supremecorp.bass.domain.model.AudioBackendResult
import com.supremecorp.bass.domain.model.AudioEncoding
import com.supremecorp.bass.domain.model.AudioOutputConfig
import com.supremecorp.bass.domain.model.SignalEngineError
import java.nio.ByteBuffer
import java.nio.ByteOrder

class AndroidAudioTrackBackend : AudioOutputBackend {

    private var audioTrack: AudioTrack? = null
    private var config: AudioOutputConfig? = null
    private var running = false

    // Pre-allocated write buffer — eliminates per-write allocation
    private var writeBuffer: ByteBuffer? = null
    private var writeBufferSize = 0

    override fun start(config: AudioOutputConfig): AudioBackendResult {
        stop()
        this.config = config

        return try {
            val encoding = when (config.encoding) {
                AudioEncoding.PCM_FLOAT -> AudioFormat.ENCODING_PCM_FLOAT
                AudioEncoding.PCM_16BIT -> AudioFormat.ENCODING_PCM_16BIT
            }

            val channelMask = when (config.channelCount) {
                1 -> AudioFormat.CHANNEL_OUT_MONO
                2 -> AudioFormat.CHANNEL_OUT_STEREO
                else -> AudioFormat.CHANNEL_OUT_MONO
            }

            val minBuffer = AudioTrack.getMinBufferSize(
                config.sampleRate,
                channelMask,
                encoding
            )

            val bytesPerFrame = config.channelCount * when (config.encoding) {
                AudioEncoding.PCM_FLOAT -> 4
                AudioEncoding.PCM_16BIT -> 2
            }

            val bufferSize = if (config.bufferFrames > 0) {
                maxOf(config.bufferFrames * bytesPerFrame, minBuffer)
            } else {
                minBuffer * 2
            }

            val attributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()

            val format = AudioFormat.Builder()
                .setSampleRate(config.sampleRate)
                .setChannelMask(channelMask)
                .setEncoding(encoding)
                .build()

            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(attributes)
                .setAudioFormat(format)
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            // Pre-allocate write buffer
            writeBufferSize = bufferSize
            writeBuffer = ByteBuffer.allocateDirect(bufferSize).order(ByteOrder.nativeOrder())

            audioTrack?.play()
            running = true

            AppLogger.i("AudioTrack", "Started: ${config.sampleRate}Hz, " +
                    "ch=${config.channelCount}, enc=${config.encoding}, " +
                    "buf=${bufferSize}bytes, minBuf=${minBuffer}bytes")

            AudioBackendResult.Success
        } catch (e: Exception) {
            AppLogger.e("AudioTrack", "Start failed: ${e.message}", e)
            running = false
            AudioBackendResult.Failure(SignalEngineError.AudioTrackInitializationFailed)
        }
    }

    override fun write(buffer: FloatArray, frames: Int): AudioBackendResult {
        val track = audioTrack ?: return AudioBackendResult.Failure(
            SignalEngineError.AudioTrackInitializationFailed
        )
        val cfg = config ?: return AudioBackendResult.Failure(
            SignalEngineError.AudioTrackInitializationFailed
        )

        return try {
            val byteBuf = writeBuffer ?: return AudioBackendResult.Failure(
                SignalEngineError.AudioTrackInitializationFailed
            )
            byteBuf.clear()

            val channelCount = cfg.channelCount
            val totalSamples = frames * channelCount

            when (cfg.encoding) {
                AudioEncoding.PCM_FLOAT -> {
                    // Write interleaved float samples
                    for (i in 0 until frames) {
                        for (ch in 0 until channelCount) {
                            val sampleIndex = i * channelCount + ch
                            if (sampleIndex < buffer.size) {
                                byteBuf.putFloat(buffer[sampleIndex])
                            } else {
                                byteBuf.putFloat(0f)
                            }
                        }
                    }
                }
                AudioEncoding.PCM_16BIT -> {
                    // Convert float [-1,1] to int16 [-32768,32767]
                    for (i in 0 until frames) {
                        for (ch in 0 until channelCount) {
                            val sampleIndex = i * channelCount + ch
                            val floatSample = if (sampleIndex < buffer.size) buffer[sampleIndex] else 0f
                            val intSample = (floatSample.coerceIn(-1f, 1f) * 32767f).toInt()
                            byteBuf.putShort(intSample.toShort())
                        }
                    }
                }
            }

            byteBuf.flip()
            val bytesToWrite = byteBuf.remaining()
            val written = track.write(byteBuf, bytesToWrite, AudioTrack.WRITE_BLOCKING)

            when {
                written < 0 -> {
                    AppLogger.e("AudioTrack", "Write error: $written")
                    AudioBackendResult.Failure(SignalEngineError.WriteFailed)
                }
                written < bytesToWrite -> {
                    AppLogger.w("AudioTrack", "Partial write: $written/$bytesToWrite bytes")
                    AudioBackendResult.Success
                }
                else -> AudioBackendResult.Success
            }
        } catch (e: Exception) {
            AppLogger.e("AudioTrack", "Write exception: ${e.message}", e)
            AudioBackendResult.Failure(SignalEngineError.WriteFailed)
        }
    }

    override fun stop(): AudioBackendResult {
        running = false
        return try {
            audioTrack?.stop()
            audioTrack?.release()
            audioTrack = null
            writeBuffer = null
            writeBufferSize = 0
            AppLogger.i("AudioTrack", "Stopped and released")
            AudioBackendResult.Success
        } catch (e: Exception) {
            AppLogger.e("AudioTrack", "Stop error: ${e.message}", e)
            AudioBackendResult.Failure(SignalEngineError.PlatformFailure(e))
        }
    }

    override fun release() {
        stop()
        config = null
    }

    override fun isRunning(): Boolean = running

    override fun getConfig(): AudioOutputConfig? = config

    /** Get the actual routed device after playback starts */
    fun getRoutedDevice(): android.media.AudioDeviceInfo? {
        return try {
            audioTrack?.routedDevice
        } catch (e: Exception) {
            AppLogger.w("AudioTrack", "getRoutedDevice failed: ${e.message}")
            null
        }
    }
}
