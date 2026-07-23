package com.hematoscope.app.ui.screens.atlas

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hematoscope.app.data.model.CellCategory
import com.hematoscope.app.data.model.CellType
import com.hematoscope.app.data.model.MorphologyDescriptor
import com.hematoscope.app.domain.catalog.CellCatalog

@Composable
fun AtlasScreen() {
    var tab by remember { mutableIntStateOf(0) }
    var query by remember { mutableStateOf("") }
    var category by remember { mutableStateOf<CellCategory?>(null) }

    Column(Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = tab) {
            Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Células") })
            Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Descriptores") })
        }

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
            placeholder = { Text("Buscar (p. ej. esquistocito, blasto, Auer)") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )

        CategoryFilterRow(selected = category, onSelect = { category = it })

        if (tab == 0) {
            CellList(query, category)
        } else {
            DescriptorList(query, category)
        }
    }
}

@Composable
private fun CategoryFilterRow(selected: CellCategory?, onSelect: (CellCategory?) -> Unit) {
    androidx.compose.foundation.lazy.LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            FilterChip(selected = selected == null, onClick = { onSelect(null) },
                label = { Text("Todas") })
        }
        items(CellCategory.entries) { cat ->
            FilterChip(
                selected = selected == cat,
                onClick = { onSelect(if (selected == cat) null else cat) },
                label = { Text(cat.displayName) }
            )
        }
    }
}

@Composable
private fun CellList(query: String, category: CellCategory?) {
    val q = query.trim().lowercase()
    val cells = CellCatalog.cellTypes.filter { cell ->
        (category == null || cell.category == category) &&
            (q.isEmpty() || cell.matches(q))
    }
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(cells) { CellCard(it) }
    }
}

@Composable
private fun DescriptorList(query: String, category: CellCategory?) {
    val q = query.trim().lowercase()
    val list = CellCatalog.descriptors.filter { d ->
        (category == null || d.appliesTo == category) &&
            (q.isEmpty() || d.matches(q))
    }
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(list) { DescriptorCard(it) }
    }
}

@Composable
private fun CellCard(cell: CellType) {
    var expanded by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(28.dp)
                        .background(Color(cell.color), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        cell.shortcut,
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.size(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(cell.name, style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold)
                    Text(cell.category.displayName, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            cell.sizeMicrons?.let {
                Spacer(Modifier.size(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistChip(onClick = {}, label = { Text("${it.start.toInt()}–${it.endInclusive.toInt()} µm") })
                    cell.ncRatio?.let { nc -> AssistChip(onClick = {}, label = { Text("N:C $nc") }) }
                }
            }
            AnimatedVisibility(visible = expanded) {
                Column(Modifier.padding(top = 12.dp)) {
                    cell.keyFeatures.forEach { feature ->
                        Row(Modifier.padding(vertical = 2.dp)) {
                            Text("•  ", style = MaterialTheme.typography.bodyMedium)
                            Text(feature, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    cell.nucleus?.let { LabeledLine("Núcleo", it) }
                    cell.cytoplasm?.let { LabeledLine("Citoplasma", it) }
                    cell.clinicalNote?.let { LabeledLine("Relevancia", it) }
                }
            }
            if (!expanded) {
                Text("Toque para ver más", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp))
            }
        }
    }
}

@Composable
private fun DescriptorCard(d: MorphologyDescriptor) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(d.name, style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Box(
                    Modifier
                        .background(
                            MaterialTheme.colorScheme.secondaryContainer,
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(d.axis.displayName, style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer)
                }
            }
            LabeledLine("Definición", d.definition)
            LabeledLine("Significado", d.significance)
            if (d.gradable) {
                Text("Se informa 0 / 1+ / 2+ / 3+", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 6.dp))
            }
        }
    }
}

@Composable
private fun LabeledLine(label: String, value: String) {
    Column(Modifier.padding(top = 8.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

private fun CellType.matches(q: String): Boolean =
    name.lowercase().contains(q) ||
        synonyms.any { it.lowercase().contains(q) } ||
        keyFeatures.any { it.lowercase().contains(q) }

private fun MorphologyDescriptor.matches(q: String): Boolean =
    name.lowercase().contains(q) ||
        synonyms.any { it.lowercase().contains(q) } ||
        definition.lowercase().contains(q) ||
        significance.lowercase().contains(q)
