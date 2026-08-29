package com.supremecorp.bass.data.device

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        DeviceAcousticProfileEntity::class,
        FrequencyResponsePointEntity::class,
        DeviceInfoEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class DeviceDatabase : RoomDatabase() {
    abstract fun deviceAcousticProfileDao(): DeviceAcousticProfileDao
    abstract fun frequencyResponsePointDao(): FrequencyResponsePointDao
    abstract fun deviceInfoDao(): DeviceInfoDao

    companion object {
        @Volatile private var INSTANCE: DeviceDatabase? = null

        fun getInstance(context: Context): DeviceDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    DeviceDatabase::class.java,
                    "device_database"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}