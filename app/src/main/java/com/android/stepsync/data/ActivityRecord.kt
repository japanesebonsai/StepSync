// ActivityRecord.kt
package com.android.stepsync.data

data class ActivityRecord(
    val id: String = "",
    val userId: String = "",
    val timestamp: Long = 0,
    val durationSeconds: Long = 0,
    val distanceKm: Float = 0f,
    val avgSpeedKmh: Float = 0f
) {
    // Required empty constructor for Firebase
    constructor() : this("", "", 0, 0, 0f, 0f)
}