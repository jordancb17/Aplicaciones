package com.hematoscope.app.ui.screens.capture

import android.graphics.Bitmap
import android.view.TextureView
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Usb
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.hematoscope.app.camera.CameraSourceType
import com.hematoscope.app.camera.UsbCameraState
import com.hematoscope.app.domain.measurement.Calibration
import java.util.concurrent.Executors

@Composable
fun CaptureScreen(vm: CaptureViewModel = viewModel()) {
    val message = vm.saveMessage
    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SourceSelector(selected = vm.source, onSelect = vm::setSource)
        ObjectiveSelector(selected = vm.objectiveLabel, onSelect = vm::setObjective)

        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(4f / 3f)
        ) {
            when (vm.source) {
                CameraSourceType.DEVICE -> DevicePreview(vm)
                CameraSourceType.USB_UVC -> UsbPreview(vm)
                CameraSourceType.NETWORK -> NetworkPreview(vm)
            }
        }

        vm.lastCapture?.let { bmp ->
            CapturedThumbnail(bmp = bmp, onSave = vm::saveLastCapture)
        }

        message?.let {
            Text(it, color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun SourceSelector(selected: CameraSourceType, onSelect: (CameraSourceType) -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        CameraSourceType.entries.forEach { src ->
            FilterChip(
                selected = selected == src,
                onClick = { onSelect(src) },
                label = {
                    Text(
                        when (src) {
                            CameraSourceType.DEVICE -> "Dispositivo"
                            CameraSourceType.USB_UVC -> "HY500 / USB"
                            CameraSourceType.NETWORK -> "Red"
                        },
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            )
        }
    }
}

@Composable
private fun ObjectiveSelector(selected: String, onSelect: (String) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(Calibration.COMMON_OBJECTIVES) { (label, _) ->
            FilterChip(
                selected = selected == label,
                onClick = { onSelect(label) },
                label = { Text(label) }
            )
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun DevicePreview(vm: CaptureViewModel) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val permission = rememberPermissionState(android.Manifest.permission.CAMERA)
    val imageCapture = remember { ImageCapture.Builder().build() }
    val executor = remember { Executors.newSingleThreadExecutor() }

    if (!permission.status.isGranted) {
        PermissionPrompt { permission.launchPermissionRequest() }
        return
    }

    Box(Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                val providerFuture = ProcessCameraProvider.getInstance(ctx)
                providerFuture.addListener({
                    val provider = providerFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }
                    provider.unbindAll()
                    provider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageCapture
                    )
                }, ContextCompat.getMainExecutor(ctx))
                previewView
            },
            modifier = Modifier.fillMaxSize()
        )
        CaptureButton {
            imageCapture.takePicture(
                executor,
                object : ImageCapture.OnImageCapturedCallback() {
                    override fun onCaptureSuccess(image: ImageProxy) {
                        val bmp = image.toBitmap()
                        image.close()
                        vm.onDeviceCaptured(bmp)
                    }
                    override fun onError(exception: ImageCaptureException) { /* surfaced elsewhere */ }
                }
            )
        }
    }

    DisposableEffect(Unit) { onDispose { executor.shutdown() } }
}

@Composable
private fun UsbPreview(vm: CaptureViewModel) {
    val status by vm.usbCamera.status.collectAsState()
    Box(Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                TextureView(ctx).also { view -> vm.usbCamera.start(view) }
            },
            modifier = Modifier.fillMaxSize()
        )
        Column(
            Modifier
                .align(Alignment.TopStart)
                .padding(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Usb, contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary)
                Spacer(Modifier.size(6.dp))
                Text(
                    usbStateText(status.state),
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.labelLarge
                )
            }
            status.device?.let {
                Text("VID:PID ${it.vidPidHex}", color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.labelSmall)
            }
            if (status.message.isNotBlank()) {
                Text(status.message, color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.labelSmall)
            }
        }
        CaptureButton { vm.captureFromUsb() }
    }
}

@Composable
private fun NetworkPreview(vm: CaptureViewModel) {
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = vm.networkUrl,
            onValueChange = vm::setNetworkUrl,
            label = { Text("URL MJPEG (PC con la HY500)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = vm::startNetwork) {
                Icon(Icons.Outlined.PlayArrow, contentDescription = null)
                Spacer(Modifier.size(4.dp)); Text("Conectar")
            }
            OutlinedButton(onClick = vm::stopNetwork) { Text("Detener") }
        }
        Box(
            Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            val frame = vm.networkFrame
            if (frame != null) {
                Image(bitmap = frame.asImageBitmap(), contentDescription = "Fotograma en vivo",
                    modifier = Modifier.fillMaxSize())
                CaptureButton { vm.captureFromNetwork() }
            } else {
                Text("Sin transmisión. Indique la URL del re-emisor MJPEG del PC.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.BoxScope.CaptureButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(16.dp)
    ) {
        Icon(Icons.Outlined.CameraAlt, contentDescription = null)
        Spacer(Modifier.size(6.dp))
        Text("Capturar")
    }
}

@Composable
private fun CapturedThumbnail(bmp: Bitmap, onSave: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = "Última captura",
                modifier = Modifier.size(72.dp)
            )
            Column(Modifier.weight(1f)) {
                Text("Última captura", fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.titleSmall)
                Text("${bmp.width} × ${bmp.height} px",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Button(onClick = onSave) {
                Icon(Icons.Outlined.Save, contentDescription = null)
                Spacer(Modifier.size(4.dp)); Text("Guardar")
            }
        }
    }
}

@Composable
private fun PermissionPrompt(onRequest: () -> Unit) {
    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Se necesita acceso a la cámara para capturar campos del microscopio.",
            style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.size(12.dp))
        Button(onClick = onRequest) { Text("Conceder permiso") }
    }
}

private fun usbStateText(state: UsbCameraState): String = when (state) {
    UsbCameraState.IDLE -> "Sin cámara USB"
    UsbCameraState.ATTACHED -> "Detectada"
    UsbCameraState.PERMISSION_DENIED -> "Permiso denegado"
    UsbCameraState.OPENING -> "Abriendo…"
    UsbCameraState.STREAMING -> "En vivo"
    UsbCameraState.ERROR -> "Error"
}
