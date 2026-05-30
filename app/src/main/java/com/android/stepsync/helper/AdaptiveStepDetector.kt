package com.android.stepsync.helper

import kotlin.math.sqrt

class AdaptiveStepDetector(
    private val minStepIntervalNs: Long = MIN_STEP_INTERVAL_NS,
    private val highPassAlpha: Float = HIGH_PASS_ALPHA,
    private val stepThreshold: Float = STEP_THRESHOLD
) {
    private var gravityMagnitude = 0f
    private var previousLinearMagnitude = 0f
    private var lastStepTimestampNs = 0L

    fun reset() {
        gravityMagnitude = 0f
        previousLinearMagnitude = 0f
        lastStepTimestampNs = 0L
    }

    fun onAcceleration(
        x: Float,
        y: Float,
        z: Float,
        timestampNs: Long,
        gyroMagnitude: Float?,
        allowGyroAssist: Boolean
    ): Boolean {
        val magnitude = sqrt((x * x + y * y + z * z).toDouble()).toFloat()
        gravityMagnitude = if (gravityMagnitude == 0f) {
            magnitude
        } else {
            highPassAlpha * gravityMagnitude + (1f - highPassAlpha) * magnitude
        }

        val linearMagnitude = magnitude - gravityMagnitude
        val crossedThreshold = previousLinearMagnitude < stepThreshold && linearMagnitude >= stepThreshold
        previousLinearMagnitude = linearMagnitude

        if (!crossedThreshold) return false
        if (timestampNs - lastStepTimestampNs < minStepIntervalNs) return false

        val hasEnoughRotation = gyroMagnitude == null || gyroMagnitude >= GYRO_ASSIST_THRESHOLD
        if (allowGyroAssist && !hasEnoughRotation) return false

        lastStepTimestampNs = timestampNs
        return true
    }

    companion object {
        private const val MIN_STEP_INTERVAL_NS = 250_000_000L
        private const val HIGH_PASS_ALPHA = 0.8f
        private const val STEP_THRESHOLD = 1.15f
        private const val GYRO_ASSIST_THRESHOLD = 0.08f
    }
}
