package com.hematoscope.app.ui.screens.cases

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hematoscope.app.HematoScopeApp
import com.hematoscope.app.data.db.CaptureEntity
import com.hematoscope.app.data.db.CaseEntity
import com.hematoscope.app.data.db.ObservationEntity
import com.hematoscope.app.domain.report.CaseReportGenerator
import java.io.File
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow

class CasesViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = (app as HematoScopeApp).repository

    val cases: StateFlow<List<CaseEntity>> =
        repository.observeCases().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val selectedCaseId = MutableStateFlow<Long?>(null)
    val selectedId: StateFlow<Long?> = selectedCaseId

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val captures: StateFlow<List<CaptureEntity>> =
        selectedCaseId.flatMapLatest { id ->
            if (id == null) flowOf(emptyList()) else repository.observeCaptures(id)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val observations: StateFlow<List<ObservationEntity>> =
        selectedCaseId.flatMapLatest { id ->
            if (id == null) flowOf(emptyList()) else repository.observeObservations(id)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun select(id: Long?) { selectedCaseId.value = id }

    fun createCase(patientCode: String, description: String) {
        if (patientCode.isBlank()) return
        viewModelScope.launch { repository.createCase(patientCode.trim(), description.trim()) }
    }

    fun deleteCase(entity: CaseEntity) {
        viewModelScope.launch {
            if (selectedCaseId.value == entity.id) selectedCaseId.value = null
            repository.deleteCase(entity)
        }
    }

    fun setObservation(descriptorId: String, gradePlus: Int, present: Boolean) {
        val id = selectedCaseId.value ?: return
        viewModelScope.launch {
            repository.setObservation(id, descriptorId, gradePlus, present, note = "")
        }
    }

    /** Build and render a PDF report for the selected case, then hand back the file. */
    fun exportReport(onReady: (File) -> Unit, onError: (String) -> Unit) {
        val id = selectedCaseId.value ?: return onError("No hay caso seleccionado")
        viewModelScope.launch {
            val data = repository.buildCaseReport(id)
                ?: return@launch onError("No se pudo cargar el caso")
            val file = runCatching {
                CaseReportGenerator.generate(getApplication(), data)
            }.getOrElse { return@launch onError("Error al generar el PDF: ${it.message}") }
            onReady(file)
        }
    }
}
