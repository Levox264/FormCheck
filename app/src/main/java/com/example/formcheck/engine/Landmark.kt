package com.example.formcheck.engine

/**
 * Framework-free landmark, so the engine runs in plain JVM unit tests.
 *
 * Coordinates are in one consistent unit chosen by the adapter:
 *  - world landmarks: metres, 3D (preferred - independent of camera framing)
 *  - image landmarks: pixels, z = 0
 */
data class Landmark(val x: Float, val y: Float, val z: Float, val visibility: Float)

/** Replaces android.util.Log inside the engine. */
fun interface RepLogger {
    fun log(message: String)

    companion object {
        val None: RepLogger = RepLogger { }
    }
}
