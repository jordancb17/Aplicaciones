package com.hematoscope.app.domain.measurement

import android.graphics.Bitmap

/**
 * Result of auto-segmenting a single cell into nucleus and cytoplasm.
 *
 * [overlay] is a small ARGB bitmap covering only the analysed region; the caller
 * draws it at image position ([overlayLeft], [overlayTop]).
 */
data class SegmentationResult(
    val nucleusPixels: Int,
    val cytoplasmPixels: Int,
    val cellPixels: Int,
    val ncRatio: Float,
    val nucleusFraction: Float,
    val overlay: Bitmap,
    val overlayLeft: Int,
    val overlayTop: Int,
    /** Mean orange-vs-blue bias of the cytoplasm, 0..1 (high in eosinophils). */
    val cytoOrangeness: Float = 0f,
    /** Fraction of cytoplasm that is dark blue-purple, 0..1 (basophil granules). */
    val cytoDarkPurpleFraction: Float = 0f,
    /** Cytoplasm luminance texture, 0..1 (granularity vs smoothness). */
    val cytoGranularity: Float = 0f
)

/**
 * Colour-threshold nucleus/cytoplasm segmentation for a Wright/Giemsa-stained
 * cell, to estimate the nucleus-to-cytoplasm ratio automatically.
 *
 * Given a tap point and a radius, within that circular ROI it:
 *  1. drops near-white slide background (bright + low saturation);
 *  2. scores the remaining "cell" pixels by darkness and blue-violet content —
 *     nuclei stain dark purple/blue, cytoplasm is paler;
 *  3. splits nucleus vs cytoplasm with an Otsu threshold on that score.
 *
 * This is decision support, not a validated segmentation; the manual N:C tool
 * remains available for exact tracing.
 */
object NucleusSegmenter {

    private val NUCLEUS_ARGB = 0x808E24AA.toInt() // semi-transparent violet
    private val CYTOPLASM_ARGB = 0x6000BCD4.toInt() // semi-transparent cyan

    fun segment(source: Bitmap, centerX: Int, centerY: Int, radius: Int): SegmentationResult? {
        val w = source.width
        val h = source.height
        val r = radius.coerceIn(8, maxOf(w, h))
        val x0 = (centerX - r).coerceIn(0, w - 1)
        val y0 = (centerY - r).coerceIn(0, h - 1)
        val x1 = (centerX + r).coerceIn(0, w - 1)
        val y1 = (centerY + r).coerceIn(0, h - 1)
        val bw = x1 - x0 + 1
        val bh = y1 - y0 + 1
        if (bw <= 2 || bh <= 2) return null

        val px = IntArray(bw * bh)
        source.getPixels(px, 0, bw, x0, y0, bw, bh)

        val isCell = BooleanArray(bw * bh)
        val score = FloatArray(bw * bh)
        val r2 = r * r
        var cellCount = 0

        for (j in 0 until bh) {
            for (i in 0 until bw) {
                val idx = j * bw + i
                val gx = x0 + i - centerX
                val gy = y0 + j - centerY
                if (gx * gx + gy * gy > r2) continue

                val c = px[idx]
                val red = (c shr 16) and 0xFF
                val green = (c shr 8) and 0xFF
                val blue = c and 0xFF
                val lum = 0.299f * red + 0.587f * green + 0.114f * blue
                val maxc = maxOf(red, green, blue)
                val minc = minOf(red, green, blue)
                val sat = if (maxc == 0) 0f else (maxc - minc).toFloat() / maxc

                val isBackground = lum > 200f && sat < 0.15f
                if (!isBackground) {
                    isCell[idx] = true
                    cellCount++
                    val darkness = (255f - lum) / 255f
                    val blueViolet =
                        (((blue - green) + (red - green)) / 2f / 128f).coerceIn(0f, 1f)
                    score[idx] = 0.55f * darkness + 0.45f * blueViolet
                }
            }
        }
        if (cellCount < 20) return null // tap likely on background

        val hist = IntArray(256)
        for (idx in isCell.indices) {
            if (isCell[idx]) hist[(score[idx] * 255f).toInt().coerceIn(0, 255)]++
        }
        val threshold = otsu(hist, cellCount)

        var nucleus = 0
        var cytoCount = 0
        var sumOrange = 0L
        var darkPurple = 0
        var sumLum = 0.0
        var sumLumSq = 0.0
        val overlayPixels = IntArray(bw * bh) // 0 == transparent
        for (idx in isCell.indices) {
            if (!isCell[idx]) continue
            val bin = (score[idx] * 255f).toInt().coerceIn(0, 255)
            if (bin >= threshold) {
                nucleus++
                overlayPixels[idx] = NUCLEUS_ARGB
            } else {
                overlayPixels[idx] = CYTOPLASM_ARGB
                val c = px[idx]
                val red = (c shr 16) and 0xFF
                val green = (c shr 8) and 0xFF
                val blue = c and 0xFF
                val lum = 0.299 * red + 0.587 * green + 0.114 * blue
                cytoCount++
                sumOrange += (red - blue).toLong()
                if (lum < 120.0 && blue > green && red > green) darkPurple++
                sumLum += lum
                sumLumSq += lum * lum
            }
        }

        val overlay = Bitmap.createBitmap(bw, bh, Bitmap.Config.ARGB_8888)
        overlay.setPixels(overlayPixels, 0, bw, 0, 0, bw, bh)

        val cytoplasm = cellCount - nucleus
        val ratio = if (cytoplasm > 0) nucleus.toFloat() / cytoplasm else Float.POSITIVE_INFINITY
        val fraction = if (cellCount > 0) nucleus.toFloat() / cellCount else 0f

        var orangeness = 0f
        var darkPurpleFraction = 0f
        var granularity = 0f
        if (cytoCount > 0) {
            orangeness = ((sumOrange.toDouble() / cytoCount) / 128.0).coerceIn(0.0, 1.0).toFloat()
            darkPurpleFraction = (darkPurple.toDouble() / cytoCount).toFloat()
            val meanLum = sumLum / cytoCount
            val variance = (sumLumSq / cytoCount - meanLum * meanLum).coerceAtLeast(0.0)
            granularity = (kotlin.math.sqrt(variance) / 60.0).coerceIn(0.0, 1.0).toFloat()
        }

        return SegmentationResult(
            nucleusPixels = nucleus,
            cytoplasmPixels = cytoplasm,
            cellPixels = cellCount,
            ncRatio = ratio,
            nucleusFraction = fraction,
            overlay = overlay,
            overlayLeft = x0,
            overlayTop = y0,
            cytoOrangeness = orangeness,
            cytoDarkPurpleFraction = darkPurpleFraction,
            cytoGranularity = granularity
        )
    }

    /** Otsu's between-class-variance threshold over a 256-bin histogram. */
    private fun otsu(hist: IntArray, total: Int): Int {
        var sum = 0.0
        for (t in 0..255) sum += (t * hist[t]).toDouble()
        var sumB = 0.0
        var weightB = 0
        var maxVar = -1.0
        var threshold = 128
        for (t in 0..255) {
            weightB += hist[t]
            if (weightB == 0) continue
            val weightF = total - weightB
            if (weightF == 0) break
            sumB += (t * hist[t]).toDouble()
            val meanB = sumB / weightB
            val meanF = (sum - sumB) / weightF
            val between = weightB.toDouble() * weightF * (meanB - meanF) * (meanB - meanF)
            if (between > maxVar) {
                maxVar = between
                threshold = t
            }
        }
        return threshold
    }
}
