package com.hematoscope.app.ui.screens.comparison

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hematoscope.app.data.model.CellType
import com.hematoscope.app.domain.catalog.CellCatalog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComparisonScreen(onBack: () -> Unit) {
    var left by remember { mutableStateOf<Bitmap?>(null) }
    var right by remember { mutableStateOf<Bitmap?>(null) }
    var reference by remember { mutableStateOf<CellType?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Comparación") },
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
                "Cargue dos campos para compararlos, y elija una célula de referencia " +
                    "para contrastar sus rasgos morfológicos.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ImageSlot("Campo A", left, Modifier.weight(1f)) { left = it }
                ImageSlot("Campo B", right, Modifier.weight(1f)) { right = it }
            }
            ReferenceSelector(selected = reference, onSelect = { reference = it })
            reference?.let { ReferenceCard(it) }
        }
    }
}

@Composable
private fun ImageSlot(
    label: String,
    bitmap: Bitmap?,
    modifier: Modifier = Modifier,
    onPicked: (Bitmap) -> Unit
) {
    val context = LocalContext.current
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream)?.let(onPicked)
        }
    }
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .background(Color.Black)
                .clickable { picker.launch("image/*") },
            contentAlignment = Alignment.Center
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = label,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(Icons.Outlined.AddPhotoAlternate, contentDescription = null,
                    tint = Color.White)
            }
        }
    }
}

@Composable
private fun ReferenceSelector(selected: CellType?, onSelect: (CellType?) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(CellCatalog.cellTypes) { cell ->
            FilterChip(
                selected = selected?.id == cell.id,
                onClick = { onSelect(if (selected?.id == cell.id) null else cell) },
                label = { Text(cell.name, style = MaterialTheme.typography.labelMedium) }
            )
        }
    }
}

@Composable
private fun ReferenceCard(cell: CellType) {
    androidx.compose.material3.Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(24.dp)
                        .background(Color(cell.color), androidx.compose.foundation.shape.CircleShape))
                Spacer(Modifier.size(8.dp))
                Text(cell.name, style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold)
            }
            cell.sizeMicrons?.let {
                Text("Tamaño: ${it.start.toInt()}–${it.endInclusive.toInt()} µm",
                    style = MaterialTheme.typography.bodySmall)
            }
            cell.ncRatio?.let {
                Text("Relación N:C: $it", style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.size(4.dp))
            cell.keyFeatures.forEach { feature ->
                Row { Text("•  "); Text(feature, style = MaterialTheme.typography.bodyMedium) }
            }
        }
    }
}
