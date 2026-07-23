package com.hematoscope.app.ui.screens.differential

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hematoscope.app.HematoScopeApp
import com.hematoscope.app.data.db.CaseEntity
import com.hematoscope.app.data.model.CellType
import com.hematoscope.app.data.model.CountingGroup
import com.hematoscope.app.domain.catalog.CellCatalog
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** A cell key with its live count and running percentage. */
data class TallyRow(
    val cell: CellType,
    val count: Int,
    val percent: Float
)

/**
 * In-memory manual differential counter. Leukocytes are tallied toward
 * [targetTotal] (100 or 200 by convention); nucleated red cells are counted
 * separately and reported per 100 WBC, matching laboratory practice.
 */
class DifferentialViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = (app as HematoScopeApp).repository

    /** Cases available as save targets for the current count. */
    val cases: StateFlow<List<CaseEntity>> =
        repository.observeCases()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    var saveMessage by mutableStateOf<String?>(null)
        private set

    private val counts = mutableStateMapOf<String, Int>()
    private val history = ArrayDeque<String>()  // cellIds, for undo

    var targetTotal by mutableIntStateOf(100)
        private set

    private var nrbc by mutableIntStateOf(0)

    val wbcKeys: List<CellType> =
        CellCatalog.countableCells.filter { it.countingGroup == CountingGroup.WBC_DIFFERENTIAL }

    val nrbcKey: CellType? =
        CellCatalog.countableCells.firstOrNull { it.countingGroup == CountingGroup.NRBC_PER_100_WBC }

    /** Number of leukocytes tallied so far (denominator for the differential). */
    val wbcTotal: Int
        get() = wbcKeys.sumOf { counts[it.id] ?: 0 }

    val nrbcCount: Int get() = nrbc

    /** NRBC per 100 WBC — the reported correction figure. */
    val nrbcPer100Wbc: Float
        get() = if (wbcTotal > 0) nrbc * 100f / wbcTotal else 0f

    val isComplete: Boolean get() = wbcTotal >= targetTotal

    fun setTarget(value: Int) { targetTotal = value }

    fun increment(cellId: String) {
        counts[cellId] = (counts[cellId] ?: 0) + 1
        history.addLast(cellId)
    }

    fun incrementNrbc() {
        nrbc += 1
        history.addLast(NRBC_TOKEN)
    }

    fun undo() {
        val last = history.removeLastOrNull() ?: return
        if (last == NRBC_TOKEN) {
            nrbc = (nrbc - 1).coerceAtLeast(0)
        } else {
            val next = ((counts[last] ?: 0) - 1).coerceAtLeast(0)
            if (next <= 0) counts.remove(last) else counts[last] = next
        }
    }

    fun reset() {
        counts.clear()
        history.clear()
        nrbc = 0
    }

    fun rows(): List<TallyRow> {
        val total = wbcTotal
        return wbcKeys.map { cell ->
            val c = counts[cell.id] ?: 0
            TallyRow(cell, c, if (total > 0) c * 100f / total else 0f)
        }
    }

    /** Snapshot suitable for persisting via the repository. */
    fun tallySnapshot(): Map<String, Int> = wbcKeys.associate { it.id to (counts[it.id] ?: 0) }

    /** Persist the current count as a new differential attached to [caseId]. */
    fun saveToCase(caseId: Long) {
        viewModelScope.launch {
            val diffId = repository.createDifferential(caseId, "Recuento diferencial", targetTotal)
            repository.saveDifferentialTallies(diffId, tallySnapshot(), nrbcCount)
            saveMessage = "Recuento guardado en el caso"
        }
    }

    fun clearMessage() { saveMessage = null }

    companion object { private const val NRBC_TOKEN = "__nrbc__" }
}
