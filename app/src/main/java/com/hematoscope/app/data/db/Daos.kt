package com.hematoscope.app.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CaseDao {
    @Query("SELECT * FROM cases ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<CaseEntity>>

    @Query("SELECT * FROM cases WHERE id = :id")
    fun observe(id: Long): Flow<CaseEntity?>

    @Query("SELECT * FROM cases WHERE id = :id")
    suspend fun get(id: Long): CaseEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: CaseEntity): Long

    @Update
    suspend fun update(entity: CaseEntity)

    @Delete
    suspend fun delete(entity: CaseEntity)
}

@Dao
interface CaptureDao {
    @Query("SELECT * FROM captures WHERE caseId = :caseId ORDER BY createdAt DESC")
    fun observeForCase(caseId: Long): Flow<List<CaptureEntity>>

    @Query("SELECT * FROM captures WHERE id = :id")
    suspend fun get(id: Long): CaptureEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: CaptureEntity): Long

    @Delete
    suspend fun delete(entity: CaptureEntity)
}

@Dao
interface DifferentialDao {
    @Query("SELECT * FROM differentials WHERE caseId = :caseId ORDER BY updatedAt DESC")
    fun observeForCase(caseId: Long): Flow<List<DifferentialEntity>>

    @Query("SELECT * FROM differentials WHERE id = :id")
    fun observe(id: Long): Flow<DifferentialEntity?>

    @Query("SELECT * FROM differentials WHERE id = :id")
    suspend fun get(id: Long): DifferentialEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: DifferentialEntity): Long

    @Update
    suspend fun update(entity: DifferentialEntity)

    @Delete
    suspend fun delete(entity: DifferentialEntity)
}

@Dao
interface ObservationDao {
    @Query("SELECT * FROM observations WHERE caseId = :caseId ORDER BY createdAt DESC")
    fun observeForCase(caseId: Long): Flow<List<ObservationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ObservationEntity): Long

    @Update
    suspend fun update(entity: ObservationEntity)

    @Delete
    suspend fun delete(entity: ObservationEntity)

    @Query("DELETE FROM observations WHERE caseId = :caseId AND descriptorId = :descriptorId")
    suspend fun deleteByDescriptor(caseId: Long, descriptorId: String)
}

@Dao
interface CalibrationDao {
    @Query("SELECT * FROM calibrations ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<CalibrationEntity>>

    @Query("SELECT * FROM calibrations WHERE objectiveLabel = :objective ORDER BY createdAt DESC LIMIT 1")
    suspend fun latestFor(objective: String): CalibrationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: CalibrationEntity): Long

    @Delete
    suspend fun delete(entity: CalibrationEntity)
}

@Dao
interface MeasurementDao {
    @Query("SELECT * FROM measurements WHERE captureId = :captureId ORDER BY createdAt DESC")
    fun observeForCapture(captureId: Long): Flow<List<MeasurementEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: MeasurementEntity): Long

    @Delete
    suspend fun delete(entity: MeasurementEntity)
}
