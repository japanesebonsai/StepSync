package com.android.stepsync.fragment

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.android.stepsync.R
import com.android.stepsync.activity.SettingsActivity
import com.android.stepsync.app.MyApplication
import com.android.stepsync.helper.StepTrackingService
import com.android.stepsync.data.ActivityRecord
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale

class HomeFragment : Fragment(R.layout.fragment_home) {
    private val TAG = "HomeFragment"
    private val DEFAULT_STEP_GOAL = 10000 // Default daily goal of 10,000 steps
    
    private lateinit var distanceTextView: TextView
    private lateinit var timeTextView: TextView
    private lateinit var speedTextView: TextView
    private lateinit var statusTextView: TextView
    
    private lateinit var weeklyActivitiesTextView: TextView
    private lateinit var weeklyTimeTextView: TextView
    private lateinit var weeklyDistanceTextView: TextView
    private lateinit var weeklyPaceTextView: TextView
    
    private lateinit var stepsCountTextView: TextView
    private lateinit var goalPercentageTextView: TextView
    private lateinit var progressSteps: CircularProgressIndicator
    
    private lateinit var sharedPreferences: SharedPreferences
    private var dailyStepGoal = DEFAULT_STEP_GOAL
    private var currentStepCount = 0
    private var dailyTotalSteps = 0
    private var distanceUnit = "km"
    private var currentUserId: String = ""
    
    private val unitsChangedReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == "com.android.stepsync.UNITS_CHANGED") {
                val unitType = intent.getStringExtra("unit_type") ?: "Kilometers (km)"
                updateDisplayUnits(unitType)
                
                // Update the displayed values with new units
                val isTracking = sharedPreferences.getBoolean(StepTrackingService.PREF_IS_TRACKING, false)
                val distance = sharedPreferences.getFloat("current_distance", 0f)
                val speed = sharedPreferences.getFloat("current_speed", 0f)
                
                updateDistanceDisplay(distance)
                updateSpeedDisplay(speed)
                updateStatusDisplay(isTracking)
                
                // Reload weekly stats to apply new units
                loadWeeklyStats()
            }
        }
    }
    
    private val trackingUpdateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                StepTrackingService.ACTION_TIME_UPDATE -> {
                    val timeInSeconds = intent.getLongExtra(StepTrackingService.EXTRA_TIME, 0)
                    updateTimeDisplay(timeInSeconds)
                }
                StepTrackingService.ACTION_DISTANCE_UPDATE -> {
                    val distance = intent.getFloatExtra(StepTrackingService.EXTRA_DISTANCE, 0f)
                    updateDistanceDisplay(distance)
                    
                    // Estimate steps using the user's step length setting
                    val stepLengthCm = sharedPreferences.getInt("step_length", 65) // Default 65cm
                    val stepsPerKm = (100000 / stepLengthCm) // 100,000 cm per km / step length in cm
                    val estimatedSteps = (distance * stepsPerKm).toInt()
                    updateStepProgress(estimatedSteps)
                }
                StepTrackingService.ACTION_SPEED_UPDATE -> {
                    val speed = intent.getFloatExtra(StepTrackingService.EXTRA_SPEED, 0f)
                    updateSpeedDisplay(speed)
                }
                StepTrackingService.ACTION_TRACKING_STATUS -> {
                    val isTracking = intent.getBooleanExtra(StepTrackingService.EXTRA_IS_TRACKING, false)
                    updateStatusDisplay(isTracking)
                }
                ACTION_ACTIVITY_COMPLETED -> {
                    // If an activity was completed, get the steps from it
                    val activitySteps = intent.getIntExtra("activity_steps", 0)
                    if (activitySteps > 0) {
                        android.util.Log.d(TAG, "Activity completed with $activitySteps steps")

                        dailyTotalSteps += activitySteps
                        sharedPreferences.edit()
                            .putInt("${currentUserId}_daily_total_steps", dailyTotalSteps)
                            .putInt("${currentUserId}_current_activity_steps", 0) // Reset current activity
                            .apply()

                        // Update the UI directly
                        val formatter = NumberFormat.getNumberInstance(Locale.US)
                        stepsCountTextView.text = formatter.format(dailyTotalSteps)
                        
                        val percentage = if (dailyStepGoal > 0) (dailyTotalSteps * 100 / dailyStepGoal) else 0
                        goalPercentageTextView.text = "$percentage% of ${formatter.format(dailyStepGoal)} steps"
                        
                        progressSteps.progress = dailyTotalSteps.coerceAtMost(dailyStepGoal)
                    }
                    
                    loadWeeklyStats()
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        android.util.Log.d(TAG, "onViewCreated called")

        sharedPreferences = requireActivity().getSharedPreferences("step_sync_prefs", Context.MODE_PRIVATE)
        
        // Get current user ID
        val app = activity?.application as? MyApplication
        currentUserId = app?.firebaseAuth?.currentUser?.uid ?: ""
        
        // Load user-specific daily step goal
        dailyStepGoal = sharedPreferences.getInt("${currentUserId}_daily_step_goal", DEFAULT_STEP_GOAL)
        
        // Load the daily total steps and check if we need to reset for a new day
        checkAndResetDailySteps()

        // Get preferred units
        val unitPreference = sharedPreferences.getString("units", "Kilometers (km)")
        updateDisplayUnits(unitPreference ?: "Kilometers (km)")

        distanceTextView = view.findViewById(R.id.text_home_distance)
        timeTextView = view.findViewById(R.id.text_home_time)
        speedTextView = view.findViewById(R.id.text_home_speed)
        statusTextView = view.findViewById(R.id.text_home_status)

        weeklyActivitiesTextView = view.findViewById(R.id.text_activities)
        weeklyTimeTextView = view.findViewById(R.id.text_time)
        weeklyDistanceTextView = view.findViewById(R.id.text_distance)
        
        // Try to find weekly pace TextView if it exists in the layout
        try {
            weeklyPaceTextView = view.findViewById(R.id.text_pace)
        } catch (e: Exception) {
            android.util.Log.d(TAG, "Weekly pace TextView not found in layout")
        }
        
        stepsCountTextView = view.findViewById(R.id.text_step_count)
        goalPercentageTextView = view.findViewById(R.id.text_goal_percentage)
        progressSteps = view.findViewById(R.id.progress_steps)
        
        progressSteps.max = dailyStepGoal
        
        val buttonSettings = view.findViewById<Button>(R.id.button_settings)
        val buttonSetGoal = view.findViewById<Button>(R.id.button_set_goal)

        buttonSettings.setOnClickListener {
            val intent = Intent(requireContext(), SettingsActivity::class.java)
            startActivity(intent)
        }
        
        buttonSetGoal.setOnClickListener {
            showStepGoalDialog()
        }
        
        stepsCountTextView.setOnLongClickListener {
            showStepGoalDialog()
            true
        }
        
        updateStepProgress(currentStepCount)
        
        requestTrackingStatus()
        
        loadWeeklyStats()
    }
    
    override fun onResume() {
        super.onResume()
        
        // Check if we need to reset the daily counter when resuming the app
        checkAndResetDailySteps()
        
        val intentFilter = IntentFilter().apply {
            addAction(StepTrackingService.ACTION_TIME_UPDATE)
            addAction(StepTrackingService.ACTION_DISTANCE_UPDATE)
            addAction(StepTrackingService.ACTION_SPEED_UPDATE)
            addAction(StepTrackingService.ACTION_TRACKING_STATUS)
            addAction(ACTION_ACTIVITY_COMPLETED)
            addAction("com.android.stepsync.UNITS_CHANGED")
        }
        
        LocalBroadcastManager.getInstance(requireContext())
            .registerReceiver(trackingUpdateReceiver, intentFilter)
            
        ContextCompat.registerReceiver(
            requireContext(),
            unitsChangedReceiver,
            IntentFilter("com.android.stepsync.UNITS_CHANGED"),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
            
        requestTrackingStatus()
        loadWeeklyStats()
    }
    
    override fun onPause() {
        super.onPause()
        
        LocalBroadcastManager.getInstance(requireContext())
            .unregisterReceiver(trackingUpdateReceiver)
            
        try {
            requireActivity().unregisterReceiver(unitsChangedReceiver)
        } catch (e: Exception) {
            // Receiver might not be registered
        }
    }
    
    private fun requestTrackingStatus() {
        if (!sharedPreferences.getBoolean(StepTrackingService.PREF_IS_TRACKING, false)) {
            updateStatusDisplay(false)
            return
        }

        val serviceIntent = Intent(requireContext(), StepTrackingService::class.java).apply {
            action = StepTrackingService.ACTION_REQUEST_STATUS
        }
        requireContext().startService(serviceIntent)
    }
    
    private fun updateTimeDisplay(timeInSeconds: Long) {
        val hours = timeInSeconds / 3600
        val minutes = (timeInSeconds % 3600) / 60
        val seconds = timeInSeconds % 60

        // Format time to include seconds, especially for short durations
        timeTextView.text = when {
            hours > 0 -> String.format(Locale.getDefault(), "%dh %dm", hours, minutes)
            minutes > 0 -> String.format(Locale.getDefault(), "%dm %ds", minutes, seconds)
            else -> String.format(Locale.getDefault(), "%ds", seconds)
        }
        
        // Save current time for pace calculations
        sharedPreferences.edit().putLong("current_time_seconds", timeInSeconds).apply()
    }
    
    private fun updateDistanceDisplay(distanceInKm: Float) {
        // Convert based on selected unit
        val converted = when {
            distanceUnit == "mi" -> distanceInKm * 0.621371f // km to miles
            else -> distanceInKm // Already in km
        }
        
        distanceTextView.text = String.format(Locale.getDefault(), "%.2f %s", converted, distanceUnit)
        
        // Save current distance for potential unit conversion updates
        sharedPreferences.edit().putFloat("current_distance", distanceInKm).apply()
    }
    
    private fun updateSpeedDisplay(speedInKmh: Float) {
        // Convert based on selected unit
        val converted = when {
            distanceUnit == "mi" -> speedInKmh * 0.621371f // km/h to mph
            else -> speedInKmh // Already in km/h
        }
        
        val unitText = if (distanceUnit == "mi") "mph" else "km/h"
        
        // Ensure consistent display with proper spacing
        if (converted < 10) {
            // Add extra space for single digit speeds to improve alignment
            speedTextView.text = String.format(Locale.getDefault(), "%.2f %s", converted, unitText)
        } else {
            speedTextView.text = String.format(Locale.getDefault(), "%.2f %s", converted, unitText)
        }
        
        // Save current speed for potential unit conversion updates
        sharedPreferences.edit().putFloat("current_speed", speedInKmh).apply()
    }
    
    private fun updateStatusDisplay(isTracking: Boolean) {
        statusTextView.text = if (isTracking) "Status: Recording" else "Status: Not Recording"
    }
    
    private fun showStepGoalDialog() {
        val items = arrayOf("5,000 steps", "7,500 steps", "10,000 steps", "15,000 steps", "20,000 steps")
        val values = intArrayOf(5000, 7500, 10000, 15000, 20000)
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Set Daily Step Goal")
            .setItems(items) { _, which ->
                dailyStepGoal = values[which]
                sharedPreferences.edit().putInt("${currentUserId}_daily_step_goal", dailyStepGoal).apply()
                progressSteps.max = dailyStepGoal
                updateStepProgress(currentStepCount)
                val formatter = NumberFormat.getNumberInstance(Locale.US)
                Toast.makeText(context, "Daily goal set to ${formatter.format(dailyStepGoal)} steps", Toast.LENGTH_SHORT).show()
            }
            .show()
    }
    
    private fun updateStepProgress(steps: Int) {
        currentStepCount = steps
        
        // Add new steps to daily total (only if this is higher than current value to avoid duplicates)
        if (steps > 0) {
            val previousActivitySteps = sharedPreferences.getInt("${currentUserId}_current_activity_steps", 0)
            
            // If this is a new value and higher than previous, update the daily total
            if (steps > previousActivitySteps) {
                val additionalSteps = steps - previousActivitySteps
                dailyTotalSteps += additionalSteps
                
                // Save the new activity steps and daily total
                sharedPreferences.edit()
                    .putInt("${currentUserId}_current_activity_steps", steps)
                    .putInt("${currentUserId}_daily_total_steps", dailyTotalSteps)
                    .apply()
                    
                android.util.Log.d(TAG, "Daily total steps updated: $dailyTotalSteps (added $additionalSteps from current activity)")
            }
        }
        
        // Display total daily steps instead of just current activity
        val formatter = NumberFormat.getNumberInstance(Locale.US)
        stepsCountTextView.text = formatter.format(dailyTotalSteps)
        
        val percentage = if (dailyStepGoal > 0) (dailyTotalSteps * 100 / dailyStepGoal) else 0
        goalPercentageTextView.text = "$percentage% of ${formatter.format(dailyStepGoal)} steps"
        
        progressSteps.progress = dailyTotalSteps.coerceAtMost(dailyStepGoal)
    }
    
    /**
     * Check if we need to reset the daily step counter (at midnight)
     */
    private fun checkAndResetDailySteps() {
        val currentTimeMillis = System.currentTimeMillis()
        val lastResetMillis = sharedPreferences.getLong("${currentUserId}_last_daily_steps_reset", 0L)
        
        // Get today's date at midnight
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = currentTimeMillis
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val todayMidnightMillis = calendar.timeInMillis
        
        // Check if we last reset before today's midnight
        if (lastResetMillis < todayMidnightMillis) {
            // It's a new day, reset the counter
            android.util.Log.d(TAG, "New day detected - resetting daily step counter")
            dailyTotalSteps = 0
            sharedPreferences.edit()
                .putInt("${currentUserId}_daily_total_steps", 0)
                .putInt("${currentUserId}_current_activity_steps", 0)
                .putLong("${currentUserId}_last_daily_steps_reset", currentTimeMillis)
                .apply()
            
        } else {
            // Same day, load the existing total
            dailyTotalSteps = sharedPreferences.getInt("${currentUserId}_daily_total_steps", 0)
            android.util.Log.d(TAG, "Loaded existing daily step counter: $dailyTotalSteps")
        }
    }
    
    fun loadWeeklyStats() {
        android.util.Log.d(TAG, "loadWeeklyStats called")
        val currentUser = (requireActivity().application as MyApplication).firebaseAuth.currentUser
        
        if (currentUser != null) {
            val userId = currentUser.uid
            val dbRef = (requireActivity().application as MyApplication)
                .database
                .getReference("user_activities/$userId")

            val calendar = Calendar.getInstance()
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            calendar.firstDayOfWeek = Calendar.SUNDAY
            calendar.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
            val startOfWeekMillis = calendar.timeInMillis
            
            android.util.Log.d(TAG, "Loading weekly stats for user: $userId, starting from: $startOfWeekMillis")

            dbRef.orderByChild("timestamp")
                .startAt(startOfWeekMillis.toDouble())
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        var totalActivities = 0
                        var totalTimeSeconds = 0L
                        var totalDistanceKm = 0f

                        android.util.Log.d(TAG, "Weekly data snapshot size: ${snapshot.childrenCount}")

                        for (activitySnapshot in snapshot.children) {
                            android.util.Log.d(TAG, "Processing activity with key: ${activitySnapshot.key}")

                            val activity = activitySnapshot.getValue(ActivityRecord::class.java)
                            activity?.let {
                                android.util.Log.d(TAG, "Activity found: id=${it.id}, time=${it.durationSeconds}s, distance=${it.distanceKm}km, timestamp=${it.timestamp}")
                                totalActivities++
                                totalTimeSeconds += it.durationSeconds
                                totalDistanceKm += it.distanceKm
                            } ?: android.util.Log.e(TAG, "Failed to parse activity from snapshot")
                        }

                        android.util.Log.d(TAG, "Weekly stats totals: activities=$totalActivities, time=${totalTimeSeconds}s, distance=${totalDistanceKm}km")

                        activity?.runOnUiThread {
                            updateWeeklyStats(totalActivities, totalTimeSeconds, totalDistanceKm)
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        android.util.Log.e(TAG, "Database error: ${error.message}")
                        updateWeeklyStats(0, 0, 0f)
                    }
                })
        } else {
            android.util.Log.d(TAG, "No user logged in, showing zeros")
            updateWeeklyStats(0, 0, 0f)
        }
    }
    
    private fun updateWeeklyStats(activities: Int, timeSeconds: Long, distanceKm: Float) {
        android.util.Log.d(TAG, "Updating UI with stats: activities=$activities, timeSeconds=$timeSeconds, distanceKm=$distanceKm")
        
        weeklyActivitiesTextView.text = activities.toString()

        val hours = timeSeconds / 3600
        val minutes = (timeSeconds % 3600) / 60
        weeklyTimeTextView.text = String.format(Locale.getDefault(), "%dh %dm", hours, minutes)

        // Convert distance units if needed
        val convertedDistance = when {
            distanceUnit == "mi" -> distanceKm * 0.621371f // km to miles
            else -> distanceKm // Already in km
        }
        
        weeklyDistanceTextView.text = String.format(Locale.getDefault(), "%.2f %s", 
            convertedDistance, distanceUnit)
            
        // Update weekly pace if the TextView exists
        if (::weeklyPaceTextView.isInitialized && activities > 0) {
            val unitPreference = sharedPreferences.getString("units", "Kilometers (km)")
            val pace = com.android.stepsync.activity.SettingsActivity.calculatePace(
                distanceKm, timeSeconds, unitPreference ?: "Kilometers (km)")
            weeklyPaceTextView.text = pace
        }
    }

    private fun updateDisplayUnits(unitType: String) {
        distanceUnit = when (unitType) {
            "Miles (mi)" -> "mi"
            else -> "km" // Default to metric
        }
    }
    companion object {
        const val ACTION_ACTIVITY_COMPLETED = "com.android.stepsync.ACTIVITY_COMPLETED"
    }
}
