package com.hematoscope.app.ui.screens.cases

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.hematoscope.app.data.db.CaseEntity
import com.hematoscope.app.data.model.CellCategory
import com.hematoscope.app.data.model.MorphologyGrade
import com.hematoscope.app.domain.catalog.CellCatalog
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CasesScreen(onBack: () -> Unit, vm: CasesViewModel = viewModel()) {
    val cases by vm.cases.collectAsStateWithLifecycle()
    val selectedId by vm.selectedId.collectAsStateWithLifecycle()
    var showCreate by remember { mutableStateOf(false) }

    val selectedCase = cases.firstOrNull { it.id == selectedId }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(selectedCase?.patientCode ?: "Casos") },
                navigationIcon = {
                    IconButton(onClick = { if (selectedCase != null) vm.select(null) else onBack() }) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        },
        floatingActionButton = {
            if (selectedCase == null) {
                FloatingActionButton(onClick = { showCreate = true }) {
                    Icon(Icons.Outlined.Add, contentDescription = "Nuevo caso")
                }
            }
        }
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (selectedCase == null) {
                CaseList(cases, onOpen = { vm.select(it.id) }, onDelete = vm::deleteCase)
            } else {
                CaseDetail(vm)
            }
        }
    }

    if (showCreate) {
        CreateCaseDialog(
            onDismiss = { showCreate = false },
            onConfirm = { code, desc -> vm.createCase(code, desc); showCreate = false }
        )
    }
}

@Composable
private fun CaseList(
    cases: List<CaseEntity>,
    onOpen: (CaseEntity) -> Unit,
    onDelete: (CaseEntity) -> Unit
) {
    if (cases.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Sin casos. Cree uno con el botón +.",
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }
    val fmt = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }
    LazyColumn(
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(cases) { case ->
            Card(Modifier.fillMaxWidth()) {
                Row(
                    Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        Modifier
                            .weight(1f)
                            .padding(end = 8.dp)
                    ) {
                        Text(case.patientCode, style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold)
                        if (case.description.isNotBlank()) {
                            Text(case.description, style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(fmt.format(Date(case.updatedAt)),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    FilledTonalButton(onClick = { onOpen(case) }) { Text("Abrir") }
                    IconButton(onClick = { onDelete(case) }) {
                        Icon(Icons.Outlined.Delete, contentDescription = "Eliminar")
                    }
                }
            }
        }
    }
}

@Composable
private fun CaseDetail(vm: CasesViewModel) {
    val captures by vm.captures.collectAsStateWithLifecycle()
    val observations by vm.observations.collectAsStateWithLifecycle()

    LazyColumn(
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Campos capturados (${captures.size})",
                style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
        item {
            if (captures.isEmpty()) {
                Text("Sin campos. Guárdelos desde la pantalla de Captura.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(captures) { cap ->
                        Column {
                            AsyncImage(
                                model = File(cap.filePath),
                                contentDescription = null,
                                modifier = Modifier.size(120.dp)
                            )
                            Text(cap.objectiveLabel, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
        item {
            Text("Morfología (serie roja)", style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold)
        }
        val gradable = CellCatalog.descriptorsFor(CellCategory.ERYTHROID).filter { it.gradable }
        items(gradable) { d ->
            val current = observations.firstOrNull { it.descriptorId == d.id }?.gradePlus ?: 0
            GradeRow(
                name = d.name,
                grade = current,
                onGrade = { g -> vm.setObservation(d.id, g, present = g > 0) }
            )
        }
        item {
            Text("Hallazgos cualitativos", style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold)
        }
        val qualitative = CellCatalog.descriptors.filter { !it.gradable }
        items(qualitative) { d ->
            val present = observations.any { it.descriptorId == d.id }
            PresenceRow(
                name = d.name,
                present = present,
                onToggle = { vm.setObservation(d.id, if (it) 1 else 0, present = it) }
            )
        }
    }
}

@Composable
private fun GradeRow(name: String, grade: Int, onGrade: (Int) -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Text(name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Spacer(Modifier.size(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                MorphologyGrade.entries.forEach { g ->
                    val selected = grade == g.plusValue
                    if (selected) {
                        FilledTonalButton(onClick = { onGrade(g.plusValue) }) { Text(g.label) }
                    } else {
                        OutlinedButton(onClick = { onGrade(g.plusValue) }) { Text(g.label) }
                    }
                }
            }
        }
    }
}

@Composable
private fun PresenceRow(name: String, present: Boolean, onToggle: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(name, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        androidx.compose.material3.Switch(checked = present, onCheckedChange = onToggle)
    }
}

@Composable
private fun CreateCaseDialog(onDismiss: () -> Unit, onConfirm: (String, String) -> Unit) {
    var code by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nuevo caso") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = code, onValueChange = { code = it },
                    label = { Text("Código / identificador") }, singleLine = true
                )
                OutlinedTextField(
                    value = desc, onValueChange = { desc = it },
                    label = { Text("Descripción (opcional)") }
                )
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(code, desc) }) { Text("Crear") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
