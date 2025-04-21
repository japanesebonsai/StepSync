package com.android.stepsync.data

data class ActivityRecord(
    val duration: Long,
    val steps: Int,
    val distance: Float,
    val avgSpeed: Float
)