package inu.appcenter.walkman.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import inu.appcenter.walkman.local.dao.GaitAnalysisDao
import inu.appcenter.walkman.local.dao.RecordingSessionDao
import inu.appcenter.walkman.local.dao.SensorReadingDao
import inu.appcenter.walkman.local.entity.GaitAnalysisEntity
import inu.appcenter.walkman.local.entity.RecordingSessionEntity
import inu.appcenter.walkman.local.entity.SensorReadingEntity

@Database(
    entities = [
        RecordingSessionEntity::class,
        SensorReadingEntity::class,
        GaitAnalysisEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class WalkManDatabase : RoomDatabase() {

    abstract fun recordingSessionDao(): RecordingSessionDao
    abstract fun sensorReadingDao(): SensorReadingDao
    abstract fun gaitAnalysisDao(): GaitAnalysisDao

    companion object {
        @Volatile
        private var INSTANCE: WalkManDatabase? = null

        fun getDatabase(context: Context): WalkManDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    WalkManDatabase::class.java,
                    "walkman_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}