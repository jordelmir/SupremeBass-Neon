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

class AndroidAudioTrackBackend : AudioOutputBackend {

    private var audioTrack: AudioTrack? = null
    private var config: AudioOutputConfig? = null
    private var running = false

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

            val bufferSize = if (config.bufferFrames > 0) {
                val bytesPerFrame = config.channelCount * when (config.encoding) {
                    AudioEncoding.PCM_FLOAT -> 4
                    AudioEncoding.PCM_16BIT -> 2
                }
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

        return try {
            val byteBuffer = java.nio.ByteBuffer.allocate(frames * 4)
                .order(java.nio.ByteOrder.nativeOrder())
            for (i in 0 until frames) {
                byteBuffer.putFloat(buffer[i])
            }
            byteBuffer.flip()
            val written = track.write(byteBuffer, frames * 4, AudioTrack.WRITE_BLOCKING)
            if (written < 0) {
                AppLogger.e("AudioTrack", "Write error: $written")
                AudioBackendResult.Failure(SignalEngineError.WriteFailed)
            } else {
                AudioBackendResult.Success
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
}
