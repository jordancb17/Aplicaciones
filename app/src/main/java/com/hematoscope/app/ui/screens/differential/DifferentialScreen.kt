package com.hematoscope.app.ui.screens.differential

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Undo
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hematoscope.app.data.model.CellType

@Composable
fun DifferentialScreen(vm: DifferentialViewModel = viewModel()) {
    val rows = vm.rows()
    val progress = if (vm.targetTotal > 0)
        (vm.wbcTotal.toFloat() / vm.targetTotal).coerceIn(0f, 1f) else 0f

    Column(Modifier.fillMaxSize()) {
        SummaryBar(vm = vm, progress = progress)

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(vm.wbcKeys) { cell ->
                val row = rows.firstOrNull { it.cell.id == cell.id }
                CounterKey(
                    cell = cell,
                    count = row?.count ?: 0,
                    percent = row?.percent ?: 0f,
                    onClick = { vm.increment(cell.id) }
                )
            }
            vm.nrbcKey?.let { nrbc ->
                items(listOf(nrbc)) { cell ->
                    CounterKey(
                        cell = cell,
                        count = vm.nrbcCount,
                        percent = -1f, // shown as "/100 WBC" below
                        subLabel = "%.1f /100 GB".format(vm.nrbcPer100Wbc),
                        onClick = { vm.incrementNrbc() }
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryBar(vm: DifferentialViewModel, progress: Float) {
    Surface(tonalElevation = 3.dp) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "Leucocitos contados: ${vm.wbcTotal} / ${vm.targetTotal}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (vm.isComplete) {
                        Text("Recuento completo ✓", color = MaterialTheme.colorScheme.tertiary,
                            style = MaterialTheme.typography.labelLarge)
                    }
                }
                IconButton(onClick = { vm.undo() }) {
                    Icon(Icons.Outlined.Undo, contentDescription = "Deshacer")
                }
                IconButton(onClick = { vm.reset() }) {
                    Icon(Icons.Outlined.Refresh, contentDescription = "Reiniciar")
                }
            }
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp)
            )
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(100, 200).forEach { target ->
                    FilterChip(
                        selected = vm.targetTotal == target,
                        onClick = { vm.setTarget(target) },
                        label = { Text("$target células") }
                    )
                }
            }
        }
    }
}

@Composable
private fun CounterKey(
    cell: CellType,
    count: Int,
    percent: Float,
    subLabel: String? = null,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                Modifier
                    .size(30.dp)
                    .background(Color(cell.color), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(cell.shortcut, color = Color.White, fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelMedium)
            }
            Text(
                cell.name,
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
                maxLines = 2
            )
            Text(
                "$count",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                subLabel ?: (if (percent >= 0f) "%.0f %%".format(percent) else ""),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
