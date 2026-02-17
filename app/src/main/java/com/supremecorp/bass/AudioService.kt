package com.supremecorp.bass

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder

import android.content.pm.ServiceInfo
import android.os.Build

class AudioService : Service() {
    private lateinit var audioEngine: AudioEngine

    override fun onCreate() {
        super.onCreate()
        audioEngine = AudioEngine(this)
        
        val notification = createNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        } else {
            startForeground(1, notification)
        }
        
        audioEngine.startSession()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val gain = intent?.getIntExtra("GAIN", 0) ?: 0
        audioEngine.setGain(gain)
        return START_STICKY
    }

    override fun onDestroy() {
        audioEngine.stopSession()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun createNotification(): Notification {
        val channelId = "SupremeBassChannel"
        val channel = NotificationChannel(channelId, "Supreme Bass Active", NotificationManager.IMPORTANCE_LOW)
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)

        return Notification.Builder(this, channelId)
            .setContentTitle("Supreme Bass Active")
            .setContentText("Hyper-Drive Audio Boost is Engine On.")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .build()
    }
}
