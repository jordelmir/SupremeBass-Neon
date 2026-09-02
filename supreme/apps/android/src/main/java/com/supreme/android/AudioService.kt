package com.supreme.android

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.PowerManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.util.Log

class AudioService : Service() {
    private companion object {
        const val TAG = "SupremeBass_Service"
        const val NOTIFICATION_ID = 1
        const val CHANNEL_ID = "SupremeBassChannel"
    }

    private var audioEngine: LegacyEffectsEngine? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var currentGain = 0
    private var sessionStartTime = 0L

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "onCreate")
        acquireWakeLock()
        startForegroundNotification()
        audioEngine = LegacyEffectsEngine(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val isRestart = intent == null

        val gain: Int
        if (isRestart) {
            gain = AudioStatePersistence.gainValue(this).toInt()
            val wasEnabled = AudioStatePersistence.isEnabled(this)
            Log.i(TAG, "System restart: enabled=$wasEnabled, gain=$gain")

            if (!wasEnabled || gain <= 0) {
                Log.w(TAG, "Was disabled or gain=0, stopping self")
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
        } else {
            gain = intent?.getIntExtra("GAIN", 0) ?: 0
            Log.i(TAG, "onStartCommand: gain=$gain")
        }

        if (audioEngine == null) {
            Log.w(TAG, "Engine was null — recreating")
            acquireWakeLock()
            startForegroundNotification()
            audioEngine = LegacyEffectsEngine(this)
        }

        audioEngine?.stopSession()
        audioEngine?.startSession()
        audioEngine?.setGain(gain)
        currentGain = gain
        sessionStartTime = System.currentTimeMillis()

        updateNotification(gain)

        Log.i(TAG, "Engine active: gain=$gain")

        return START_STICKY
    }

    override fun onDestroy() {
        Log.i(TAG, "onDestroy — releasing effects")
        audioEngine?.stopSession()
        audioEngine = null
        releaseWakeLock()
        super.onDestroy()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        // DO NOT stop service here — keep boost alive in background
        // The foreground service keeps running even when app is swiped
        Log.i(TAG, "onTaskRemoved — keeping service alive for background boost")
        super.onTaskRemoved(rootIntent)
    }

    override fun onLowMemory() {
        Log.w(TAG, "onLowMemory — effects may be killed, will auto-recover via polling")
        super.onLowMemory()
    }

    override fun onTrimMemory(level: Int) {
        Log.w(TAG, "onTrimMemory level=$level — effects may be killed, will auto-recover")
        super.onTrimMemory(level)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun acquireWakeLock() {
        try {
            val pm = getSystemService(POWER_SERVICE) as PowerManager
            wakeLock = pm.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "SupremeAcoustics::AudioService"
            ).apply {
                acquire(24 * 60 * 60 * 1000L)
            }
            Log.d(TAG, "WakeLock acquired")
        } catch (e: Exception) {
            Log.e(TAG, "WakeLock acquire failed: ${e.message}")
        }
    }

    private fun releaseWakeLock() {
        try {
            wakeLock?.let { if (it.isHeld) it.release() }
            wakeLock = null
        } catch (_: Exception) {}
    }

    private fun startForegroundNotification() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "SupremeBass Active",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Audio boost is running in background"
            setShowBadge(false)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)

        val notification = buildNotification(0)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        Log.d(TAG, "Foreground notification started")
    }

    fun updateNotification(gain: Int) {
        try {
            val notification = buildNotification(gain)
            getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            Log.e(TAG, "Notification update failed: ${e.message}")
        }
    }

    private fun buildNotification(gain: Int): Notification {
        val duration = if (sessionStartTime > 0) {
            val elapsed = (System.currentTimeMillis() - sessionStartTime) / 1000
            val min = elapsed / 60
            val sec = elapsed % 60
            " | ${min}m ${sec}s"
        } else ""

        val title = if (gain > 0) {
            "${100 + gain}% boost active$duration"
        } else {
            "SupremeBass Active"
        }

        val openIntent = packageManager.getLaunchIntentForPackage(packageName)
        val pendingIntent = android.app.PendingIntent.getActivity(
            this, 0, openIntent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )

        // Stop action
        val stopIntent = Intent(this, AudioService::class.java).apply {
            action = "STOP"
        }
        val stopPendingIntent = android.app.PendingIntent.getService(
            this, 1, stopIntent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )

        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText("Boosting all audio — tap to open")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_media_pause, "Stop", stopPendingIntent)
            .setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }
}
