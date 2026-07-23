package com.hematoscope.app.data.repository

import android.content.Context
import android.graphics.Bitmap
import com.hematoscope.app.data.db.CalibrationEntity
import com.hematoscope.app.data.db.CaptureEntity
import com.hematoscope.app.data.db.CaseEntity
import com.hematoscope.app.data.db.DifferentialEntity
import com.hematoscope.app.data.db.HematoDatabase
import com.hematoscope.app.data.db.MeasurementEntity
import com.hematoscope.app.data.db.ObservationEntity
import com.hematoscope.app.data.db.Serialization
import com.hematoscope.app.data.model.MeasurementAnnotation
import com.hematoscope.app.data.model.MeasurementTool
import com.hematoscope.app.data.model.MorphologyGrade
import com.hematoscope.app.domain.catalog.CellCatalog
import com.hematoscope.app.domain.measurement.Calibration
import com.hematoscope.app.domain.report.CaseReportData
import com.hematoscope.app.domain.report.DiffRow
import com.hematoscope.app.domain.report.GradedFinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * Single access point for persistence. Wraps Room DAOs and manages the on-disk
 * capture files (kept in app-private storage under `captures/`).
 */
class HematoRepository(context: Context) {

    private val db = HematoDatabase.get(context)
    private val appContext = context.applicationContext
    private val capturesDir: File =
        File(appContext.filesDir, "captures").apply { if (!exists()) mkdirs() }

    // ---------------------------------------------------------------- Cases
    fun observeCases(): Flow<List<CaseEntity>> = db.caseDao().observeAll()
    fun observeCase(id: Long): Flow<CaseEntity?> = db.caseDao().observe(id)
    suspend fun getCase(id: Long) = db.caseDao().get(id)

    suspend fun createCase(patientCode: String, description: String): Long =
        db.caseDao().insert(CaseEntity(patientCode = patientCode, description = description))

    suspend fun updateCase(entity: CaseEntity) =
        db.caseDao().update(entity.copy(updatedAt = System.currentTimeMillis()))

    suspend fun deleteCase(entity: CaseEntity) = db.caseDao().delete(entity)

    // ------------------------------------------------------------- Captures
    fun observeCaptures(caseId: Long): Flow<List<CaptureEntity>> =
        db.captureDao().observeForCase(caseId)

    suspend fun getCapture(id: Long) = db.captureDao().get(id)

    /** Persist a bitmap to disk and record it against a case. Returns the row id. */
    suspend fun saveCapture(
        caseId: Long,
        bitmap: Bitmap,
        sourceType: String,
        objectiveLabel: String,
        note: String = ""
    ): Long = withContext(Dispatchers.IO) {
        val file = File(capturesDir, "cap_${System.currentTimeMillis()}.jpg")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
        }
        db.captureDao().insert(
            CaptureEntity(
                caseId = caseId,
                filePath = file.absolutePath,
                sourceType = sourceType,
                objectiveLabel = objectiveLabel,
                note = note
            )
        )
    }

    suspend fun deleteCapture(entity: CaptureEntity) = withContext(Dispatchers.IO) {
        runCatching { File(entity.filePath).delete() }
        db.captureDao().delete(entity)
    }

    // --------------------------------------------------------- Differentials
    fun observeDifferentials(caseId: Long): Flow<List<DifferentialEntity>> =
        db.differentialDao().observeForCase(caseId)

    fun observeDifferential(id: Long): Flow<DifferentialEntity?> =
        db.differentialDao().observe(id)

    suspend fun createDifferential(caseId: Long, name: String, targetTotal: Int): Long =
        db.differentialDao().insert(
            DifferentialEntity(caseId = caseId, name = name, targetTotal = targetTotal)
        )

    suspend fun saveDifferentialTallies(
        id: Long,
        tallies: Map<String, Int>,
        nrbcCount: Int
    ) {
        val current = db.differentialDao().get(id) ?: return
        db.differentialDao().update(
            current.copy(
                talliesJson = Serialization.encodeTallies(tallies),
                nrbcCount = nrbcCount,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun deleteDifferential(entity: DifferentialEntity) =
        db.differentialDao().delete(entity)

    fun decodeTallies(entity: DifferentialEntity): Map<String, Int> =
        Serialization.decodeTallies(entity.talliesJson)

    // -------------------------------------------------------- Observations
    fun observeObservations(caseId: Long): Flow<List<ObservationEntity>> =
        db.observationDao().observeForCase(caseId)

    suspend fun setObservation(
        caseId: Long,
        descriptorId: String,
        gradePlus: Int,
        present: Boolean,
        note: String
    ) {
        // Replace any prior grade for this descriptor within the case.
        db.observationDao().deleteByDescriptor(caseId, descriptorId)
        if (present || gradePlus > 0) {
            db.observationDao().insert(
                ObservationEntity(
                    caseId = caseId,
                    descriptorId = descriptorId,
                    gradePlus = gradePlus,
                    present = present,
                    note = note
                )
            )
        }
    }

    // --------------------------------------------------------- Calibrations
    fun observeCalibrations(): Flow<List<Calibration>> =
        db.calibrationDao().observeAll().map { list -> list.map { it.toDomain() } }

    suspend fun latestCalibration(objectiveLabel: String): Calibration? =
        db.calibrationDao().latestFor(objectiveLabel)?.toDomain()

    suspend fun saveCalibration(cal: Calibration): Long =
        db.calibrationDao().insert(
            CalibrationEntity(
                objectiveLabel = cal.objectiveLabel,
                magnification = cal.magnification,
                micronsPerPixel = cal.micronsPerPixel,
                referencePixels = cal.referencePixels,
                referenceMicrons = cal.referenceMicrons,
                imageWidthPx = cal.imageWidthPx,
                imageHeightPx = cal.imageHeightPx
            )
        )

    // ---------------------------------------------------------- Measurements
    fun observeMeasurements(captureId: Long): Flow<List<MeasurementAnnotation>> =
        db.measurementDao().observeForCapture(captureId).map { list ->
            list.map { it.toDomain() }
        }

    suspend fun saveMeasurement(captureId: Long, m: MeasurementAnnotation): Long =
        db.measurementDao().insert(
            MeasurementEntity(
                captureId = captureId,
                tool = m.tool.name,
                pointsCsv = Serialization.encodePoints(m.points),
                nucleusPointCount = m.nucleusPointCount,
                label = m.label,
                primaryValue = m.primaryValue,
                secondaryValue = m.secondaryValue,
                unit = m.unit,
                calibrated = m.calibrated
            )
        )

    // ------------------------------------------------------------- Reporting
    /**
     * Gather everything needed for a case report, resolving catalog names.
     * Returns null if the case no longer exists.
     */
    suspend fun buildCaseReport(caseId: Long): CaseReportData? = withContext(Dispatchers.IO) {
        val case = db.caseDao().get(caseId) ?: return@withContext null
        val captures = db.captureDao().observeForCase(caseId).first()
        val observations = db.observationDao().observeForCase(caseId).first()
        val latestDiff = db.differentialDao().observeForCase(caseId).first().firstOrNull()

        val diffRows: List<DiffRow>
        val wbcTotal: Int
        if (latestDiff != null) {
            val tallies = Serialization.decodeTallies(latestDiff.talliesJson)
            wbcTotal = tallies.values.sum()
            diffRows = tallies
                .filter { it.value > 0 }
                .map { (id, count) ->
                    val name = CellCatalog.cellById(id)?.name ?: id
                    val pct = if (wbcTotal > 0) count * 100f / wbcTotal else 0f
                    DiffRow(name, count, pct)
                }
                .sortedByDescending { it.count }
        } else {
            diffRows = emptyList()
            wbcTotal = 0
        }
        val nrbc = latestDiff?.nrbcCount ?: 0
        val nrbcPer100 = if (wbcTotal > 0) nrbc * 100f / wbcTotal else 0f

        val graded = observations
            .filter { it.gradePlus > 0 }
            .mapNotNull { obs ->
                val d = CellCatalog.descriptorById(obs.descriptorId) ?: return@mapNotNull null
                if (!d.gradable) return@mapNotNull null
                GradedFinding(d.name, MorphologyGrade.fromPlus(obs.gradePlus).label)
            }
        val qualitative = observations
            .mapNotNull { obs ->
                val d = CellCatalog.descriptorById(obs.descriptorId) ?: return@mapNotNull null
                if (d.gradable) null else d.name
            }

        CaseReportData(
            patientCode = case.patientCode,
            description = case.description,
            createdAtEpochMs = case.createdAt,
            hasDifferential = latestDiff != null,
            differentialName = latestDiff?.name ?: "",
            differentialTarget = latestDiff?.targetTotal ?: 0,
            wbcTotal = wbcTotal,
            differentialRows = diffRows,
            nrbcCount = nrbc,
            nrbcPer100Wbc = nrbcPer100,
            gradedFindings = graded,
            qualitativeFindings = qualitative,
            capturePaths = captures.map { it.filePath }
        )
    }

    // ------------------------------------------------------------- Mappers
    private fun CalibrationEntity.toDomain() = Calibration(
        objectiveLabel = objectiveLabel,
        magnification = magnification,
        micronsPerPixel = micronsPerPixel,
        referencePixels = referencePixels,
        referenceMicrons = referenceMicrons,
        imageWidthPx = imageWidthPx,
        imageHeightPx = imageHeightPx,
        createdAtEpochMs = createdAt
    )

    private fun MeasurementEntity.toDomain() = MeasurementAnnotation(
        id = id,
        tool = runCatching { MeasurementTool.valueOf(tool) }.getOrDefault(MeasurementTool.DISTANCE),
        points = Serialization.decodePoints(pointsCsv),
        nucleusPointCount = nucleusPointCount,
        label = label,
        primaryValue = primaryValue,
        secondaryValue = secondaryValue,
        unit = unit,
        calibrated = calibrated
    )
}
