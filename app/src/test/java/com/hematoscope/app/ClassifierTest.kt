package com.hematoscope.app

import com.hematoscope.app.domain.catalog.CellClassifier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ClassifierTest {

    @Test
    fun smallHighNc_favoursLymphocyte() {
        // ~9 µm, N:C ~4 → small lymphocyte territory.
        val suggestions = CellClassifier.suggest(diameterMicrons = 9f, ncRatio = 4f)
        assertTrue(suggestions.isNotEmpty())
        assertEquals("lymphocyte", suggestions.first().cell.id)
    }

    @Test
    fun largeLowNc_favoursGranulocyteOrMonocyte() {
        // ~14 µm, N:C ~0.4 → abundant cytoplasm (neutrophil / monocyte range).
        val top = CellClassifier.suggest(diameterMicrons = 14f, ncRatio = 0.4f).first()
        assertTrue(top.cell.id in setOf("neutrophil_segmented", "neutrophil_band", "monocyte", "eosinophil"))
    }

    @Test
    fun ncOnly_stillRanks_whenUncalibrated() {
        // No diameter (uncalibrated): ranking must still work from N:C alone.
        val suggestions = CellClassifier.suggest(diameterMicrons = null, ncRatio = 5f)
        assertTrue(suggestions.isNotEmpty())
        // Very high N:C should put a blast or lymphocyte on top.
        assertTrue(suggestions.first().cell.id in setOf("blast", "lymphocyte"))
    }

    @Test
    fun invalidNc_returnsNoSuggestions() {
        assertTrue(CellClassifier.suggest(null, 0f).isEmpty())
        assertTrue(CellClassifier.suggest(null, Float.POSITIVE_INFINITY).isEmpty())
    }
}
