package com.supremecorp.bass.audio.safety

import android.content.Context
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import com.supremecorp.bass.core.logging.AppLogger
import com.supremecorp.bass.domain.model.OutputRoute

class AudioRouteMonitor(context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var currentRoute: OutputRoute = OutputRoute.UNKNOWN
    private var listener: RouteChangeListener? = null

    private val deviceCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>) {
            updateRoute()
        }

        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>) {
            updateRoute()
        }
    }

    fun start() {
        audioManager.registerAudioDeviceCallback(deviceCallback, null)
        updateRoute()
        AppLogger.i("RouteMonitor", "Started, initial route=$currentRoute")
    }

    fun stop() {
        try {
            audioManager.unregisterAudioDeviceCallback(deviceCallback)
        } catch (e: Exception) {
            AppLogger.w("RouteMonitor", "Unregister failed: ${e.message}")
        }
        listener = null
    }

    fun setListener(listener: RouteChangeListener?) {
        this.listener = listener
    }

    fun getCurrentRoute(): OutputRoute = currentRoute

    private fun updateRoute() {
        val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        val newRoute = detectRoute(devices)

        if (newRoute != currentRoute) {
            val oldRoute = currentRoute
            currentRoute = newRoute
            AppLogger.i("RouteMonitor", "Route changed: $oldRoute -> $newRoute")
            listener?.onRouteChanged(oldRoute, newRoute)
        }
    }

    private fun detectRoute(devices: Array<out AudioDeviceInfo>): OutputRoute {
        for (device in devices) {
            when (device.type) {
                AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> return OutputRoute.BUILT_IN_SPEAKER
                AudioDeviceInfo.TYPE_WIRED_HEADSET,
                AudioDeviceInfo.TYPE_WIRED_HEADPHONES -> return OutputRoute.WIRED_HEADPHONES
                AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
                AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
                AudioDeviceInfo.TYPE_BLE_HEADSET,
                AudioDeviceInfo.TYPE_BLE_SPEAKER -> return OutputRoute.BLUETOOTH
                AudioDeviceInfo.TYPE_USB_ACCESSORY,
                AudioDeviceInfo.TYPE_USB_DEVICE,
                AudioDeviceInfo.TYPE_USB_HEADSET -> return OutputRoute.USB_AUDIO
                AudioDeviceInfo.TYPE_HDMI,
                AudioDeviceInfo.TYPE_HDMI_ARC -> return OutputRoute.HDMI
            }
        }
        return OutputRoute.UNKNOWN
    }
}

fun interface RouteChangeListener {
    fun onRouteChanged(oldRoute: OutputRoute, newRoute: OutputRoute)
}
