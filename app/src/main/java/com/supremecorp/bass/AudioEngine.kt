package com.supremecorp.bass

import android.content.Context
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.AudioPlaybackConfiguration
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.os.Handler
import android.os.Looper
import com.supremecorp.bass.core.logging.AppLogger

class AudioEngine(private val context: Context) {

    private var loudnessEnhancer: LoudnessEnhancer? = null
    private var equalizer: Equalizer? = null
    private val SESSION_ID = 0

    private val handler = Handler(Looper.getMainLooper())
    private var currentGain = 0
    private var isRunning = false

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    // ── Detect YouTube / any app switching videos ──
    private var lastPlaybackHash = 0
    private val playbackCallback = object : AudioManager.AudioPlaybackCallback() {
        override fun onPlaybackConfigChanged(configs: MutableList<AudioPlaybackConfiguration>?) {
            if (!isRunning) return
            val newHash = configs?.sumOf { it.hashCode() } ?: 0
            if (newHash != lastPlaybackHash) {
                lastPlaybackHash = newHash
                AppLogger.d("Engine", "Playback changed, forcing re-create")
                // Small delay so Android finishes setting up new audio pipeline
                handler.postDelayed({ forceRecreate() }, 150)
            }
        }
    }

    // ── Detect speaker→BT→USB changes ──
    private val deviceCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>) {
            if (isRunning) handler.postDelayed({ forceRecreate() }, 200)
        }
        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>) {
            if (isRunning) handler.postDelayed({ forceRecreate() }, 200)
        }
    }

    // ── Aggressive health check every 1 second ──
    private val healthCheckRunnable = object : Runnable {
        override fun run() {
            if (isRunning) {
                ensureAlive()
                handler.postDelayed(this, 1000)
            }
        }
    }

    fun startSession() {
        isRunning = true
        forceRecreate()
        registerCallbacks()
        handler.removeCallbacks(healthCheckRunnable)
        handler.postDelayed(healthCheckRunnable, 1000)
        AppLogger.i("Engine", "Session started")
    }

    private fun forceRecreate() {
        destroyAll()
        createAll()
        applyGain()
    }

    private fun createAll() {
        // LoudnessEnhancer
        try {
            loudnessEnhancer = LoudnessEnhancer(SESSION_ID).apply {
                enabled = true
                if (currentGain > 0) setTargetGain(currentGain * 60)
            }
            AppLogger.d("Engine", "LoudnessEnhancer created, gain=${currentGain}")
        } catch (e: Exception) {
            AppLogger.e("Engine", "LoudnessEnhancer FAILED: ${e.message}")
        }

        // Equalizer as fallback
        try {
            equalizer = Equalizer(0, SESSION_ID).apply {
                enabled = true
                applyEqBands()
            }
            AppLogger.d("Engine", "Equalizer created")
        } catch (e: Exception) {
            AppLogger.e("Engine", "Equalizer FAILED: ${e.message}")
        }
    }

    private fun applyEqBands() {
        val eq = equalizer ?: return
        if (currentGain <= 0) return
        try {
            val bands = eq.numberOfBands.toInt()
            for (i in 0 until bands) {
                val range = eq.getBandFreqRange(i.toShort())
                val upperHz = range[1]
                val gainDb = when {
                    upperHz <= 300 -> (currentGain * 0.8).toInt().coerceIn(-1200, 1200)
                    upperHz <= 1000 -> (currentGain * 0.3).toInt().coerceIn(-1200, 1200)
                    else -> (currentGain * -0.1).toInt().coerceIn(-1200, 1200)
                }
                eq.setBandLevel(i.toShort(), gainDb.toShort())
            }
        } catch (e: Exception) {
            AppLogger.e("Engine", "applyEqBands failed: ${e.message}")
        }
    }

    private fun destroyAll() {
        try { loudnessEnhancer?.release() } catch (_: Exception) {}
        loudnessEnhancer = null
        try { equalizer?.release() } catch (_: Exception) {}
        equalizer = null
    }

    private fun ensureAlive() {
        val enhancerOk = try {
            val e = loudnessEnhancer
            e != null && e.enabled
        } catch (_: Exception) { false }

        val eqOk = try {
            val e = equalizer
            e != null && e.enabled
        } catch (_: Exception) { false }

        if (!enhancerOk || !eqOk) {
            AppLogger.w("Engine", "Effects dead (le=$enhancerOk, eq=$eqOk), force re-creating")
            forceRecreate()
        }
    }

    private fun registerCallbacks() {
        try {
            audioManager.registerAudioPlaybackCallback(playbackCallback, handler)
            audioManager.registerAudioDeviceCallback(deviceCallback, handler)
        } catch (e: Exception) {
            AppLogger.e("Engine", "Callback register failed: ${e.message}")
        }
    }

    private fun unregisterCallbacks() {
        try {
            audioManager.unregisterAudioPlaybackCallback(playbackCallback)
            audioManager.unregisterAudioDeviceCallback(deviceCallback)
        } catch (_: Exception) {}
    }

    fun setGain(gainValue: Int) {
        currentGain = gainValue
        applyGain()
    }

    private fun applyGain() {
        val boostedGain = currentGain * 60

        try {
            loudnessEnhancer?.setTargetGain(boostedGain)
        } catch (e: Exception) {
            AppLogger.e("Engine", "setTargetGain failed: ${e.message}")
            forceRecreate()
            try { loudnessEnhancer?.setTargetGain(boostedGain) } catch (_: Exception) {}
        }

        try {
            applyEqBands()
        } catch (e: Exception) {
            AppLogger.e("Engine", "applyEqBands failed in setGain: ${e.message}")
        }
    }

    fun stopSession() {
        isRunning = false
        handler.removeCallbacks(healthCheckRunnable)
        unregisterCallbacks()
        destroyAll()
        AppLogger.i("Engine", "Session stopped")
    }

    fun isActive(): Boolean = isRunning
}
