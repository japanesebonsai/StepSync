package com.android.stepsync.utils

import java.util.Locale

object TrackingFormatters {
    fun distanceUnitCode(unitPreference: String?): String {
        return when (normalizeUnitPreference(unitPreference)) {
            StepSyncConfig.UNIT_MILES -> StepSyncConfig.DISTANCE_UNIT_MI
            else -> StepSyncConfig.DISTANCE_UNIT_KM
        }
    }

    fun normalizeUnitPreference(unitPreference: String?): String {
        return when (unitPreference) {
            "Metric", "Metric (km)" -> StepSyncConfig.UNIT_KILOMETERS
            "Imperial", "Imperial (mi)" -> StepSyncConfig.UNIT_MILES
            StepSyncConfig.UNIT_MILES -> StepSyncConfig.UNIT_MILES
            else -> StepSyncConfig.UNIT_KILOMETERS
        }
    }

    fun convertDistance(distance: Float, fromUnit: String, toUnit: String): Float {
        return when {
            fromUnit == toUnit -> distance
            fromUnit == StepSyncConfig.UNIT_KILOMETERS && toUnit == StepSyncConfig.UNIT_MILES ->
                distance * StepSyncConfig.KM_TO_MILES
            fromUnit == StepSyncConfig.UNIT_MILES && toUnit == StepSyncConfig.UNIT_KILOMETERS ->
                distance * StepSyncConfig.MILES_TO_KM
            else -> distance
        }
    }

    fun convertDistanceFromKm(distanceKm: Float, distanceUnitCode: String): Float {
        return if (distanceUnitCode == StepSyncConfig.DISTANCE_UNIT_MI) {
            distanceKm * StepSyncConfig.KM_TO_MILES
        } else {
            distanceKm
        }
    }

    fun convertSpeedFromKmh(speedKmh: Float, distanceUnitCode: String): Float {
        return if (distanceUnitCode == StepSyncConfig.DISTANCE_UNIT_MI) {
            speedKmh * StepSyncConfig.KM_TO_MILES
        } else {
            speedKmh
        }
    }

    fun speedUnitLabel(distanceUnitCode: String): String {
        return if (distanceUnitCode == StepSyncConfig.DISTANCE_UNIT_MI) "mph" else "km/h"
    }

    fun formatDistance(distanceKm: Float, distanceUnitCode: String): String {
        return String.format(
            Locale.getDefault(),
            "%.2f %s",
            convertDistanceFromKm(distanceKm, distanceUnitCode),
            distanceUnitCode
        )
    }

    fun formatSpeed(speedKmh: Float, distanceUnitCode: String): String {
        return String.format(
            Locale.getDefault(),
            "%.2f %s",
            convertSpeedFromKmh(speedKmh, distanceUnitCode),
            speedUnitLabel(distanceUnitCode)
        )
    }

    fun formatCompactDuration(timeSeconds: Long): String {
        val hours = timeSeconds / 3600
        val minutes = (timeSeconds % 3600) / 60
        val seconds = timeSeconds % 60

        return when {
            hours > 0 -> String.format(Locale.getDefault(), "%dh %dm", hours, minutes)
            minutes > 0 -> String.format(Locale.getDefault(), "%dm %ds", minutes, seconds)
            else -> String.format(Locale.getDefault(), "%ds", seconds)
        }
    }

    fun formatClockDuration(timeSeconds: Long): String {
        val hours = timeSeconds / 3600
        val minutes = (timeSeconds % 3600) / 60
        val seconds = timeSeconds % 60
        return String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds)
    }

    fun formatHoursAndMinutes(timeSeconds: Long): String {
        val hours = timeSeconds / 3600
        val minutes = (timeSeconds % 3600) / 60
        return String.format(Locale.getDefault(), "%dh %dm", hours, minutes)
    }

    fun formatPace(distanceKm: Float, timeSeconds: Long, unitPreference: String?): String {
        if (distanceKm <= 0f || timeSeconds <= 0) return "-"

        val unitCode = distanceUnitCode(unitPreference)
        val distance = convertDistanceFromKm(distanceKm, unitCode)
        if (distance <= 0f) return "-"

        val minutesPerUnit = (timeSeconds / 60f) / distance
        val minutes = minutesPerUnit.toInt()
        val seconds = ((minutesPerUnit - minutes) * 60).toInt()

        return String.format(Locale.getDefault(), "%d:%02d /%s", minutes, seconds, unitCode)
    }

    fun stepsPerKm(stepLengthCm: Int): Int {
        val safeStepLength = stepLengthCm.coerceAtLeast(1)
        return 100_000 / safeStepLength
    }
}
