package com.hematoscope.app.ui.screens.calibration

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hematoscope.app.domain.measurement.Calibration
import kotlin.math.min

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalibrationScreen(onBack: () -> Unit, vm: CalibrationViewModel = viewModel()) {
    val context = LocalContext.current
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream)?.let(vm::setImage)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Calibración µm/píxel") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Fotografíe una platina micrométrica con el objetivo elegido, trace una " +
                    "línea sobre una distancia conocida e introduzca su valor en µm.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(Calibration.COMMON_OBJECTIVES) { (label, mag) ->
                    FilterChip(
                        selected = vm.objectiveLabel == label,
                        onClick = { vm.setObjective(label, mag) },
                        label = { Text(label) }
                    )
                }
            }

            val bmp = vm.image
            if (bmp == null) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(4f / 3f)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Outlined.AddPhotoAlternate, contentDescription = null,
                            modifier = Modifier.size(40.dp))
                        Spacer(Modifier.size(8.dp))
                        Button(onClick = { picker.launch("image/*") }) {
                            Text("Cargar imagen de la platina")
                        }
                    }
                }
            } else {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(bmp.width.toFloat() / bmp.height)
                        .background(Color.Black)
                ) {
                    val img = bmp.asImageBitmap()
                    Canvas(
                        Modifier
                            .fillMaxSize()
                            .pointerInput(bmp) {
                                detectTapGestures { tap ->
                                    val fit = fitOf(size, bmp.width, bmp.height)
                                    vm.addPoint(viewToImage(fit, tap))
                                }
                            }
                    ) {
                        val fit = fitOf(IntSize(size.width.toInt(), size.height.toInt()),
                            bmp.width, bmp.height)
                        drawImage(
                            image = img,
                            dstSize = IntSize((bmp.width * fit.scale).toInt(),
                                (bmp.height * fit.scale).toInt()),
                            dstOffset = IntOffset(fit.dx.toInt(), fit.dy.toInt())
                        )
                        val viewPts = vm.points.map { imageToView(fit, it) }
                        viewPts.forEach { drawCircle(Color(0xFFFFEB3B), radius = 7f, center = it) }
                        if (viewPts.size == 2) {
                            drawLine(Color(0xFFFFEB3B), viewPts[0], viewPts[1], strokeWidth = 4f)
                        }
                    }
                }

                OutlinedTextField(
                    value = vm.knownMicronsText,
                    onValueChange = { vm.knownMicronsText = it },
                    label = { Text("Distancia real (µm)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                vm.previewMicronsPerPixel()?.let {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            Text("Escala calculada", style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary)
                            Text("%.4f µm/píxel".format(it),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold)
                            Text("Línea: %.0f px".format(vm.measuredPixels),
                                style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = vm::save) {
                        Icon(Icons.Outlined.Save, contentDescription = null)
                        Spacer(Modifier.size(4.dp)); Text("Guardar calibración")
                    }
                    OutlinedButton(onClick = vm::clearPoints) { Text("Rehacer línea") }
                }
            }

            vm.message?.let {
                Text(it, color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyMedium)
            }

            if (vm.saved.isNotEmpty()) {
                Text("Calibraciones guardadas", style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold)
                vm.saved.forEach { cal ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            Text(cal.objectiveLabel, fontWeight = FontWeight.SemiBold)
                            Text("%.4f µm/px · ref %.0f px = %.0f µm".format(
                                cal.micronsPerPixel, cal.referencePixels, cal.referenceMicrons),
                                style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

// --- Fit helpers (duplicated locally to keep the screen self-contained) ---
private data class Fit(val scale: Float, val dx: Float, val dy: Float)

private fun fitOf(container: IntSize, imgW: Int, imgH: Int): Fit {
    if (imgW == 0 || imgH == 0) return Fit(1f, 0f, 0f)
    val scale = min(container.width.toFloat() / imgW, container.height.toFloat() / imgH)
    return Fit(scale, (container.width - imgW * scale) / 2f, (container.height - imgH * scale) / 2f)
}

private fun viewToImage(fit: Fit, p: Offset) = Offset((p.x - fit.dx) / fit.scale, (p.y - fit.dy) / fit.scale)
private fun imageToView(fit: Fit, p: Offset) = Offset(p.x * fit.scale + fit.dx, p.y * fit.scale + fit.dy)
