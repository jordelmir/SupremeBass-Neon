package com.supreme.android.data.experiment

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        ExperimentEntity::class,
        ObservationEntity::class,
        ExperimentResultEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class ExperimentDatabase : RoomDatabase() {
    abstract fun experimentDao(): ExperimentDao

    companion object {
        @Volatile
        private var INSTANCE: ExperimentDatabase? = null

        fun getInstance(context: Context): ExperimentDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ExperimentDatabase::class.java,
                    "experiment_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
