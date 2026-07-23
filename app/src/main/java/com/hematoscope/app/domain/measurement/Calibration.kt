package com.hematoscope.app.domain.measurement

/**
 * Spatial calibration for a given optical configuration.
 *
 * The core datum is [micronsPerPixel]: how many micrometres one image pixel
 * represents at a particular objective. It is established once per objective
 * with a stage micrometer (a slide with a ruled scale, typically 1 mm divided
 * into 100 parts of 10 µm each):
 *
 *   1. Capture the micrometer scale at the objective.
 *   2. Draw a line spanning a known distance (e.g. 10 divisions = 100 µm).
 *   3. Call [fromReference] with that pixel length and known length.
 *
 * A calibration is only valid for the same objective, camera, sensor
 * resolution and zoom used when it was made.
 */
data class Calibration(
    val objectiveLabel: String,      // e.g. "40x", "100x (aceite)"
    val magnification: Float,        // objective magnification, informational
    val micronsPerPixel: Float,      // the spatial scale
    val referencePixels: Float = 0f, // measured pixel length used to derive it
    val referenceMicrons: Float = 0f,// known real length used to derive it
    val imageWidthPx: Int = 0,       // capture resolution the scale is valid for
    val imageHeightPx: Int = 0,
    val createdAtEpochMs: Long = System.currentTimeMillis()
) {
    val isValid: Boolean get() = micronsPerPixel > 0f && micronsPerPixel.isFinite()

    /** Convert a pixel length to micrometres. */
    fun toMicrons(pixels: Float): Float = pixels * micronsPerPixel

    /** Convert a pixel area to µm². */
    fun toSquareMicrons(pixelsSquared: Float): Float =
        pixelsSquared * micronsPerPixel * micronsPerPixel

    companion object {
        /**
         * Build a calibration from a reference measurement.
         *
         * @param objectiveLabel human label for the objective
         * @param magnification  objective magnification (e.g. 40f)
         * @param measuredPixels pixel length drawn over the known scale
         * @param knownMicrons   real length of that scale in µm
         */
        fun fromReference(
            objectiveLabel: String,
            magnification: Float,
            measuredPixels: Float,
            knownMicrons: Float,
            imageWidthPx: Int = 0,
            imageHeightPx: Int = 0
        ): Calibration {
            require(measuredPixels > 0f) { "La longitud en píxeles debe ser positiva" }
            require(knownMicrons > 0f) { "La longitud conocida debe ser positiva" }
            return Calibration(
                objectiveLabel = objectiveLabel,
                magnification = magnification,
                micronsPerPixel = knownMicrons / measuredPixels,
                referencePixels = measuredPixels,
                referenceMicrons = knownMicrons,
                imageWidthPx = imageWidthPx,
                imageHeightPx = imageHeightPx
            )
        }

        /** Objectives commonly present on a clinical microscope turret. */
        val COMMON_OBJECTIVES = listOf(
            "4x" to 4f,
            "10x" to 10f,
            "40x" to 40f,
            "50x (aceite)" to 50f,
            "100x (aceite)" to 100f
        )
    }
}

/**
 * Reference-free rough scale, used when no stage-micrometer calibration exists
 * but a normal red cell is visible. A mature erythrocyte is a well-known
 * internal size standard (~7.5 µm). Selecting a single red cell's diameter in
 * pixels yields an approximate µm/pixel value.
 */
object InternalSizeStandards {
    /** Mean adult red-cell diameter in micrometres. */
    const val RED_CELL_MICRONS = 7.5f

    /** Small-lymphocyte nucleus, a classic reference for "microcyte" cut-off. */
    const val SMALL_LYMPHOCYTE_NUCLEUS_MICRONS = 7.0f

    fun micronsPerPixelFromRedCell(diameterPixels: Float): Float {
        require(diameterPixels > 0f)
        return RED_CELL_MICRONS / diameterPixels
    }
}
