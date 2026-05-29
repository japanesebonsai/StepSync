package com.android.stepsync.fragment

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.android.stepsync.R
import com.android.stepsync.activity.SettingsActivity
import com.android.stepsync.app.MyApplication
import com.android.stepsync.helper.StepTrackingService
import com.android.stepsync.data.ActivityRecord
import com.android.stepsync.utils.StepSyncConfig
import com.android.stepsync.utils.TrackingFormatters
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale

class HomeFragment : Fragment(R.layout.fragment_home) {
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
    private var dailyStepGoal = StepSyncConfig.DEFAULT_DAILY_STEP_GOAL
    private var currentStepCount = 0
    private var dailyTotalSteps = 0
    private var distanceUnit = "km"
    private var currentUserId: String = ""
    
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

                    val stepLengthCm = sharedPreferences.getInt(
                        StepSyncConfig.KEY_STEP_LENGTH,
                        StepSyncConfig.DEFAULT_STEP_LENGTH_CM
                    )
                    val stepsPerKm = TrackingFormatters.stepsPerKm(stepLengthCm)
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
                    val activitySteps = intent.getIntExtra("activity_steps", 0)
                    if (activitySteps > 0) {
                        dailyTotalSteps += activitySteps
                        sharedPreferences.edit()
                            .putInt(userKey(StepSyncConfig.KEY_DAILY_TOTAL_STEPS), dailyTotalSteps)
                            .putInt(userKey(StepSyncConfig.KEY_CURRENT_ACTIVITY_STEPS), 0)
                            .apply()

                        renderDailyStepProgress()
                    }
                    
                    loadWeeklyStats()
                }
                StepSyncConfig.ACTION_UNITS_CHANGED -> {
                    val unitType = intent.getStringExtra(StepSyncConfig.EXTRA_UNIT_TYPE)
                    updateDisplayUnits(unitType)
                    val isTracking = sharedPreferences.getBoolean(StepTrackingService.PREF_IS_TRACKING, false)
                    val distance = sharedPreferences.getFloat(StepSyncConfig.KEY_CURRENT_DISTANCE, 0f)
                    val speed = sharedPreferences.getFloat(StepSyncConfig.KEY_CURRENT_SPEED, 0f)

                    updateDistanceDisplay(distance)
                    updateSpeedDisplay(speed)
                    updateStatusDisplay(isTracking)
                    loadWeeklyStats()
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sharedPreferences = requireActivity().getSharedPreferences(StepSyncConfig.PREFS_NAME, Context.MODE_PRIVATE)
        val app = activity?.application as? MyApplication
        currentUserId = app?.firebaseAuth?.currentUser?.uid ?: ""
        
        dailyStepGoal = sharedPreferences.getInt(
            userKey(StepSyncConfig.KEY_DAILY_STEP_GOAL),
            StepSyncConfig.DEFAULT_DAILY_STEP_GOAL
        )
        checkAndResetDailySteps()

        val unitPreference = sharedPreferences.getString(
            StepSyncConfig.KEY_UNITS,
            StepSyncConfig.DEFAULT_UNITS
        )
        updateDisplayUnits(unitPreference)

        distanceTextView = view.findViewById(R.id.text_home_distance)
        timeTextView = view.findViewById(R.id.text_home_time)
        speedTextView = view.findViewById(R.id.text_home_speed)
        statusTextView = view.findViewById(R.id.text_home_status)

        weeklyActivitiesTextView = view.findViewById(R.id.text_activities)
        weeklyTimeTextView = view.findViewById(R.id.text_time)
        weeklyDistanceTextView = view.findViewById(R.id.text_distance)
        
        view.findViewById<TextView?>(R.id.text_pace)?.let { weeklyPaceTextView = it }
        
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
            addAction(StepSyncConfig.ACTION_UNITS_CHANGED)
        }
        
        ContextCompat.registerReceiver(
            requireContext(),
            trackingUpdateReceiver,
            intentFilter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
            
        requestTrackingStatus()
        loadWeeklyStats()
    }
    
    override fun onPause() {
        super.onPause()
        
        try {
            requireActivity().unregisterReceiver(trackingUpdateReceiver)
        } catch (_: IllegalArgumentException) {
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
        timeTextView.text = TrackingFormatters.formatCompactDuration(timeInSeconds)
        sharedPreferences.edit().putLong(StepSyncConfig.KEY_CURRENT_TIME_SECONDS, timeInSeconds).apply()
    }
    
    private fun updateDistanceDisplay(distanceInKm: Float) {
        distanceTextView.text = TrackingFormatters.formatDistance(distanceInKm, distanceUnit)
        sharedPreferences.edit().putFloat(StepSyncConfig.KEY_CURRENT_DISTANCE, distanceInKm).apply()
    }
    
    private fun updateSpeedDisplay(speedInKmh: Float) {
        speedTextView.text = TrackingFormatters.formatSpeed(speedInKmh, distanceUnit)
        sharedPreferences.edit().putFloat(StepSyncConfig.KEY_CURRENT_SPEED, speedInKmh).apply()
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
                sharedPreferences.edit().putInt(userKey(StepSyncConfig.KEY_DAILY_STEP_GOAL), dailyStepGoal).apply()
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
            val previousActivitySteps = sharedPreferences.getInt(userKey(StepSyncConfig.KEY_CURRENT_ACTIVITY_STEPS), 0)
            
            if (steps > previousActivitySteps) {
                val additionalSteps = steps - previousActivitySteps
                dailyTotalSteps += additionalSteps
                
                sharedPreferences.edit()
                    .putInt(userKey(StepSyncConfig.KEY_CURRENT_ACTIVITY_STEPS), steps)
                    .putInt(userKey(StepSyncConfig.KEY_DAILY_TOTAL_STEPS), dailyTotalSteps)
                    .apply()
            }
        }
        renderDailyStepProgress()
    }

    private fun renderDailyStepProgress() {
        val formatter = NumberFormat.getNumberInstance(Locale.US)
        stepsCountTextView.text = formatter.format(dailyTotalSteps)
        
        val percentage = if (dailyStepGoal > 0) (dailyTotalSteps * 100 / dailyStepGoal) else 0
        goalPercentageTextView.text = "$percentage% of ${formatter.format(dailyStepGoal)} steps"
        
        progressSteps.progress = dailyTotalSteps.coerceAtMost(dailyStepGoal)
    }
    
    private fun checkAndResetDailySteps() {
        val currentTimeMillis = System.currentTimeMillis()
        val lastResetMillis = sharedPreferences.getLong(userKey(StepSyncConfig.KEY_LAST_DAILY_STEPS_RESET), 0L)
        
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = currentTimeMillis
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val todayMidnightMillis = calendar.timeInMillis
        
        if (lastResetMillis < todayMidnightMillis) {
            dailyTotalSteps = 0
            sharedPreferences.edit()
                .putInt(userKey(StepSyncConfig.KEY_DAILY_TOTAL_STEPS), 0)
                .putInt(userKey(StepSyncConfig.KEY_CURRENT_ACTIVITY_STEPS), 0)
                .putLong(userKey(StepSyncConfig.KEY_LAST_DAILY_STEPS_RESET), currentTimeMillis)
                .apply()
            
        } else {
            dailyTotalSteps = sharedPreferences.getInt(userKey(StepSyncConfig.KEY_DAILY_TOTAL_STEPS), 0)
        }
    }
    
    fun loadWeeklyStats() {
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
            
            dbRef.orderByChild("timestamp")
                .startAt(startOfWeekMillis.toDouble())
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        var totalActivities = 0
                        var totalTimeSeconds = 0L
                        var totalDistanceKm = 0f
                        for (activitySnapshot in snapshot.children) {
                            val activity = activitySnapshot.getValue(ActivityRecord::class.java)
                            activity?.let {
                                totalActivities++
                                totalTimeSeconds += it.durationSeconds
                                totalDistanceKm += it.distanceKm
                            } ?: Log.w(TAG, "Failed to parse weekly activity ${activitySnapshot.key}")
                        }

                        activity?.runOnUiThread {
                            updateWeeklyStats(totalActivities, totalTimeSeconds, totalDistanceKm)
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.e(TAG, "Failed to load weekly stats: ${error.message}")
                        updateWeeklyStats(0, 0, 0f)
                    }
                })
        } else {
            updateWeeklyStats(0, 0, 0f)
        }
    }
    
    private fun updateWeeklyStats(activities: Int, timeSeconds: Long, distanceKm: Float) {
        weeklyActivitiesTextView.text = activities.toString()
        weeklyTimeTextView.text = TrackingFormatters.formatHoursAndMinutes(timeSeconds)
        weeklyDistanceTextView.text = TrackingFormatters.formatDistance(distanceKm, distanceUnit)
            
        if (::weeklyPaceTextView.isInitialized && activities > 0) {
            val unitPreference = sharedPreferences.getString(
                StepSyncConfig.KEY_UNITS,
                StepSyncConfig.DEFAULT_UNITS
            )
            weeklyPaceTextView.text = TrackingFormatters.formatPace(distanceKm, timeSeconds, unitPreference)
        }
    }

    private fun updateDisplayUnits(unitType: String?) {
        distanceUnit = TrackingFormatters.distanceUnitCode(unitType)
    }

    private fun userKey(key: String): String {
        return StepSyncConfig.userScopedKey(currentUserId, key)
    }

    companion object {
        private const val TAG = "HomeFragment"
        const val ACTION_ACTIVITY_COMPLETED = "com.android.stepsync.ACTIVITY_COMPLETED"
    }
}
