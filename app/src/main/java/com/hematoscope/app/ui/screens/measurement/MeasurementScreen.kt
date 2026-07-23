package com.hematoscope.app.ui.screens.measurement

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
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Undo
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hematoscope.app.data.model.MeasurementTool
import kotlin.math.min

/** Fit transform (scale + centering offset) mapping image pixels to view pixels. */
private data class FitTransform(val scale: Float, val dx: Float, val dy: Float) {
    fun viewToImage(p: Offset) = Offset((p.x - dx) / scale, (p.y - dy) / scale)
    fun imageToView(p: Offset) = Offset(p.x * scale + dx, p.y * scale + dy)
}

private fun computeFit(container: IntSize, imgW: Int, imgH: Int): FitTransform {
    if (imgW == 0 || imgH == 0) return FitTransform(1f, 0f, 0f)
    val scale = min(container.width.toFloat() / imgW, container.height.toFloat() / imgH)
    val dx = (container.width - imgW * scale) / 2f
    val dy = (container.height - imgH * scale) / 2f
    return FitTransform(scale, dx, dy)
}

@Composable
fun MeasurementScreen(vm: MeasurementViewModel = viewModel()) {
    val context = LocalContext.current
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream)?.let(vm::loadImage)
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        ToolRow(selected = vm.tool, onSelect = vm::selectTool)
        CalibrationRow(vm)
        AutoNcToggle(vm)

        val bmp = vm.image
        if (bmp == null) {
            EmptyImagePrompt { picker.launch("image/*") }
        } else {
            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(bmp.width.toFloat() / bmp.height)
                    .background(Color.Black)
            ) {
                val imgBitmap = bmp.asImageBitmap()
                Canvas(
                    Modifier
                        .fillMaxSize()
                        .pointerInput(vm.tool, bmp, vm.autoNc) {
                            detectTapGestures { tap ->
                                val fit = computeFit(size, bmp.width, bmp.height)
                                val imagePoint = fit.viewToImage(tap)
                                if (vm.autoNc) vm.runAutoSegmentation(imagePoint)
                                else vm.addPoint(imagePoint)
                            }
                        }
                ) {
                    val fit = computeFit(
                        IntSize(size.width.toInt(), size.height.toInt()),
                        bmp.width, bmp.height
                    )
                    drawImage(
                        image = imgBitmap,
                        dstSize = androidx.compose.ui.unit.IntSize(
                            (bmp.width * fit.scale).toInt(),
                            (bmp.height * fit.scale).toInt()
                        ),
                        dstOffset = androidx.compose.ui.unit.IntOffset(fit.dx.toInt(), fit.dy.toInt())
                    )

                    // Committed annotations.
                    vm.annotations.forEach { ann ->
                        drawMeasurement(
                            ann.points.map { fit.imageToView(it) },
                            ann.tool,
                            ann.nucleusPointCount,
                            Color(0xFF00E5FF)
                        )
                    }
                    // In-progress measurement.
                    drawMeasurement(
                        vm.currentPoints.map { fit.imageToView(it) },
                        vm.tool,
                        vm.nucleusPointCount,
                        Color(0xFFFFEB3B)
                    )

                    // Assisted N:C segmentation overlay.
                    val seg = vm.segResult
                    if (vm.autoNc && seg != null) {
                        drawImage(
                            image = seg.overlay.asImageBitmap(),
                            dstSize = androidx.compose.ui.unit.IntSize(
                                (seg.overlay.width * fit.scale).toInt(),
                                (seg.overlay.height * fit.scale).toInt()
                            ),
                            dstOffset = androidx.compose.ui.unit.IntOffset(
                                (seg.overlayLeft * fit.scale + fit.dx).toInt(),
                                (seg.overlayTop * fit.scale + fit.dy).toInt()
                            )
                        )
                    }
                }
            }
            if (vm.autoNc) {
                SegmentationPanel(vm) { picker.launch("image/*") }
            } else {
                LiveResultCard(vm)
                ActionRow(vm) { picker.launch("image/*") }
                AnnotationList(vm)
            }
        }
    }
}

@Composable
private fun ToolRow(selected: MeasurementTool, onSelect: (MeasurementTool) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(MeasurementTool.entries) { tool ->
            FilterChip(
                selected = selected == tool,
                onClick = { onSelect(tool) },
                label = { Text(tool.displayName, style = MaterialTheme.typography.labelMedium) }
            )
        }
    }
}

@Composable
private fun CalibrationRow(vm: MeasurementViewModel) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Escala:", style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.size(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                FilterChip(
                    selected = vm.calibration == null,
                    onClick = { vm.selectCalibration(null) },
                    label = { Text("Píxeles") }
                )
            }
            items(vm.calibrations) { cal ->
                FilterChip(
                    selected = vm.calibration == cal,
                    onClick = { vm.selectCalibration(cal) },
                    label = { Text("${cal.objectiveLabel} · %.3f µm/px".format(cal.micronsPerPixel)) }
                )
            }
        }
    }
}

@Composable
private fun AutoNcToggle(vm: MeasurementViewModel) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            "N:C automática (segmentación)",
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold
        )
        Switch(checked = vm.autoNc, onCheckedChange = vm::switchAutoNc)
    }
}

@Composable
private fun SegmentationPanel(vm: MeasurementViewModel, onPick: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Text(
                "Segmentación N:C asistida",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                vm.segSummary ?: "Toque una célula para estimar su relación núcleo/citoplasma.",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            vm.segDiameterMicrons?.let { diam ->
                Text(
                    "Ø célula ≈ %.1f µm (equivalente)".format(diam),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            if (vm.segSuggestions.isNotEmpty()) {
                Spacer(Modifier.size(6.dp))
                Text(
                    "Sugerencias morfométricas",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                vm.segSuggestions.forEach { s ->
                    Row(Modifier.fillMaxWidth()) {
                        Text(
                            s.cell.name,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            "${(s.score * 100f).toInt()} %",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                Text(
                    if (vm.segDiameterMicrons == null)
                        "Solo N:C (calibre un objetivo para incluir también el tamaño)."
                    else "Combina tamaño y N:C. No distingue por granulación.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.size(8.dp))
            Text(
                "Radio de análisis: ${vm.segRadius.toInt()} px",
                style = MaterialTheme.typography.bodySmall
            )
            Slider(
                value = vm.segRadius,
                onValueChange = vm::updateSegRadius,
                valueRange = 20f..200f
            )
            Text(
                "Violeta = núcleo · cian = citoplasma (estimación automática, no diagnóstica).",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.size(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { vm.clearSegmentation() }) {
                    Icon(Icons.Outlined.Clear, contentDescription = null)
                    Spacer(Modifier.size(4.dp)); Text("Limpiar")
                }
                OutlinedButton(onClick = onPick) {
                    Icon(Icons.Outlined.AddPhotoAlternate, contentDescription = null)
                    Spacer(Modifier.size(4.dp)); Text("Otra imagen")
                }
            }
        }
    }
}

@Composable
private fun LiveResultCard(vm: MeasurementViewModel) {
    val result = vm.liveResult()
    Card(Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(vm.tool.displayName, style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary)
                Text(
                    result.summary.ifBlank { "Toque la imagen para medir" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                if (!result.calibrated && vm.tool != MeasurementTool.ANGLE &&
                    vm.tool != MeasurementTool.NC_RATIO) {
                    Text("Sin calibración: resultado en píxeles",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error)
                }
            }
            if (vm.tool == MeasurementTool.NC_RATIO && vm.nucleusPointCount == 0) {
                OutlinedButton(onClick = vm::finishNucleus) { Text("Núcleo listo") }
            }
        }
    }
}

@Composable
private fun ActionRow(vm: MeasurementViewModel, onPick: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = vm::commit) {
            Icon(Icons.Outlined.Check, contentDescription = null)
            Spacer(Modifier.size(4.dp)); Text("Fijar")
        }
        OutlinedButton(onClick = vm::undoPoint) {
            Icon(Icons.Outlined.Undo, contentDescription = null)
            Spacer(Modifier.size(4.dp)); Text("Deshacer")
        }
        OutlinedButton(onClick = vm::clearAll) {
            Icon(Icons.Outlined.Clear, contentDescription = null)
            Spacer(Modifier.size(4.dp)); Text("Limpiar")
        }
        OutlinedButton(onClick = onPick) {
            Icon(Icons.Outlined.AddPhotoAlternate, contentDescription = null)
        }
    }
}

@Composable
private fun AnnotationList(vm: MeasurementViewModel) {
    if (vm.annotations.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        vm.annotations.forEachIndexed { index, ann ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                AssistChip(onClick = {}, label = { Text("${index + 1}") })
                Spacer(Modifier.size(8.dp))
                Text(ann.label, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun EmptyImagePrompt(onPick: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(4f / 3f)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Outlined.AddPhotoAlternate, contentDescription = null,
                modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.size(8.dp))
            Text("Cargue un campo capturado o una imagen para medir",
                style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.size(8.dp))
            Button(onClick = onPick) { Text("Elegir imagen") }
        }
    }
}

/** Draw the geometry of one measurement in view coordinates. */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawMeasurement(
    points: List<Offset>,
    tool: MeasurementTool,
    nucleusPointCount: Int,
    color: Color
) {
    if (points.isEmpty()) return
    val strokePx = 3f
    points.forEach { drawCircle(color, radius = 6f, center = it) }

    when (tool) {
        MeasurementTool.DISTANCE, MeasurementTool.POLYLINE, MeasurementTool.ANGLE -> {
            for (i in 1 until points.size) {
                drawLine(color, points[i - 1], points[i], strokeWidth = strokePx)
            }
        }
        MeasurementTool.AREA -> {
            for (i in 1 until points.size) {
                drawLine(color, points[i - 1], points[i], strokeWidth = strokePx)
            }
            if (points.size >= 3) drawLine(color, points.last(), points.first(), strokeWidth = strokePx)
        }
        MeasurementTool.CIRCLE -> {
            if (points.size >= 2) {
                val center = points[0]
                val radius = (points[1] - points[0]).getDistance()
                drawCircle(color, radius = radius, center = center, style = Stroke(strokePx))
            }
        }
        MeasurementTool.NC_RATIO -> {
            val nucleus = if (nucleusPointCount > 0) points.take(nucleusPointCount) else points
            val cell = if (nucleusPointCount > 0) points.drop(nucleusPointCount) else emptyList()
            drawClosed(nucleus, Color(0xFF8E24AA), strokePx)   // nucleus in violet
            drawClosed(cell, color, strokePx)
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawClosed(
    points: List<Offset>, color: Color, strokePx: Float
) {
    if (points.size < 2) return
    for (i in 1 until points.size) drawLine(color, points[i - 1], points[i], strokeWidth = strokePx)
    if (points.size >= 3) drawLine(color, points.last(), points.first(), strokeWidth = strokePx)
}
