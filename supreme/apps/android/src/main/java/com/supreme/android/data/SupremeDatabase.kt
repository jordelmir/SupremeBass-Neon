package com.supreme.android.data

import android.content.Context
import androidx.room.*
import com.supreme.core.*
import java.time.Instant

/**
 * Supreme Database — Room persistence layer.
 */

@Entity(tableName = "assets")
data class AssetEntity(
    @PrimaryKey val id: String,
    val name: String,
    val category: String,
    val subcategory: String? = null,
    val brand: String? = null,
    val model: String? = null,
    val serialNumber: String? = null,
    val purchaseDate: Long? = null,
    val purchasePrice: Double? = null,
    val currency: String = "CRC",
    val warrantyExpiry: Long? = null,
    val condition: String = "GOOD",
    val location: String? = null,
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "devices")
data class DeviceEntity(
    @PrimaryKey val id: String,
    val name: String,
    val type: String,
    val protocol: String,
    val assetId: String? = null,
    val connected: Boolean = false,
    val batteryLevel: Double? = null,
    val lastSeen: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "observations")
data class ObservationEntity(
    @PrimaryKey val id: String,
    val deviceId: String? = null,
    val assetId: String? = null,
    val sensorType: String,
    val timestamp: Long,
    val readings: String, // JSON
    val confidence: Double = 0.9,
    val source: String = "DEVICE"
)

@Entity(tableName = "maintenance_tasks")
data class MaintenanceTaskEntity(
    @PrimaryKey val id: String,
    val assetId: String,
    val title: String,
    val description: String,
    val type: String,
    val priority: String,
    val scheduledDate: Long,
    val dueDate: Long,
    val completedDate: Long? = null,
    val intervalDays: Int? = null,
    val estimatedCost: Double? = null,
    val actualCost: Double? = null,
    val currency: String = "CRC",
    val recurring: Boolean = false,
    val notes: String? = null
)

@Entity(tableName = "warranties")
data class WarrantyEntity(
    @PrimaryKey val assetId: String,
    val provider: String,
    val purchaseDate: Long,
    val warrantyStart: Long,
    val warrantyEnd: Long,
    val warrantyMonths: Int,
    val purchasePrice: Double,
    val currency: String = "CRC",
    val serialNumber: String? = null,
    val isActive: Boolean = true,
    val notes: String? = null
)

@Entity(tableName = "meter_readings")
data class MeterReadingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val meterId: String,
    val meterType: String,
    val value: Double,
    val timestamp: Long,
    val source: String = "MANUAL"
)

@Entity(tableName = "inventory_items")
data class InventoryItemEntity(
    @PrimaryKey val id: String,
    val name: String,
    val category: String,
    val subcategory: String? = null,
    val brand: String? = null,
    val model: String? = null,
    val serialNumber: String? = null,
    val purchaseDate: Long? = null,
    val purchasePrice: Double? = null,
    val currency: String = "CRC",
    val warrantyExpiry: Long? = null,
    val condition: String = "GOOD",
    val location: String? = null,
    val barcode: String? = null,
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "vehicles")
data class VehicleEntity(
    @PrimaryKey val id: String,
    val name: String,
    val make: String? = null,
    val model: String? = null,
    val year: Int? = null,
    val licensePlate: String? = null,
    val vin: String? = null,
    val odometerKm: Double? = null,
    val fuelType: String = "GASOLINE",
    val connected: Boolean = false
)

@Entity(tableName = "leak_readings")
data class LeakReadingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sensorId: String,
    val timestamp: Long,
    val waterDetected: Boolean,
    val flowRateLpm: Double? = null
)

@Dao
interface AssetDao {
    @Query("SELECT * FROM assets")
    suspend fun getAll(): List<AssetEntity>

    @Query("SELECT * FROM assets WHERE id = :id")
    suspend fun getById(id: String): AssetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(asset: AssetEntity)

    @Update
    suspend fun update(asset: AssetEntity)

    @Delete
    suspend fun delete(asset: AssetEntity)

    @Query("SELECT * FROM assets WHERE category = :category")
    suspend fun getByCategory(category: String): List<AssetEntity>
}

@Dao
interface MaintenanceDao {
    @Query("SELECT * FROM maintenance_tasks WHERE assetId = :assetId")
    suspend fun getByAsset(assetId: String): List<MaintenanceTaskEntity>

    @Query("SELECT * FROM maintenance_tasks WHERE completedDate IS NULL AND dueDate < :now")
    suspend fun getOverdue(now: Long): List<MaintenanceTaskEntity>

    @Query("SELECT * FROM maintenance_tasks WHERE completedDate IS NULL AND dueDate < :end")
    suspend fun getDueSoon(end: Long): List<MaintenanceTaskEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: MaintenanceTaskEntity)

    @Update
    suspend fun update(task: MaintenanceTaskEntity)
}

@Dao
interface WarrantyDao {
    @Query("SELECT * FROM warranties")
    suspend fun getAll(): List<WarrantyEntity>

    @Query("SELECT * FROM warranties WHERE assetId = :assetId")
    suspend fun getByAsset(assetId: String): WarrantyEntity?

    @Query("SELECT * FROM warranties WHERE isActive = 1 AND warrantyEnd > :now")
    suspend fun getActive(now: Long): List<WarrantyEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(warranty: WarrantyEntity)
}

@Dao
interface InventoryDao {
    @Query("SELECT * FROM inventory_items")
    suspend fun getAll(): List<InventoryItemEntity>

    @Query("SELECT * FROM inventory_items WHERE id = :id")
    suspend fun getById(id: String): InventoryItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: InventoryItemEntity)

    @Delete
    suspend fun delete(item: InventoryItemEntity)

    @Query("SELECT * FROM inventory_items WHERE name LIKE '%' || :query || '%' OR brand LIKE '%' || :query || '%'")
    suspend fun search(query: String): List<InventoryItemEntity>
}

@Dao
interface MeterDao {
    @Query("SELECT * FROM meter_readings WHERE meterId = :meterId ORDER BY timestamp DESC")
    suspend fun getByMeter(meterId: String): List<MeterReadingEntity>

    @Insert
    suspend fun insert(reading: MeterReadingEntity)
}

@Dao
interface VehicleDao {
    @Query("SELECT * FROM vehicles")
    suspend fun getAll(): List<VehicleEntity>

    @Query("SELECT * FROM vehicles WHERE id = :id")
    suspend fun getById(id: String): VehicleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(vehicle: VehicleEntity)
}

@Database(
    entities = [
        AssetEntity::class,
        DeviceEntity::class,
        ObservationEntity::class,
        MaintenanceTaskEntity::class,
        WarrantyEntity::class,
        MeterReadingEntity::class,
        InventoryItemEntity::class,
        VehicleEntity::class,
        LeakReadingEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class SupremeDatabase : RoomDatabase() {
    abstract fun assetDao(): AssetDao
    abstract fun maintenanceDao(): MaintenanceDao
    abstract fun warrantyDao(): WarrantyDao
    abstract fun inventoryDao(): InventoryDao
    abstract fun meterDao(): MeterDao
    abstract fun vehicleDao(): VehicleDao

    companion object {
        @Volatile
        private var INSTANCE: SupremeDatabase? = null

        fun getDatabase(context: Context): SupremeDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SupremeDatabase::class.java,
                    "supreme_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
