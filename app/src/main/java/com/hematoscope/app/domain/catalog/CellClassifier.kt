package com.hematoscope.app.domain.catalog

import com.hematoscope.app.data.model.CellType
import com.hematoscope.app.data.model.GranuleType

/** A ranked cell-type suggestion with a 0..1 match score. */
data class CellSuggestion(val cell: CellType, val score: Float)

/**
 * Cytoplasm colour/texture features from segmentation, used to separate
 * granulocytes by granule colour. All fields are 0..1.
 */
data class GranuleFeatures(
    val orangeness: Float,
    val darkPurpleFraction: Float,
    val granularity: Float
)

/**
 * Morphometric (rule-based) cell-type suggester.
 *
 * Given a measured nucleus-to-cytoplasm ratio (from [com.hematoscope.app.domain
 * .measurement.NucleusSegmenter]) and, when a calibration exists, a cell diameter
 * in micrometres, it ranks candidate cell types by how well those two features
 * match each type's reference ranges in [CellCatalog].
 *
 * This is deterministic decision support — not a trained classifier and not a
 * diagnosis. It only considers size and N:C, so it cannot distinguish types that
 * overlap on both (e.g. it will not tell an eosinophil from a neutrophil, which
 * differ by granulation). Always confirm against the atlas and the smear.
 */
object CellClassifier {

    /** Candidates are the cell types that carry a numeric N:C reference. */
    val candidates: List<CellType>
        get() = CellCatalog.cellTypes.filter { it.ncRatioRange != null }

    fun suggest(
        diameterMicrons: Float?,
        ncRatio: Float,
        granules: GranuleFeatures? = null,
        limit: Int = 4
    ): List<CellSuggestion> {
        if (!ncRatio.isFinite() || ncRatio <= 0f) return emptyList()
        return candidates
            .mapNotNull { cell ->
                var total = 0f
                var weight = 0f

                cell.ncRatioRange?.let { range ->
                    total += rangeScore(ncRatio, range.start, range.endInclusive) * NC_WEIGHT
                    weight += NC_WEIGHT
                }
                val size = cell.sizeMicrons
                if (diameterMicrons != null && size != null) {
                    total += rangeScore(diameterMicrons, size.start, size.endInclusive) * SIZE_WEIGHT
                    weight += SIZE_WEIGHT
                }
                if (granules != null) {
                    total += granuleScore(cell.granuleType, granules) * GRAN_WEIGHT
                    weight += GRAN_WEIGHT
                }

                if (weight == 0f) null else CellSuggestion(cell, total / weight)
            }
            .sortedByDescending { it.score }
            .take(limit)
    }

    /** How well the observed cytoplasm colour matches a cell's granule type. */
    private fun granuleScore(type: GranuleType, f: GranuleFeatures): Float {
        val dark = (f.darkPurpleFraction * 2f).coerceIn(0f, 1f)
        return when (type) {
            GranuleType.EOSINOPHILIC -> f.orangeness
            GranuleType.BASOPHILIC -> dark
            GranuleType.NEUTRAL -> ((1f - f.orangeness) * (1f - dark)).coerceIn(0f, 1f)
            GranuleType.NONE ->
                ((1f - f.orangeness) * (1f - dark) * (1f - 0.5f * f.granularity)).coerceIn(0f, 1f)
        }
    }

    /**
     * 1.0 when [value] lies within [lo, hi]; smoothly decaying outside, reaching
     * ~0.5 one half-width beyond an edge.
     */
    private fun rangeScore(value: Float, lo: Float, hi: Float): Float {
        if (value in lo..hi) return 1f
        val halfWidth = ((hi - lo) / 2f).coerceAtLeast(1e-4f)
        val dist = if (value < lo) lo - value else value - hi
        return (1f / (1f + dist / halfWidth)).coerceIn(0f, 1f)
    }

    private const val NC_WEIGHT = 1.0f
    private const val SIZE_WEIGHT = 1.0f
    private const val GRAN_WEIGHT = 0.9f
}
