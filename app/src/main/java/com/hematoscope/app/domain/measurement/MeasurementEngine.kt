package com.hematoscope.app.domain.measurement

import androidx.compose.ui.geometry.Offset
import com.hematoscope.app.data.model.MeasurementAnnotation
import com.hematoscope.app.data.model.MeasurementTool

/** Result of evaluating a measurement, with human-readable formatting. */
data class MeasurementResult(
    val primaryValue: Float,
    val secondaryValue: Float,
    val unit: String,
    val calibrated: Boolean,
    val summary: String
)

/**
 * Turns raw drawn points into physical quantities, applying [Calibration]
 * when present. All input points are in image-pixel coordinates.
 */
object MeasurementEngine {

    fun evaluate(
        tool: MeasurementTool,
        points: List<Offset>,
        nucleusPointCount: Int,
        calibration: Calibration?
    ): MeasurementResult {
        val cal = calibration?.takeIf { it.isValid }
        return when (tool) {
            MeasurementTool.DISTANCE -> distance(points, cal)
            MeasurementTool.POLYLINE -> polyline(points, cal)
            MeasurementTool.AREA -> area(points, cal)
            MeasurementTool.CIRCLE -> circle(points, cal)
            MeasurementTool.ANGLE -> angle(points)
            MeasurementTool.NC_RATIO -> ncRatio(points, nucleusPointCount, cal)
        }
    }

    fun apply(annotation: MeasurementAnnotation, calibration: Calibration?): MeasurementAnnotation {
        val r = evaluate(annotation.tool, annotation.points, annotation.nucleusPointCount, calibration)
        return annotation.copy(
            primaryValue = r.primaryValue,
            secondaryValue = r.secondaryValue,
            unit = r.unit,
            calibrated = r.calibrated,
            label = annotation.label.ifBlank { r.summary }
        )
    }

    private fun distance(points: List<Offset>, cal: Calibration?): MeasurementResult {
        val px = if (points.size >= 2) Geometry.distance(points[0], points[1]) else 0f
        return if (cal != null) {
            val um = cal.toMicrons(px)
            MeasurementResult(um, 0f, "µm", true, "%.2f µm".format(um))
        } else {
            MeasurementResult(px, 0f, "px", false, "%.0f px".format(px))
        }
    }

    private fun polyline(points: List<Offset>, cal: Calibration?): MeasurementResult {
        val px = Geometry.polylineLength(points)
        return if (cal != null) {
            val um = cal.toMicrons(px)
            MeasurementResult(um, 0f, "µm", true, "%.2f µm".format(um))
        } else {
            MeasurementResult(px, 0f, "px", false, "%.0f px".format(px))
        }
    }

    private fun area(points: List<Offset>, cal: Calibration?): MeasurementResult {
        val areaPx = Geometry.polygonArea(points)
        val diamPx = Geometry.equivalentDiameter(areaPx)
        return if (cal != null) {
            val areaUm = cal.toSquareMicrons(areaPx)
            val diamUm = cal.toMicrons(diamPx)
            MeasurementResult(
                areaUm, diamUm, "µm²", true,
                "Área %.1f µm² · Ø equiv. %.1f µm".format(areaUm, diamUm)
            )
        } else {
            MeasurementResult(
                areaPx, diamPx, "px²", false,
                "Área %.0f px² · Ø equiv. %.0f px".format(areaPx, diamPx)
            )
        }
    }

    private fun circle(points: List<Offset>, cal: Calibration?): MeasurementResult {
        val diamPx = if (points.size >= 2) Geometry.distance(points[0], points[1]) else 0f
        val radiusPx = diamPx / 2f
        val areaPx = (Math.PI.toFloat()) * radiusPx * radiusPx
        return if (cal != null) {
            val diamUm = cal.toMicrons(diamPx)
            val areaUm = cal.toSquareMicrons(areaPx)
            MeasurementResult(
                diamUm, areaUm, "µm", true,
                "Ø %.2f µm · Área %.1f µm²".format(diamUm, areaUm)
            )
        } else {
            MeasurementResult(
                diamPx, areaPx, "px", false,
                "Ø %.0f px".format(diamPx)
            )
        }
    }

    private fun angle(points: List<Offset>): MeasurementResult {
        val deg = if (points.size >= 3) Geometry.angleDegrees(points[0], points[1], points[2]) else 0f
        return MeasurementResult(deg, 0f, "°", true, "%.1f°".format(deg))
    }

    /**
     * Nucleus-to-cytoplasm ratio. The first [nucleusPointCount] points outline
     * the nucleus; the remainder outline the whole cell. The ratio is
     * nucleus area ÷ (cell area − nucleus area) and is dimensionless, so no
     * calibration is required — but a calibration lets us also report areas.
     */
    private fun ncRatio(
        points: List<Offset>,
        nucleusPointCount: Int,
        cal: Calibration?
    ): MeasurementResult {
        if (nucleusPointCount < 3 || points.size - nucleusPointCount < 3) {
            return MeasurementResult(0f, 0f, "N:C", false, "Trace núcleo y célula")
        }
        val nucleus = points.subList(0, nucleusPointCount)
        val cell = points.subList(nucleusPointCount, points.size)
        val nucleusArea = Geometry.polygonArea(nucleus)
        val cellArea = Geometry.polygonArea(cell)
        val cytoplasmArea = (cellArea - nucleusArea).coerceAtLeast(1f)
        val ratio = nucleusArea / cytoplasmArea
        val nucleusFraction = if (cellArea > 0f) nucleusArea / cellArea else 0f
        val summary = "N:C ≈ %.2f (núcleo %.0f%% de la célula)".format(ratio, nucleusFraction * 100f)
        return MeasurementResult(ratio, nucleusFraction, "N:C", cal != null, summary)
    }
}
