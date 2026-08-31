package com.supreme.android.di

import android.content.Context
import com.supreme.android.data.SupremeDatabase
import com.supreme.fix.FixAIEngine
import com.supreme.maintenance.MaintenanceOSEngine
import com.supreme.warranty.WarrantyVaultEngine
import com.supreme.network.NetworkDoctorEngine
import com.supreme.noise.NoiseDoctorEngine
import com.supreme.vibration.VibrationDoctorEngine
import com.supreme.inventory.InventoryEngine
import com.supreme.home.HomeHubEngine
import com.supreme.find.FindEngine
import com.supreme.camera.CameraHubEngine
import com.supreme.leak.LeakWatchEngine
import com.supreme.emergency.EmergencyEngine
import com.supreme.vehicle.VehicleHubEngine
import com.supreme.utilities.UtilitiesEngine

class AppContainer(context: Context) {
    private val database = SupremeDatabase.getDatabase(context)

    val fixAIEngine = FixAIEngine()
    val maintenanceOSEngine = MaintenanceOSEngine()
    val warrantyVaultEngine = WarrantyVaultEngine()
    val networkDoctorEngine = NetworkDoctorEngine()
    val noiseDoctorEngine = NoiseDoctorEngine()
    val vibrationDoctorEngine = VibrationDoctorEngine()
    val inventoryEngine = InventoryEngine()
    val homeHubEngine = HomeHubEngine()
    val findEngine = FindEngine()
    val cameraHubEngine = CameraHubEngine()
    val leakWatchEngine = LeakWatchEngine()
    val emergencyEngine = EmergencyEngine()
    val vehicleHubEngine = VehicleHubEngine()
    val utilitiesEngine = UtilitiesEngine()

    val assetDao = database.assetDao()
    val maintenanceDao = database.maintenanceDao()
    val warrantyDao = database.warrantyDao()
    val inventoryDao = database.inventoryDao()
    val meterDao = database.meterDao()
    val vehicleDao = database.vehicleDao()
}
