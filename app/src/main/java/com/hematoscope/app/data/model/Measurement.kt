package com.hematoscope.app.data.model

import androidx.compose.ui.geometry.Offset

/** Kind of measurement the user can draw on a captured field. */
enum class MeasurementTool(val displayName: String, val minPoints: Int) {
    DISTANCE("Distancia / diámetro", 2),
    POLYLINE("Longitud (polilínea)", 2),
    AREA("Área (polígono)", 3),
    CIRCLE("Círculo (2 puntos = diámetro)", 2),
    ANGLE("Ángulo (3 puntos)", 3),
    NC_RATIO("Relación núcleo/citoplasma", 6)
}

/** Which sub-region the next tap belongs to when measuring an N:C ratio. */
enum class NcTarget { NUCLEUS, CYTOPLASM }

/**
 * A single measurement drawn over a captured image, stored in image-pixel
 * coordinates so it stays valid regardless of on-screen zoom/pan.
 *
 * [primaryValue] / [secondaryValue] hold the derived quantities already
 * converted to real units when a calibration is available (µm, µm², degrees,
 * or a dimensionless ratio); [unit] documents which.
 */
data class MeasurementAnnotation(
    val id: Long = 0,
    val tool: MeasurementTool,
    val points: List<Offset>,
    /** For N:C ratio: indices in [points] that belong to the nucleus outline. */
    val nucleusPointCount: Int = 0,
    val label: String = "",
    val primaryValue: Float = 0f,
    val secondaryValue: Float = 0f,
    val unit: String = "px",
    val calibrated: Boolean = false
)
