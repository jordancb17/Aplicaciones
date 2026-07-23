package com.hematoscope.app.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** A study: one patient sample / smear under review. */
@Entity(tableName = "cases")
data class CaseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val patientCode: String,
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/** A captured microscopy field belonging to a case. */
@Entity(
    tableName = "captures",
    foreignKeys = [ForeignKey(
        entity = CaseEntity::class,
        parentColumns = ["id"],
        childColumns = ["caseId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("caseId")]
)
data class CaptureEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val caseId: Long,
    val filePath: String,
    val sourceType: String,          // DEVICE | USB_UVC | NETWORK | IMPORTED
    val objectiveLabel: String = "",
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

/** A differential-count session for a case. Tallies stored as JSON map id→count. */
@Entity(
    tableName = "differentials",
    foreignKeys = [ForeignKey(
        entity = CaseEntity::class,
        parentColumns = ["id"],
        childColumns = ["caseId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("caseId")]
)
data class DifferentialEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val caseId: Long,
    val name: String = "Recuento diferencial",
    val targetTotal: Int = 100,
    val talliesJson: String = "{}",   // {"neutrophil_segmented": 42, ...}
    val nrbcCount: Int = 0,
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/** A graded morphological observation for a case. */
@Entity(
    tableName = "observations",
    foreignKeys = [ForeignKey(
        entity = CaseEntity::class,
        parentColumns = ["id"],
        childColumns = ["caseId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("caseId")]
)
data class ObservationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val caseId: Long,
    val descriptorId: String,
    val gradePlus: Int = 0,           // 0..3 for gradable findings; 1 = present
    val present: Boolean = true,
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

/** A saved spatial calibration for an objective. */
@Entity(tableName = "calibrations")
data class CalibrationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val objectiveLabel: String,
    val magnification: Float,
    val micronsPerPixel: Float,
    val referencePixels: Float,
    val referenceMicrons: Float,
    val imageWidthPx: Int,
    val imageHeightPx: Int,
    val createdAt: Long = System.currentTimeMillis()
)

/** A measurement drawn over a capture. Points stored as "x,y;x,y;..." pixels. */
@Entity(
    tableName = "measurements",
    foreignKeys = [ForeignKey(
        entity = CaptureEntity::class,
        parentColumns = ["id"],
        childColumns = ["captureId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("captureId")]
)
data class MeasurementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val captureId: Long,
    val tool: String,
    val pointsCsv: String,
    val nucleusPointCount: Int = 0,
    val label: String = "",
    val primaryValue: Float = 0f,
    val secondaryValue: Float = 0f,
    val unit: String = "px",
    val calibrated: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
