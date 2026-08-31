package com.supreme.android.permissions

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

object PermissionHelper {

    val CAMERA_PERMISSIONS = arrayOf(
        Manifest.permission.CAMERA
    )

    val MICROPHONE_PERMISSIONS = arrayOf(
        Manifest.permission.RECORD_AUDIO
    )

    val LOCATION_PERMISSIONS = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    val BLUETOOTH_PERMISSIONS = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_ADVERTISE
        )
    } else {
        arrayOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }

    val SENSOR_PERMISSIONS = arrayOf(
        Manifest.permission.HIGH_SAMPLING_RATE_SENSORS
    )

    fun hasPermission(context: Context, permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    fun hasPermissions(context: Context, permissions: Array<String>): Boolean {
        return permissions.all { hasPermission(context, it) }
    }

    fun getMissingPermissions(context: Context, permissions: Array<String>): Array<String> {
        return permissions.filter { !hasPermission(context, it) }.toTypedArray()
    }
}

@Composable
fun RequestPermissionEffect(
    permission: String,
    onGranted: () -> Unit,
    onDenied: () -> Unit = {}
) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) onGranted() else onDenied()
    }

    LaunchedEffect(permission) {
        if (PermissionHelper.hasPermission(context, permission)) {
            onGranted()
        } else {
            launcher.launch(permission)
        }
    }
}

@Composable
fun RequestMultiplePermissionsEffect(
    permissions: Array<String>,
    onAllGranted: () -> Unit,
    onDenied: (List<String>) -> Unit = {}
) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val denied = results.filter { !it.value }.keys.toList()
        if (denied.isEmpty()) onAllGranted() else onDenied(denied)
    }

    LaunchedEffect(permissions) {
        if (PermissionHelper.hasPermissions(context, permissions)) {
            onAllGranted()
        } else {
            launcher.launch(permissions)
        }
    }
}
