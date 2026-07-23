package com.hematoscope.app.camera

/** The three ways HematoScope can obtain a live microscopy image. */
enum class CameraSourceType(val id: String, val label: String) {
    /** Phone/tablet rear camera aligned to the eyepiece with a mechanical adapter. */
    DEVICE("DEVICE", "Cámara del dispositivo (ocular)"),

    /** USB Video Class camera over OTG — the Hayear HY500 path. */
    USB_UVC("USB_UVC", "Cámara USB / HY500 (UVC)"),

    /** MJPEG/HTTP stream, e.g. re-broadcast from the vendor PC software. */
    NETWORK("NETWORK", "Transmisión de red (MJPEG)")
}

/** Connection lifecycle for the USB camera path. */
enum class UsbCameraState {
    IDLE,             // nothing attached / not started
    ATTACHED,         // device present, awaiting permission/open
    PERMISSION_DENIED,
    OPENING,
    STREAMING,        // live preview running
    ERROR
}

/** Human-readable info about a detected USB camera, for the UI and diagnostics. */
data class UvcDeviceInfo(
    val deviceName: String,
    val vendorId: Int,
    val productId: Int,
    val productName: String?
) {
    /** VID:PID in the conventional hexadecimal form (e.g. "0bda:5830"). */
    val vidPidHex: String
        get() = "%04x:%04x".format(vendorId, productId)
}

/** Snapshot of the USB camera controller exposed to the UI. */
data class UsbCameraStatus(
    val state: UsbCameraState = UsbCameraState.IDLE,
    val device: UvcDeviceInfo? = null,
    val message: String = ""
)
