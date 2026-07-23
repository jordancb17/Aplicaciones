package com.hematoscope.app.ui.screens.measurement

import android.app.Application
import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hematoscope.app.HematoScopeApp
import com.hematoscope.app.data.model.MeasurementAnnotation
import com.hematoscope.app.data.model.MeasurementTool
import com.hematoscope.app.domain.measurement.Calibration
import com.hematoscope.app.domain.measurement.MeasurementEngine
import com.hematoscope.app.domain.measurement.MeasurementResult
import kotlinx.coroutines.launch

/**
 * State holder for the interactive measurement canvas. Points are stored in
 * **image-pixel** coordinates so results stay valid regardless of on-screen
 * scaling; the screen converts taps to image space before calling [addPoint].
 */
class MeasurementViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = (app as HematoScopeApp).repository

    var image by mutableStateOf<Bitmap?>(null)
        private set

    var tool by mutableStateOf(MeasurementTool.DISTANCE)
        private set

    var calibration by mutableStateOf<Calibration?>(null)
        private set

    val calibrations = mutableStateListOf<Calibration>()

    /** Points of the in-progress measurement (image coords). */
    val currentPoints = mutableStateListOf<Offset>()

    /** For N:C: how many leading points belong to the nucleus (0 = still tracing it). */
    var nucleusPointCount by mutableStateOf(0)
        private set

    /** Committed measurements shown on the overlay. */
    val annotations = mutableStateListOf<MeasurementAnnotation>()

    init {
        viewModelScope.launch {
            repository.observeCalibrations().collect { list ->
                calibrations.clear()
                calibrations.addAll(list)
                if (calibration == null) calibration = list.firstOrNull()
            }
        }
    }

    fun setImage(bitmap: Bitmap) {
        image = bitmap
        clearAll()
    }

    fun setTool(t: MeasurementTool) {
        tool = t
        currentPoints.clear()
        nucleusPointCount = 0
    }

    fun setCalibration(c: Calibration?) { calibration = c }

    fun addPoint(imagePoint: Offset) {
        // For simple two-point tools, cap at the required number of points.
        val cap = when (tool) {
            MeasurementTool.DISTANCE, MeasurementTool.CIRCLE -> 2
            MeasurementTool.ANGLE -> 3
            else -> Int.MAX_VALUE
        }
        if (currentPoints.size < cap) currentPoints.add(imagePoint)
    }

    /** N:C tool: mark the nucleus outline complete; subsequent taps trace the cell. */
    fun finishNucleus() {
        if (tool == MeasurementTool.NC_RATIO && currentPoints.size >= 3) {
            nucleusPointCount = currentPoints.size
        }
    }

    fun undoPoint() {
        if (currentPoints.isNotEmpty()) {
            currentPoints.removeAt(currentPoints.lastIndex)
            if (currentPoints.size < nucleusPointCount) nucleusPointCount = 0
        }
    }

    /** Evaluate the current in-progress points without committing. */
    fun liveResult(): MeasurementResult =
        MeasurementEngine.evaluate(tool, currentPoints.toList(), nucleusPointCount, calibration)

    fun commit() {
        if (currentPoints.size < tool.minPoints) return
        val base = MeasurementAnnotation(
            tool = tool,
            points = currentPoints.toList(),
            nucleusPointCount = nucleusPointCount
        )
        annotations.add(MeasurementEngine.apply(base, calibration))
        currentPoints.clear()
        nucleusPointCount = 0
    }

    fun clearAll() {
        currentPoints.clear()
        annotations.clear()
        nucleusPointCount = 0
    }
}
