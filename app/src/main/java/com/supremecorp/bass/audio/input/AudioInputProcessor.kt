package com.supremecorp.bass.audio.input

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import androidx.core.content.ContextCompat
import com.supremecorp.bass.core.logging.AppLogger
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Captures audio from the device microphone for acoustic measurements.
 * Returns raw PCM float samples for FFT analysis.
 *
 * Requirements:
 * - RECORD_AUDIO permission
 * - AudioRecord API (not deprecated MediaRecorder for this use case)
 */
class AudioInputProcessor(private val context: Context) {

    companion object {
        private const val TAG = "AudioInputProcessor"
        private const val SAMPLE_RATE = 44100
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_FLOAT
    }

    private var audioRecord: AudioRecord? = null
    private val isRecording = AtomicBoolean(false)
    private var recordingThread: Thread? = null

    // Buffer for audio input
    private var inputBuffer: FloatArray? = null
    private var bufferSize: Int = 0

    // Callback for processed audio data
    var onAudioData: ((FloatArray, Int) -> Unit)? = null

    fun isRecording(): Boolean = isRecording.get()

    fun hasPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun start(): Boolean {
        if (isRecording.get()) {
            Log.w(TAG, "Already recording")
            return false
        }

        if (!hasPermission()) {
            Log.e(TAG, "RECORD_AUDIO permission not granted")
            return false
        }

        return try {
            val minBufferSize = AudioRecord.getMinBufferSize(
                SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT
            )

            if (minBufferSize == AudioRecord.ERROR || minBufferSize == AudioRecord.ERROR_BAD_VALUE) {
                Log.e(TAG, "Invalid buffer size: $minBufferSize")
                return false
            }

            bufferSize = minBufferSize * 2 // Double buffer for smooth recording
            inputBuffer = FloatArray(bufferSize / 4) // Float = 4 bytes

            audioRecord = AudioRecord.Builder()
                .setAudioSource(MediaRecorder.AudioSource.MIC)
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(CHANNEL_CONFIG)
                        .setEncoding(AUDIO_FORMAT)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize)
                .build()

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "AudioRecord failed to initialize")
                audioRecord?.release()
                audioRecord = null
                return false
            }

            audioRecord?.startRecording()
            isRecording.set(true)

            recordingThread = Thread({
                recordLoop()
            }, "AudioInput-Record").apply {
                priority = Thread.MAX_PRIORITY
                start()
            }

            AppLogger.i(TAG, "Started: ${SAMPLE_RATE}Hz, buffer=${bufferSize}bytes")
            true
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception: ${e.message}")
            false
        } catch (e: Exception) {
            Log.e(TAG, "Start failed: ${e.message}", e)
            false
        }
    }

    fun stop() {
        if (!isRecording.get()) return

        isRecording.set(false)
        recordingThread?.join(1000)
        recordingThread = null

        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) {
            Log.w(TAG, "Stop error: ${e.message}")
        }
        audioRecord = null
        inputBuffer = null

        AppLogger.i(TAG, "Stopped")
    }

    /**
     * Read a block of audio samples (blocking).
     * Returns the number of samples read, or -1 on error.
     */
    fun read(buffer: FloatArray): Int {
        val track = audioRecord ?: return -1
        if (!isRecording.get()) return -1

        return try {
            val samplesRead = track.read(buffer, 0, buffer.size, AudioRecord.READ_BLOCKING)
            if (samplesRead > 0) {
                onAudioData?.invoke(buffer, samplesRead)
            }
            samplesRead
        } catch (e: Exception) {
            Log.e(TAG, "Read error: ${e.message}")
            -1
        }
    }

    /**
     * Read exactly N samples (blocking until complete or error).
     */
    fun readExact(count: Int): FloatArray? {
        val buffer = FloatArray(count)
        var totalRead = 0

        while (totalRead < count && isRecording.get()) {
            val tempBuffer = FloatArray(count - totalRead)
            val read = read(tempBuffer)
            if (read <= 0) break
            System.arraycopy(tempBuffer, 0, buffer, totalRead, read)
            totalRead += read
        }

        return if (totalRead == count) buffer else null
    }

    fun getSampleRate(): Int = SAMPLE_RATE

    private fun recordLoop() {
        val readBuffer = FloatArray(1024)

        while (isRecording.get()) {
            val samplesRead = read(readBuffer)
            if (samplesRead > 0) {
                onAudioData?.invoke(readBuffer, samplesRead)
            }
        }
    }
}
