package com.hematoscope.app.data.model

/**
 * Top-level lineage/category used to group cells in the counter and the atlas.
 */
enum class CellCategory(val displayName: String) {
    GRANULOCYTE("Granulocitos"),
    AGRANULOCYTE("Agranulocitos"),
    PRECURSOR("Precursores / serie inmadura"),
    ERYTHROID("Serie roja (eritrocitos)"),
    PLATELET("Plaquetas"),
    OTHER("Otros / inclusiones")
}

/**
 * Clinical grouping used to build the recommended differential-count keypad.
 * Peripheral-blood WBC differential is normally reported per 100 leukocytes,
 * while nucleated red cells (NRBC) are reported per 100 WBC.
 */
enum class CountingGroup {
    WBC_DIFFERENTIAL,
    NRBC_PER_100_WBC,
    NOT_COUNTED
}

/**
 * A morphological cell type. Immutable reference data; instances live in
 * [com.hematoscope.app.domain.catalog.CellCatalog].
 *
 * @param id            stable machine identifier (used as DB foreign key / JSON key)
 * @param name          Spanish display name
 * @param synonyms      alternative names (English / classic terms) for search
 * @param category      lineage grouping
 * @param countingGroup how this cell participates in the differential
 * @param sizeMicrons   typical diameter range in micrometres (min..max)
 * @param ncRatio       typical nucleus-to-cytoplasm ratio expressed as text
 * @param keyFeatures   short bullet descriptors used in the atlas
 * @param cytoplasm     cytoplasm description
 * @param nucleus       nuclear description
 * @param clinicalNote  when an increase/decrease is clinically relevant
 * @param shortcut      single-letter/keypad label for the counter
 * @param color         ARGB accent used in charts and the counter keypad
 */
data class CellType(
    val id: String,
    val name: String,
    val synonyms: List<String> = emptyList(),
    val category: CellCategory,
    val countingGroup: CountingGroup = CountingGroup.NOT_COUNTED,
    val sizeMicrons: ClosedFloatingPointRange<Float>? = null,
    val ncRatio: String? = null,
    val keyFeatures: List<String> = emptyList(),
    val cytoplasm: String? = null,
    val nucleus: String? = null,
    val clinicalNote: String? = null,
    val shortcut: String,
    val color: Long
)

/**
 * Axis along which a morphological descriptor is scored. Used both in the atlas
 * and by the per-cell observation form.
 */
enum class DescriptorAxis(val displayName: String) {
    SIZE("Tamaño"),
    SHAPE("Forma"),
    COLOR("Color / hemoglobinización"),
    INCLUSION("Inclusiones"),
    DISTRIBUTION("Distribución / disposición"),
    NUCLEUS("Núcleo"),
    CYTOPLASM("Citoplasma")
}

/**
 * A named morphological finding that can be graded. In routine practice red-cell
 * findings are graded semi-quantitatively (0 / 1+ / 2+ / 3+), which the app
 * models with [gradable].
 */
data class MorphologyDescriptor(
    val id: String,
    val name: String,
    val synonyms: List<String> = emptyList(),
    val axis: DescriptorAxis,
    val appliesTo: CellCategory,
    val definition: String,
    val significance: String,
    /** True when the finding is normally reported as 0/1+/2+/3+. */
    val gradable: Boolean = true
)

/** Semi-quantitative grade used for red-cell morphology reporting. */
enum class MorphologyGrade(val label: String, val plusValue: Int) {
    ABSENT("0", 0),
    SLIGHT("1+", 1),
    MODERATE("2+", 2),
    MARKED("3+", 3);

    companion object {
        fun fromPlus(value: Int): MorphologyGrade =
            entries.firstOrNull { it.plusValue == value } ?: ABSENT
    }
}
