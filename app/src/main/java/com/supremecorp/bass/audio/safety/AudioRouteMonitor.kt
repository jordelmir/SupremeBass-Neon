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

    // Optional supplier for the actual routed device from AudioTrack
    // This provides the ground truth of what device audio is actually going to
    private var routedDeviceSupplier: (() -> AudioDeviceInfo?)? = null

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
        routedDeviceSupplier = null
    }

    fun setListener(listener: RouteChangeListener?) {
        this.listener = listener
    }

    /**
     * Set a supplier that returns the actual routed device from AudioTrack.
     * This is the ground truth — AudioTrack.getRoutedDevice() tells us where
     * audio is *actually* going, not just what devices are *available*.
     */
    fun setRoutedDeviceSupplier(supplier: (() -> AudioDeviceInfo?)?) {
        this.routedDeviceSupplier = supplier
    }

    fun getCurrentRoute(): OutputRoute = currentRoute

    private fun updateRoute() {
        val newRoute = detectRoute()

        if (newRoute != currentRoute) {
            val oldRoute = currentRoute
            currentRoute = newRoute
            AppLogger.i("RouteMonitor", "Route changed: $oldRoute -> $newRoute")
            listener?.onRouteChanged(oldRoute, newRoute)
        }
    }

    /**
     * Detect the active output route.
     * Priority: AudioTrack.getRoutedDevice() > AudioManager.getDevices()
     */
    private fun detectRoute(): OutputRoute {
        // First try: use the actual routed device from AudioTrack (ground truth)
        routedDeviceSupplier?.let { supplier ->
            try {
                val routedDevice = supplier()
                if (routedDevice != null) {
                    return mapDeviceTypeToRoute(routedDevice.type)
                }
            } catch (e: Exception) {
                AppLogger.w("RouteMonitor", "Routed device query failed: ${e.message}")
            }
        }

        // Fallback: use AudioManager.getDevices() (available, not necessarily active)
        val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        return detectRouteFromDevices(devices)
    }

    private fun mapDeviceTypeToRoute(deviceType: Int): OutputRoute {
        return when (deviceType) {
            AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> OutputRoute.BUILT_IN_SPEAKER
            AudioDeviceInfo.TYPE_WIRED_HEADSET,
            AudioDeviceInfo.TYPE_WIRED_HEADPHONES -> OutputRoute.WIRED_HEADPHONES
            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
            AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
            AudioDeviceInfo.TYPE_BLE_HEADSET,
            AudioDeviceInfo.TYPE_BLE_SPEAKER -> OutputRoute.BLUETOOTH
            AudioDeviceInfo.TYPE_USB_ACCESSORY,
            AudioDeviceInfo.TYPE_USB_DEVICE,
            AudioDeviceInfo.TYPE_USB_HEADSET -> OutputRoute.USB_AUDIO
            AudioDeviceInfo.TYPE_HDMI,
            AudioDeviceInfo.TYPE_HDMI_ARC -> OutputRoute.HDMI
            else -> OutputRoute.UNKNOWN
        }
    }

    private fun detectRouteFromDevices(devices: Array<out AudioDeviceInfo>): OutputRoute {
        for (device in devices) {
            val route = mapDeviceTypeToRoute(device.type)
            if (route != OutputRoute.UNKNOWN) return route
        }
        return OutputRoute.UNKNOWN
    }
}

fun interface RouteChangeListener {
    fun onRouteChanged(oldRoute: OutputRoute, newRoute: OutputRoute)
}
