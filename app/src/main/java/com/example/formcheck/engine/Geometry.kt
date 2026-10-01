package com.example.formcheck.engine

import kotlin.math.acos
import kotlin.math.sqrt

object Geometry {
    /**
     * Interior angle (degrees, 0..180) at [b] between segments b->a and b->c.
     * Uses x, y AND z. With world landmarks this is a true 3D angle; with image
     * landmarks (z = 0) the caller must already have scaled x/y to pixels,
     * otherwise portrait frames distort every angle.
     *
     * Returns null for degenerate input (coincident points).
     */
    fun interiorAngleDeg(a: Landmark, b: Landmark, c: Landmark): Float? {
        val bax = a.x - b.x; val bay = a.y - b.y; val baz = a.z - b.z
        val bcx = c.x - b.x; val bcy = c.y - b.y; val bcz = c.z - b.z

        val dot = bax * bcx + bay * bcy + baz * bcz
        val norms = sqrt(bax * bax + bay * bay + baz * baz) *
            sqrt(bcx * bcx + bcy * bcy + bcz * bcz)
        if (norms < 1e-9f) return null

        val cosine = (dot / norms).coerceIn(-1f, 1f)
        return Math.toDegrees(acos(cosine.toDouble())).toFloat()
    }
}
