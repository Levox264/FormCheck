package com.example.formcheck

import com.example.formcheck.engine.Landmark
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import com.google.mediapipe.tasks.components.containers.Landmark as MpWorldLandmark

/** The only place the engine touches MediaPipe types. */
object MediaPipeAdapter {

    /**
     * Preferred: metric 3D coordinates (metres), so angles do not depend on camera
     * framing or aspect ratio. Visibility still comes from the normalized landmarks.
     */
    fun fromWorld(
        normalized: List<NormalizedLandmark>,
        world: List<MpWorldLandmark>,
    ): List<Landmark> = normalized.indices.map { i ->
        val w = world[i]
        Landmark(w.x(), w.y(), w.z(), normalized[i].visibility().orElse(0f))
    }

    /**
     * Fallback: image-space pixels with z = 0. Scaling x by width and y by height is
     * what removes the portrait-frame distortion. Pass width = height = 1 to reproduce
     * the OLD unscaled behaviour exactly (useful for A/B comparison).
     */
    fun fromImage2D(
        normalized: List<NormalizedLandmark>,
        imageWidth: Int,
        imageHeight: Int,
    ): List<Landmark> = normalized.map {
        Landmark(it.x() * imageWidth, it.y() * imageHeight, 0f, it.visibility().orElse(0f))
    }
}
