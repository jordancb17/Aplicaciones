package com.hematoscope.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        CaseEntity::class,
        CaptureEntity::class,
        DifferentialEntity::class,
        ObservationEntity::class,
        CalibrationEntity::class,
        MeasurementEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class HematoDatabase : RoomDatabase() {
    abstract fun caseDao(): CaseDao
    abstract fun captureDao(): CaptureDao
    abstract fun differentialDao(): DifferentialDao
    abstract fun observationDao(): ObservationDao
    abstract fun calibrationDao(): CalibrationDao
    abstract fun measurementDao(): MeasurementDao

    companion object {
        @Volatile private var instance: HematoDatabase? = null

        fun get(context: Context): HematoDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    HematoDatabase::class.java,
                    "hematoscope.db"
                ).fallbackToDestructiveMigration().build().also { instance = it }
            }
    }
}
