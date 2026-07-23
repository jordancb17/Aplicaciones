package com.hematoscope.app.ui.screens.capture

import android.app.Application
import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hematoscope.app.HematoScopeApp
import com.hematoscope.app.camera.CameraSourceType
import com.hematoscope.app.camera.NetworkStreamController
import com.hematoscope.app.camera.UsbMicroscopeCamera
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

/**
 * Coordinates the live image sources and persists snapshots. Captures are filed
 * under an auto-created "Captura rápida" case so the flow works before the user
 * formally creates a study.
 */
class CaptureViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = (app as HematoScopeApp).repository

    val usbCamera = UsbMicroscopeCamera(app)
    private val networkController = NetworkStreamController()

    var source by mutableStateOf(CameraSourceType.DEVICE)
        private set

    var objectiveLabel by mutableStateOf("100x (aceite)")
        private set

    var networkUrl by mutableStateOf("http://192.168.1.100:8080/stream")
        private set

    var networkFrame by mutableStateOf<Bitmap?>(null)
        private set

    var lastCapture by mutableStateOf<Bitmap?>(null)
        private set

    var saveMessage by mutableStateOf<String?>(null)
        private set

    private var networkJob: Job? = null

    fun setSource(s: CameraSourceType) {
        if (s != CameraSourceType.NETWORK) stopNetwork()
        source = s
    }

    fun setObjective(o: String) { objectiveLabel = o }
    fun setNetworkUrl(url: String) { networkUrl = url }

    fun startNetwork() {
        stopNetwork()
        networkJob = viewModelScope.launch {
            networkController.frames(networkUrl)
                .catch { saveMessage = "Error de transmisión: ${it.message}" }
                .collect { frame -> networkFrame = frame }
        }
    }

    fun stopNetwork() {
        networkJob?.cancel()
        networkJob = null
    }

    fun onDeviceCaptured(bitmap: Bitmap) {
        lastCapture = bitmap
    }

    fun captureFromUsb() {
        val bmp = usbCamera.captureStill()
        if (bmp != null) lastCapture = bmp
        else saveMessage = "Aún no hay imagen en vivo de la cámara USB"
    }

    fun captureFromNetwork() {
        val frame = networkFrame
        if (frame != null) lastCapture = frame
        else saveMessage = "Sin fotograma de red todavía"
    }

    fun saveLastCapture() {
        val bmp = lastCapture ?: run {
            saveMessage = "No hay imagen para guardar"
            return
        }
        viewModelScope.launch {
            val caseId = ensureQuickCase()
            repository.saveCapture(
                caseId = caseId,
                bitmap = bmp,
                sourceType = source.id,
                objectiveLabel = objectiveLabel
            )
            saveMessage = "Imagen guardada en 'Captura rápida'"
        }
    }

    fun clearMessage() { saveMessage = null }

    private suspend fun ensureQuickCase(): Long {
        // Reuse the quick-capture case created earlier this session; otherwise create it.
        return quickCaseId ?: repository.createCase(
            patientCode = "Captura rápida",
            description = "Campos capturados sin estudio asignado"
        ).also { quickCaseId = it }
    }

    private var quickCaseId: Long? = null

    override fun onCleared() {
        super.onCleared()
        stopNetwork()
        usbCamera.release()
    }
}
