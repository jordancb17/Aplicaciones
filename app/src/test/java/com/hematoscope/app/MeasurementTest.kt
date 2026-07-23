package com.hematoscope.app

import androidx.compose.ui.geometry.Offset
import com.hematoscope.app.data.model.MeasurementTool
import com.hematoscope.app.domain.measurement.Calibration
import com.hematoscope.app.domain.measurement.Geometry
import com.hematoscope.app.domain.measurement.MeasurementEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MeasurementTest {

    @Test
    fun distance_isEuclidean() {
        val d = Geometry.distance(Offset(0f, 0f), Offset(3f, 4f))
        assertEquals(5f, d, 1e-4f)
    }

    @Test
    fun polygonArea_unitSquare() {
        val square = listOf(Offset(0f, 0f), Offset(10f, 0f), Offset(10f, 10f), Offset(0f, 10f))
        assertEquals(100f, Geometry.polygonArea(square), 1e-3f)
    }

    @Test
    fun angle_rightAngleIs90() {
        val deg = Geometry.angleDegrees(Offset(1f, 0f), Offset(0f, 0f), Offset(0f, 1f))
        assertEquals(90f, deg, 1e-3f)
    }

    @Test
    fun calibration_fromReference_convertsPixelsToMicrons() {
        // 200 px drawn over a known 100 µm scale → 0.5 µm/px.
        val cal = Calibration.fromReference("40x", 40f, measuredPixels = 200f, knownMicrons = 100f)
        assertEquals(0.5f, cal.micronsPerPixel, 1e-4f)
        assertEquals(50f, cal.toMicrons(100f), 1e-4f) // 100 px → 50 µm
    }

    @Test
    fun engine_distanceUsesCalibrationWhenPresent() {
        val cal = Calibration.fromReference("100x", 100f, measuredPixels = 100f, knownMicrons = 10f)
        val result = MeasurementEngine.evaluate(
            MeasurementTool.DISTANCE,
            listOf(Offset(0f, 0f), Offset(100f, 0f)),
            nucleusPointCount = 0,
            calibration = cal
        )
        assertTrue(result.calibrated)
        assertEquals(10f, result.primaryValue, 1e-3f) // 100 px * 0.1 µm/px
        assertEquals("µm", result.unit)
    }

    @Test
    fun engine_ncRatio_isDimensionlessAndReasonable() {
        // Nucleus: 10x10 square (area 100). Whole cell: 20x20 square (area 400).
        // Cytoplasm area = 300, so N:C = 100/300 ≈ 0.333.
        val nucleus = listOf(Offset(0f, 0f), Offset(10f, 0f), Offset(10f, 10f), Offset(0f, 10f))
        val cell = listOf(Offset(0f, 0f), Offset(20f, 0f), Offset(20f, 20f), Offset(0f, 20f))
        val result = MeasurementEngine.evaluate(
            MeasurementTool.NC_RATIO,
            nucleus + cell,
            nucleusPointCount = nucleus.size,
            calibration = null
        )
        assertEquals(0.333f, result.primaryValue, 1e-2f)
    }
}
