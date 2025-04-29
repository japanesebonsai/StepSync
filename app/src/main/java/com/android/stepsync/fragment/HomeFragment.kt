package com.android.stepsync.fragment

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.android.stepsync.R
import com.android.stepsync.activity.SettingsActivity
import com.android.stepsync.app.MyApplication
import com.android.stepsync.helper.StepTrackingService
import com.android.stepsync.data.ActivityRecord
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import java.util.Calendar
import java.util.Locale
import java.util.UUID

class HomeFragment : Fragment(R.layout.fragment_home) {
    private lateinit var distanceTextView: TextView
    private lateinit var timeTextView: TextView
    private lateinit var speedTextView: TextView
    private lateinit var statusTextView: TextView

    private lateinit var weeklyActivitiesTextView: TextView
    private lateinit var weeklyTimeTextView: TextView
    private lateinit var weeklyDistanceTextView: TextView
    
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
                    loadWeeklyStats()
                }
            }
        }
    }

    private val TAG = "HomeFragment"

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        android.util.Log.d(TAG, "onViewCreated called")

        distanceTextView = view.findViewById(R.id.text_home_distance)
        timeTextView = view.findViewById(R.id.text_home_time)
        speedTextView = view.findViewById(R.id.text_home_speed)
        statusTextView = view.findViewById(R.id.text_home_status)

        weeklyActivitiesTextView = view.findViewById(R.id.text_activities)
        weeklyTimeTextView = view.findViewById(R.id.text_time)
        weeklyDistanceTextView = view.findViewById(R.id.text_distance)
        
        val buttonSettings = view.findViewById<Button>(R.id.button_settings)

        buttonSettings.setOnClickListener {
            val intent = Intent(requireContext(), SettingsActivity::class.java)
            startActivity(intent)
        }

        // DEBUG: LONG PRESS ON SETTINGS BUTTON TO ADD TEST ACTIVITY
        buttonSettings.setOnLongClickListener {
            android.util.Log.d(TAG, "Long press on settings button - creating test activity")
            createTestActivity()
            true
        }

        weeklyActivitiesTextView.setOnClickListener {
            android.util.Log.d(TAG, "Activities TextView clicked - forcing reload of stats")
            loadWeeklyStats()
        }

        // DEBUG: LONG PRESS ON ACTIVITIES NUMBER TO CLEAR ALL ACTIVITIES
        weeklyActivitiesTextView.setOnLongClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Delete All Activities")
                .setMessage("This will delete ALL your activity records. This action cannot be undone. Continue?")
                .setPositiveButton("Yes, Delete All") { _, _ ->
                    deleteAllActivities()
                }
                .setNegativeButton("Cancel", null)
                .show()
            true
        }

        requestTrackingStatus()

        loadWeeklyStats()
    }
    
    override fun onResume() {
        super.onResume()
        
        val intentFilter = IntentFilter().apply {
            addAction(StepTrackingService.ACTION_TIME_UPDATE)
            addAction(StepTrackingService.ACTION_DISTANCE_UPDATE)
            addAction(StepTrackingService.ACTION_SPEED_UPDATE)
            addAction(StepTrackingService.ACTION_TRACKING_STATUS)
            addAction(ACTION_ACTIVITY_COMPLETED)
        }
        
        LocalBroadcastManager.getInstance(requireContext())
            .registerReceiver(trackingUpdateReceiver, intentFilter)
            
        requestTrackingStatus()
        loadWeeklyStats()
    }
    
    override fun onPause() {
        super.onPause()
        
        LocalBroadcastManager.getInstance(requireContext())
            .unregisterReceiver(trackingUpdateReceiver)
    }
    
    private fun requestTrackingStatus() {
        val serviceIntent = Intent(requireContext(), StepTrackingService::class.java).apply {
            action = StepTrackingService.ACTION_REQUEST_STATUS
        }
        requireContext().startService(serviceIntent)
    }
    
    private fun updateTimeDisplay(timeInSeconds: Long) {
        val hours = timeInSeconds / 3600
        val minutes = (timeInSeconds % 3600) / 60

        timeTextView.text= "${hours}h ${minutes}m"
    }
    
    private fun updateDistanceDisplay(distanceInKm: Float) {
        distanceTextView.text = String.format(Locale.getDefault(), "%.2f km", distanceInKm)
    }
    
    private fun updateSpeedDisplay(speedInKmh: Float) {
        speedTextView.text = String.format(Locale.getDefault(), "%.2f km/h", speedInKmh)
    }
    
    private fun updateStatusDisplay(isTracking: Boolean) {
        statusTextView.text = if (isTracking) "Status: Recording" else "Status: Not Recording"
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

            dbRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(allActivitiesSnapshot: DataSnapshot) {
                    android.util.Log.d(TAG, "All activities data snapshot size: ${allActivitiesSnapshot.childrenCount}")

                    if (allActivitiesSnapshot.childrenCount > 0) {
                        dbRef.orderByChild("timestamp")
                            .startAt(startOfWeekMillis.toDouble())
                            .addValueEventListener(object : ValueEventListener {
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
                        android.util.Log.d(TAG, "No activities found in database for this user")
                        updateWeeklyStats(0, 0, 0f)
                    }
                }
                
                override fun onCancelled(error: DatabaseError) {
                    android.util.Log.e(TAG, "Database error when checking all activities: ${error.message}")
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

        weeklyDistanceTextView.text = String.format(Locale.getDefault(), "%.2f km", distanceKm)
    }

    private fun createTestActivity() {
        val currentUser = (requireActivity().application as MyApplication).firebaseAuth.currentUser
        
        if (currentUser != null) {
            val userId = currentUser.uid
            val activityId = "test_" + UUID.randomUUID().toString()
            val timestamp = System.currentTimeMillis()
            
            android.util.Log.d(TAG, "Creating test activity: userId=$userId, timestamp=$timestamp")

            val activityRecord = ActivityRecord(
                id = activityId,
                userId = userId,
                timestamp = timestamp,
                durationSeconds = 30L,
                distanceKm = 0.2f,
                avgSpeedKmh = 2.4f
            )

            val dbRef = (requireActivity().application as MyApplication)
                .database
                .getReference("user_activities/$userId/$activityId")
            
            dbRef.setValue(activityRecord)
                .addOnSuccessListener {
                    android.util.Log.d(TAG, "Test activity saved successfully with ID: $activityId")
                    Toast.makeText(context, "Test activity created for debugging", Toast.LENGTH_SHORT).show()
                    loadWeeklyStats()
                }
                .addOnFailureListener { e ->
                    android.util.Log.e(TAG, "Error saving test activity: ${e.message}")
                    Toast.makeText(context, "Failed to create test activity: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun deleteAllActivities() {
        val currentUser = (requireActivity().application as MyApplication).firebaseAuth.currentUser
        
        if (currentUser != null) {
            val userId = currentUser.uid
            android.util.Log.d(TAG, "Deleting all activities for user $userId")
            
            val dbRef = (requireActivity().application as MyApplication)
                .database
                .getReference("user_activities/$userId")
                
            dbRef.removeValue()
                .addOnSuccessListener {
                    android.util.Log.d(TAG, "All activities deleted successfully")
                    Toast.makeText(context, "All activities deleted", Toast.LENGTH_SHORT).show()
                    updateWeeklyStats(0, 0, 0f)
                }
                .addOnFailureListener { e ->
                    android.util.Log.e(TAG, "Error deleting activities: ${e.message}")
                    Toast.makeText(context, "Failed to delete activities: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }
    
    companion object {
        const val ACTION_ACTIVITY_COMPLETED = "com.android.stepsync.ACTIVITY_COMPLETED"
    }
}