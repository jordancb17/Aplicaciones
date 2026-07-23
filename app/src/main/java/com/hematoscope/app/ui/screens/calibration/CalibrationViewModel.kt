package com.hematoscope.app.ui.screens.calibration

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
import com.hematoscope.app.domain.measurement.Calibration
import com.hematoscope.app.domain.measurement.Geometry
import kotlinx.coroutines.launch

/**
 * Builds spatial calibrations from a reference line drawn over a stage
 * micrometer (or from the red-cell internal standard). Points are in image
 * pixels; the screen converts taps before calling [addPoint].
 */
class CalibrationViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = (app as HematoScopeApp).repository

    val saved = mutableStateListOf<Calibration>()

    var image by mutableStateOf<Bitmap?>(null)
        private set

    val points = mutableStateListOf<Offset>()

    var objectiveLabel by mutableStateOf(Calibration.COMMON_OBJECTIVES[4].first) // 100x
        private set
    var magnification by mutableStateOf(Calibration.COMMON_OBJECTIVES[4].second)
        private set

    /** Known real length of the drawn reference, in micrometres. */
    var knownMicronsText by mutableStateOf("100")

    var message by mutableStateOf<String?>(null)
        private set

    init {
        viewModelScope.launch {
            repository.observeCalibrations().collect { list ->
                saved.clear(); saved.addAll(list)
            }
        }
    }

    fun loadImage(b: Bitmap) { image = b; points.clear() }

    fun setObjective(label: String, mag: Float) {
        objectiveLabel = label; magnification = mag
    }

    fun addPoint(p: Offset) {
        if (points.size >= 2) points.clear()
        points.add(p)
    }

    fun clearPoints() = points.clear()

    val measuredPixels: Float
        get() = if (points.size == 2) Geometry.distance(points[0], points[1]) else 0f

    /** Live preview of the scale that would be saved. */
    fun previewMicronsPerPixel(): Float? {
        val known = knownMicronsText.toFloatOrNull() ?: return null
        val px = measuredPixels
        return if (px > 0f && known > 0f) known / px else null
    }

    fun save() {
        val known = knownMicronsText.toFloatOrNull()
        val px = measuredPixels
        if (known == null || known <= 0f) { message = "Introduzca la distancia real en µm"; return }
        if (px <= 0f) { message = "Trace la línea de referencia (2 puntos)"; return }
        val bmp = image
        val cal = Calibration.fromReference(
            objectiveLabel = objectiveLabel,
            magnification = magnification,
            measuredPixels = px,
            knownMicrons = known,
            imageWidthPx = bmp?.width ?: 0,
            imageHeightPx = bmp?.height ?: 0
        )
        viewModelScope.launch {
            repository.saveCalibration(cal)
            message = "Calibración guardada: %.4f µm/px (%s)".format(cal.micronsPerPixel, objectiveLabel)
            points.clear()
        }
    }

    fun clearMessage() { message = null }
}
