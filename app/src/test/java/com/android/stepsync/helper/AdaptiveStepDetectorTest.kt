package com.android.stepsync.helper

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdaptiveStepDetectorTest {
    @Test
    fun detectsStepsWhenAccelerationCrossesThresholdWithEnoughSpacing() {
        val detector = AdaptiveStepDetector(stepThreshold = 0.5f)
        var timestamp = 0L
        var detectedSteps = 0

        repeat(3) {
            detector.onAcceleration(0f, 0f, 9.8f, timestamp, 0.2f, true)
            timestamp += 300_000_000L
            if (detector.onAcceleration(0f, 0f, 12.4f, timestamp, 0.2f, true)) {
                detectedSteps++
            }
            timestamp += 300_000_000L
        }

        assertEquals(3, detectedSteps)
    }

    @Test
    fun suppressesGyroAssistedStepsWhenRotationIsTooLow() {
        val detector = AdaptiveStepDetector()

        detector.onAcceleration(0f, 0f, 9.8f, 0L, 0.01f, true)

        assertFalse(
            detector.onAcceleration(0f, 0f, 11.5f, 300_000_000L, 0.01f, true)
        )
    }

    @Test
    fun allowsAccelerometerOnlyStepsWithoutGyro() {
        val detector = AdaptiveStepDetector()

        detector.onAcceleration(0f, 0f, 9.8f, 0L, null, false)

        assertTrue(
            detector.onAcceleration(0f, 0f, 11.5f, 300_000_000L, null, false)
        )
    }
}
