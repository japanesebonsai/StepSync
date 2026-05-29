package com.android.stepsync.utils

object StepSyncConfig {
    const val DATABASE_URL =
        "https://stepsync-d21c1-default-rtdb.asia-southeast1.firebasedatabase.app/"

    const val PREFS_NAME = "step_sync_prefs"
    const val LOGIN_PREFS_NAME = "login_prefs"

    const val ACTION_UNITS_CHANGED = "com.android.stepsync.UNITS_CHANGED"
    const val EXTRA_UNIT_TYPE = "unit_type"

    const val KEY_STEP_LENGTH = "step_length"
    const val KEY_DAILY_STEP_GOAL = "daily_step_goal"
    const val KEY_UNITS = "units"
    const val KEY_THEME = "theme"
    const val KEY_NOTIFICATIONS = "notifications"
    const val KEY_DATA_SYNC = "data_sync"
    const val KEY_CURRENT_DISTANCE = "current_distance"
    const val KEY_CURRENT_SPEED = "current_speed"
    const val KEY_CURRENT_TIME_SECONDS = "current_time_seconds"
    const val KEY_DAILY_TOTAL_STEPS = "daily_total_steps"
    const val KEY_CURRENT_ACTIVITY_STEPS = "current_activity_steps"
    const val KEY_LAST_DAILY_STEPS_RESET = "last_daily_steps_reset"

    const val DEFAULT_STEP_LENGTH_CM = 65
    const val DEFAULT_DAILY_STEP_GOAL = 10_000
    const val DEFAULT_UNITS = "Kilometers (km)"
    const val DEFAULT_THEME = "Light"
    const val DEFAULT_NOTIFICATIONS = true
    const val DEFAULT_DATA_SYNC = true

    const val UNIT_KILOMETERS = "Kilometers (km)"
    const val UNIT_MILES = "Miles (mi)"
    const val DISTANCE_UNIT_KM = "km"
    const val DISTANCE_UNIT_MI = "mi"
    const val KM_TO_MILES = 0.621371f
    const val MILES_TO_KM = 1.60934f

    fun userScopedKey(userId: String, key: String): String = "${userId}_$key"
}
