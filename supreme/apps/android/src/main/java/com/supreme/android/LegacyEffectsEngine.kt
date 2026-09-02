package com.supreme.android

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.AudioPlaybackConfiguration
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.os.Handler
import android.os.HandlerThread
import android.os.PowerManager
import android.util.Log
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class LegacyEffectsEngine(private val context: Context) {

    private companion object {
        const val TAG = "SupremeBass_Engine"

        // Gain mapping: UI slider 0..300 → dB 0..12 → mB 0..1200
        // LoudnessEnhancer.setTargetGain() takes millibels (mB), where 100 mB = 1 dB
        const val MAX_GAIN_DB = 12f
        const val MAX_SLIDER_VALUE = 300
        const val MB_PER_DB = 100
    }

    private var loudnessEnhancer: LoudnessEnhancer? = null
    private var equalizer: Equalizer? = null
    private val SESSION_ID = 0

    private var currentGain = 0
    @Volatile private var isRunning = false
    private var recreateCount = 0
    private var lastRecreateMs = 0L

    /** Convert UI gain (0..300) to millibels for LoudnessEnhancer */
    private fun gainToMillibel(gainValue: Int): Int {
        val db = (gainValue.toFloat() / MAX_SLIDER_VALUE) * MAX_GAIN_DB
        return (db * MB_PER_DB).toInt().coerceIn(0, (MAX_GAIN_DB * MB_PER_DB).toInt())
    }

    /** Convert UI gain (0..300) to display dB */
    fun gainToDb(gainValue: Int): Float {
        return (gainValue.toFloat() / MAX_SLIDER_VALUE) * MAX_GAIN_DB
    }

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager

    // Dedicated background thread for callbacks
    private val bgThread = HandlerThread("SupremeBass_EngineThread").apply { start() }
    private val bgHandler = Handler(bgThread.looper)

    // Aggressive polling scheduler — independent of Android Handler system
    private val scheduler: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()
    private var aggressivePollFuture: ScheduledFuture<*>? = null
    private val isScreenOff = AtomicBoolean(false)

    // Track playback changes
    @Volatile private var lastPlaybackConfigHash = 0
    @Volatile private var lastPlaybackConfigSize = 0

    // ── Playback callback — fires when YouTube changes video ──
    private val playbackCallback = object : AudioManager.AudioPlaybackCallback() {
        override fun onPlaybackConfigChanged(configs: MutableList<AudioPlaybackConfiguration>?) {
            if (!isRunning) return

            val newHash = configs?.sumOf { it.hashCode() } ?: 0
            val newSize = configs?.size ?: 0

            if (newHash != lastPlaybackConfigHash || newSize != lastPlaybackConfigSize) {
                lastPlaybackConfigHash = newHash
                lastPlaybackConfigSize = newSize

                Log.i(TAG, "Playback changed: hash=$newHash size=$newSize screen=${powerManager.isInteractive}")

                // Immediate recreate on background thread
                bgHandler.postDelayed({
                    if (isRunning) {
                        Log.d(TAG, "Recreating effects after playback change")
                        forceRecreate()
                    }
                }, 100)
            }
        }
    }

    private val deviceCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>) {
            if (isRunning) {
                Log.i(TAG, "Devices added: ${addedDevices.map { it.productName }}")
                bgHandler.postDelayed({ forceRecreate() }, 200)
            }
        }
        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>) {
            if (isRunning) {
                Log.i(TAG, "Devices removed: ${removedDevices.map { it.productName }}")
                bgHandler.postDelayed({ forceRecreate() }, 200)
            }
        }
    }

    // ── Screen state receiver — detect screen ON/OFF ──
    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    Log.i(TAG, "Screen OFF — starting aggressive polling")
                    isScreenOff.set(true)
                    startAggressivePolling()
                }
                Intent.ACTION_SCREEN_ON -> {
                    Log.i(TAG, "Screen ON — stopping aggressive polling")
                    isScreenOff.set(false)
                    stopAggressivePolling()
                }
            }
        }
    }

    private var screenReceiverRegistered = false

    fun startSession() {
        isRunning = true
        recreateCount = 0
        lastRecreateMs = System.currentTimeMillis()

        // Initialize tracking
        try {
            val initialConfigs = audioManager.activePlaybackConfigurations
            lastPlaybackConfigHash = initialConfigs.sumOf { it.hashCode() }
            lastPlaybackConfigSize = initialConfigs.size
            Log.i(TAG, "Initial: ${initialConfigs.size} configs, screen=${powerManager.isInteractive}")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get initial configs: ${e.message}")
        }

        forceRecreate()
        registerCallbacks()
        registerScreenReceiver()

        // If screen is already off, start aggressive polling
        if (!powerManager.isInteractive) {
            isScreenOff.set(true)
            startAggressivePolling()
        }

        Log.i(TAG, "Session started")
    }

    // ── Aggressive polling — re-creates effects every 300ms when screen is off ──
    private fun startAggressivePolling() {
        stopAggressivePolling()
        Log.d(TAG, "Starting aggressive poll (300ms)")

        aggressivePollFuture = scheduler.scheduleWithFixedDelay({
            if (!isRunning || !isScreenOff.get()) {
                stopAggressivePolling()
                return@scheduleWithFixedDelay
            }

            try {
                ensureAlive()
            } catch (e: Exception) {
                Log.e(TAG, "Aggressive poll error: ${e.message}")
            }
        }, 300, 300, TimeUnit.MILLISECONDS)
    }

    private fun stopAggressivePolling() {
        aggressivePollFuture?.cancel(false)
        aggressivePollFuture = null
    }

    private fun forceRecreate() {
        val now = System.currentTimeMillis()
        val timeSinceLast = now - lastRecreateMs

        if (timeSinceLast < 100) {
            bgHandler.postDelayed({ forceRecreate() }, 100 - timeSinceLast)
            return
        }

        lastRecreateMs = now
        recreateCount++
        Log.d(TAG, "forceRecreate #${recreateCount}")

        destroyAll()
        createAll()
        applyGain()
    }

    private fun createAll() {
        try {
            loudnessEnhancer = LoudnessEnhancer(SESSION_ID).apply {
                enabled = true
                if (currentGain > 0) {
                    val targetGain = gainToMillibel(currentGain)
                    setTargetGain(targetGain)
                    Log.d(TAG, "LoudnessEnhancer: gain=$currentGain target=${targetGain}mB (${gainToDb(currentGain)}dB)")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "LoudnessEnhancer FAILED: ${e.message}")
        }

        try {
            equalizer = Equalizer(0, SESSION_ID).apply {
                enabled = true
                applyEqBands()
            }
            Log.d(TAG, "Equalizer: ${equalizer?.numberOfBands} bands")
        } catch (e: Exception) {
            Log.e(TAG, "Equalizer FAILED: ${e.message}")
        }
    }

    private fun applyEqBands() {
        val eq = equalizer ?: return
        if (currentGain <= 0) return
        try {
            val bands = eq.numberOfBands.toInt()
            for (i in 0 until bands) {
                val range = eq.getBandFreqRange(i.toShort())
                val upperMilliHz = range[1]
                val upperHz = upperMilliHz / 1000.0
                val gainDb = when {
                    upperHz <= 300.0 -> (currentGain * 0.8).toInt().coerceIn(-1200, 1200)
                    upperHz <= 1000.0 -> (currentGain * 0.3).toInt().coerceIn(-1200, 1200)
                    else -> (currentGain * -0.1).toInt().coerceIn(-1200, 1200)
                }
                eq.setBandLevel(i.toShort(), gainDb.toShort())
            }
        } catch (e: Exception) {
            Log.e(TAG, "applyEqBands failed: ${e.message}")
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
            Log.w(TAG, "Effects DEAD (le=$enhancerOk, eq=$eqOk) — re-creating")
            forceRecreate()
            return
        }

        // Check gain drift
        if (enhancerOk && currentGain > 0) {
            try {
                val current = loudnessEnhancer?.targetGain ?: 0
                val expected = gainToMillibel(currentGain)
                if (current != expected) {
                    Log.w(TAG, "Gain drifted: expected=${expected}mB actual=${current}mB — re-applying")
                    applyGain()
                }
            } catch (_: Exception) {}
        }
    }

    private fun registerCallbacks() {
        try {
            audioManager.registerAudioPlaybackCallback(playbackCallback, bgHandler)
            audioManager.registerAudioDeviceCallback(deviceCallback, bgHandler)
            Log.i(TAG, "Callbacks registered")
        } catch (e: Exception) {
            Log.e(TAG, "Callback register failed: ${e.message}")
        }
    }

    private fun registerScreenReceiver() {
        if (screenReceiverRegistered) return
        try {
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(Intent.ACTION_SCREEN_ON)
            }
            context.registerReceiver(screenReceiver, filter)
            screenReceiverRegistered = true
            Log.d(TAG, "Screen receiver registered")
        } catch (e: Exception) {
            Log.e(TAG, "Screen receiver register failed: ${e.message}")
        }
    }

    private fun unregisterScreenReceiver() {
        if (!screenReceiverRegistered) return
        try {
            context.unregisterReceiver(screenReceiver)
            screenReceiverRegistered = false
        } catch (_: Exception) {}
    }

    private fun unregisterCallbacks() {
        try {
            audioManager.unregisterAudioPlaybackCallback(playbackCallback)
            audioManager.unregisterAudioDeviceCallback(deviceCallback)
        } catch (_: Exception) {}
    }

    fun setGain(gainValue: Int) {
        val oldGain = currentGain
        currentGain = gainValue
        Log.i(TAG, "setGain: $oldGain -> $gainValue")
        applyGain()
    }

    private fun applyGain() {
        val boostedGain = gainToMillibel(currentGain)

        try {
            loudnessEnhancer?.setTargetGain(boostedGain)
        } catch (e: Exception) {
            Log.e(TAG, "setTargetGain failed: ${e.message}")
            forceRecreate()
            try { loudnessEnhancer?.setTargetGain(boostedGain) } catch (_: Exception) {}
        }

        try {
            applyEqBands()
        } catch (e: Exception) {
            Log.e(TAG, "applyEqBands failed: ${e.message}")
        }
    }

    fun stopSession() {
        isRunning = false
        stopAggressivePolling()
        bgHandler.removeCallbacksAndMessages(null)
        unregisterCallbacks()
        unregisterScreenReceiver()
        destroyAll()
        Log.i(TAG, "Session stopped (recreated $recreateCount times)")
    }

    fun isActive(): Boolean = isRunning
}
