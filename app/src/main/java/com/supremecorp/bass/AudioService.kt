package com.supremecorp.bass

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.PowerManager
import android.content.pm.ServiceInfo
import android.os.Build
import com.supremecorp.bass.core.logging.AppLogger

class AudioService : Service() {
    private var audioEngine: AudioEngine? = null
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate() {
        super.onCreate()
        AppLogger.i("Service", "onCreate")
        acquireWakeLock()
        startForegroundNotification()
        audioEngine = AudioEngine(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val isRestart = intent == null

        val gain: Int
        if (isRestart) {
            gain = AudioStatePersistence.gainValue(this).toInt()
            val wasEnabled = AudioStatePersistence.isEnabled(this)
            AppLogger.w("Service", "Restart: enabled=$wasEnabled, gain=$gain")
            if (!wasEnabled || gain <= 0) {
                AppLogger.i("Service", "Was off, self-stopping")
                stopSelf()
                return START_NOT_STICKY
            }
        } else {
            gain = intent?.getIntExtra("GAIN", 0) ?: 0
        }

        // Always ensure engine exists
        if (audioEngine == null) {
            audioEngine = AudioEngine(this)
        }

        audioEngine?.startSession()
        audioEngine?.setGain(gain)
        AppLogger.i("Service", "gain=$gain")
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        AppLogger.i("Service", "onDestroy")
        audioEngine?.stopSession()
        audioEngine = null
        releaseWakeLock()
        super.onDestroy()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        AppLogger.i("Service", "onTaskRemoved")
        audioEngine?.stopSession()
        audioEngine = null
        releaseWakeLock()
        AudioStatePersistence.saveEnabled(this, false)
        super.onTaskRemoved(rootIntent)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun acquireWakeLock() {
        try {
            val pm = getSystemService(POWER_SERVICE) as PowerManager
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "SupremeBass::WakeLock").apply {
                acquire(60 * 60 * 1000L)
            }
        } catch (e: Exception) {
            AppLogger.e("Service", "WakeLock: ${e.message}")
        }
    }

    private fun releaseWakeLock() {
        try {
            wakeLock?.let { if (it.isHeld) it.release() }
            wakeLock = null
        } catch (_: Exception) {}
    }

    private fun startForegroundNotification() {
        val channelId = "SupremeBassChannel"
        val channel = NotificationChannel(channelId, "Supreme Bass Active", NotificationManager.IMPORTANCE_LOW)
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)

        val notification = Notification.Builder(this, channelId)
            .setContentTitle("Supreme Bass Active")
            .setContentText("Audio boost running.")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        } else {
            startForeground(1, notification)
        }
    }
}
