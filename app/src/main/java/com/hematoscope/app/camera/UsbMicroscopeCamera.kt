package com.hematoscope.app.camera

import android.content.Context
import android.graphics.Bitmap
import android.hardware.usb.UsbDevice
import android.util.Log
import android.view.Surface
import android.view.TextureView
import com.herohan.uvcapp.CameraHelper
import com.herohan.uvcapp.ICameraHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Drives a UVC (USB Video Class) microscopy camera such as the **Hayear HY500**
 * connected through a USB-OTG adapter.
 *
 * Why UVC and not the vendor's PC software: the HY500's Windows application talks
 * to the camera over the standard UVC protocol. Any UVC-compliant unit is a plain
 * USB webcam at the protocol level, so on Android we can stream it directly with
 * [com.herohan.uvcapp.CameraHelper] — no proprietary driver required. Units whose
 * firmware is *not* UVC-compliant cannot be opened this way; for those the
 * [CameraSourceType.NETWORK] path (re-broadcasting the PC preview as MJPEG) is the
 * fallback.
 *
 * Preview is rendered into a caller-supplied [TextureView]; stills are grabbed
 * from that view via [captureStill], which avoids depending on the library's
 * picture-taking API and works for every backend.
 *
 * All library callbacks arrive on the main thread. This class is not thread-safe;
 * call it from the main thread.
 */
class UsbMicroscopeCamera(private val appContext: Context) {

    private var cameraHelper: ICameraHelper? = null
    private var previewView: TextureView? = null
    private var previewSurface: Surface? = null

    private val _status = MutableStateFlow(UsbCameraStatus())
    val status: StateFlow<UsbCameraStatus> = _status.asStateFlow()

    private val stateCallback = object : ICameraHelper.StateCallback {
        override fun onAttach(device: UsbDevice) {
            _status.value = UsbCameraStatus(UsbCameraState.ATTACHED, device.toInfo(),
                "Cámara detectada: ${device.toInfo().vidPidHex}")
            // Request to open the just-attached device. The library shows the
            // system USB-permission dialog if needed.
            runCatching { cameraHelper?.selectDevice(device) }
                .onFailure { fail("No se pudo seleccionar el dispositivo", it) }
        }

        override fun onDeviceOpen(device: UsbDevice, isFirstOpen: Boolean) {
            _status.value = _status.value.copy(state = UsbCameraState.OPENING,
                message = "Abriendo cámara…")
            runCatching { cameraHelper?.openCamera() }
                .onFailure { fail("No se pudo abrir la cámara", it) }
        }

        override fun onCameraOpen(device: UsbDevice) {
            val helper = cameraHelper ?: return
            runCatching {
                helper.startPreview()
                attachPreviewSurface()
                _status.value = UsbCameraStatus(UsbCameraState.STREAMING, device.toInfo(),
                    "Transmitiendo")
            }.onFailure { fail("No se pudo iniciar la previsualización", it) }
        }

        override fun onCameraClose(device: UsbDevice) {
            detachPreviewSurface()
        }

        override fun onDeviceClose(device: UsbDevice) { /* no-op */ }

        override fun onDetach(device: UsbDevice) {
            _status.value = UsbCameraStatus(UsbCameraState.IDLE, message = "Cámara desconectada")
        }

        override fun onCancel(device: UsbDevice) {
            _status.value = UsbCameraStatus(UsbCameraState.PERMISSION_DENIED, device.toInfo(),
                "Permiso de USB denegado")
        }
    }

    /** Create the helper and begin listening for attach events. Idempotent. */
    fun start(preview: TextureView) {
        previewView = preview
        if (cameraHelper == null) {
            cameraHelper = CameraHelper().apply { setStateCallback(stateCallback) }
        }
        // If a device is already attached, open it right away.
        val existing = cameraHelper?.deviceList?.firstOrNull()
        if (existing != null) {
            runCatching { cameraHelper?.selectDevice(existing) }
        }
    }

    /** Grab the current live frame as a Bitmap, or null if not streaming yet. */
    fun captureStill(): Bitmap? {
        val view = previewView ?: return null
        if (_status.value.state != UsbCameraState.STREAMING) return null
        if (!view.isAvailable) return null
        return runCatching { view.bitmap }.getOrNull()
    }

    /** Stop preview and release the camera, but keep listening for re-attach. */
    fun stop() {
        detachPreviewSurface()
        runCatching { cameraHelper?.stopPreview() }
        runCatching { cameraHelper?.closeCamera() }
        _status.value = _status.value.copy(state = UsbCameraState.IDLE)
    }

    /** Fully release library resources. Call from onDestroy. */
    fun release() {
        stop()
        runCatching { cameraHelper?.release() }
        cameraHelper = null
        previewView = null
    }

    private fun attachPreviewSurface() {
        val view = previewView ?: return
        val texture = view.surfaceTexture ?: return
        val surface = Surface(texture)
        previewSurface = surface
        runCatching { cameraHelper?.addSurface(surface, false) }
            .onFailure { Log.w(TAG, "addSurface failed", it) }
    }

    private fun detachPreviewSurface() {
        val surface = previewSurface ?: return
        runCatching { cameraHelper?.removeSurface(surface) }
        surface.release()
        previewSurface = null
    }

    private fun fail(message: String, t: Throwable) {
        Log.e(TAG, message, t)
        _status.value = _status.value.copy(state = UsbCameraState.ERROR, message = message)
    }

    private fun UsbDevice.toInfo() = UvcDeviceInfo(
        deviceName = deviceName,
        vendorId = vendorId,
        productId = productId,
        productName = productName
    )

    companion object { private const val TAG = "UsbMicroscopeCamera" }
}
