package com.hematoscope.app.domain.measurement

import androidx.compose.ui.geometry.Offset
import kotlin.math.abs
import kotlin.math.hypot

/**
 * Pure geometry helpers operating in image-pixel space. Kept free of Android
 * framework types (except [Offset], which is a plain value class) so they can
 * be unit-tested on the JVM.
 */
object Geometry {

    /** Euclidean distance between two points, in pixels. */
    fun distance(a: Offset, b: Offset): Float = hypot(b.x - a.x, b.y - a.y)

    /** Total length of an open polyline, in pixels. */
    fun polylineLength(points: List<Offset>): Float {
        if (points.size < 2) return 0f
        var total = 0f
        for (i in 1 until points.size) total += distance(points[i - 1], points[i])
        return total
    }

    /**
     * Area of a simple polygon via the shoelace formula, in pixels².
     * The polygon is treated as closed (last vertex connects to first).
     */
    fun polygonArea(points: List<Offset>): Float {
        if (points.size < 3) return 0f
        var sum = 0f
        for (i in points.indices) {
            val j = (i + 1) % points.size
            sum += points[i].x * points[j].y - points[j].x * points[i].y
        }
        return abs(sum) / 2f
    }

    /** Perimeter of a closed polygon, in pixels. */
    fun polygonPerimeter(points: List<Offset>): Float {
        if (points.size < 2) return 0f
        var total = 0f
        for (i in points.indices) {
            val j = (i + 1) % points.size
            total += distance(points[i], points[j])
        }
        return total
    }

    /**
     * Equivalent circular diameter of an area (pixels) — the diameter of a
     * circle with the same area. Useful to report cell "size" from a freehand
     * outline. area = π r² ⇒ d = 2·√(area/π).
     */
    fun equivalentDiameter(areaPx: Float): Float =
        2f * kotlin.math.sqrt(areaPx / Math.PI.toFloat())

    /**
     * Angle at vertex [b] formed by segments b→a and b→c, in degrees [0,180].
     */
    fun angleDegrees(a: Offset, b: Offset, c: Offset): Float {
        val v1x = a.x - b.x; val v1y = a.y - b.y
        val v2x = c.x - b.x; val v2y = c.y - b.y
        val dot = v1x * v2x + v1y * v2y
        val mag = hypot(v1x, v1y) * hypot(v2x, v2y)
        if (mag == 0f) return 0f
        val cos = (dot / mag).coerceIn(-1f, 1f)
        return Math.toDegrees(kotlin.math.acos(cos).toDouble()).toFloat()
    }
}
