package com.android.stepsync.fragment

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.android.stepsync.R
import com.android.stepsync.activity.DashboardActivity
import com.android.stepsync.app.MyApplication
import com.android.stepsync.data.ActivityRecord
import com.android.stepsync.helper.StepTrackingService
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import java.util.Locale
import java.util.UUID
import java.text.NumberFormat

class RecordFragment : Fragment(R.layout.fragment_record) {
    //TODO pause activity
    private val TAG = "RecordFragment"

    private lateinit var timeTextView: TextView
    private lateinit var avgSpeedTextView: TextView
    private lateinit var distanceTextView: TextView
    private lateinit var stepsTextView: TextView
    private lateinit var recordButton: Button
    private lateinit var sharedPreferences: SharedPreferences
    private var distanceUnit = "km" // Default unit

    private var isTracking = false
    private var isPaused = false
    private val PERMISSION_REQUEST_ACTIVITY_RECOGNITION = 1001

    private var currentTimeSeconds: Long = 0
    private var currentDistanceKm: Float = 0f
    private var currentSpeedKmh: Float = 0f
    private var currentSteps: Int = 0
    
    private lateinit var pauseButton: Button
    private var currentUserId: String = ""

    private val unitsChangedReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == "com.android.stepsync.UNITS_CHANGED") {
                val unitType = intent.getStringExtra("unit_type") ?: "Kilometers (km)"
                updateDisplayUnits(unitType)
                
                // Update the displayed values with new units
                updateTimeDisplay(currentTimeSeconds)
                updateDistanceDisplay(currentDistanceKm)
                updateSpeedDisplay(currentSpeedKmh)
            }
        }
    }

    private val trackingUpdateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                StepTrackingService.ACTION_TIME_UPDATE -> {
                    val timeInSeconds = intent.getLongExtra(StepTrackingService.EXTRA_TIME, 0)
                    currentTimeSeconds = timeInSeconds
                    updateTimeDisplay(timeInSeconds)
                }
                StepTrackingService.ACTION_DISTANCE_UPDATE -> {
                    val distance = intent.getFloatExtra(StepTrackingService.EXTRA_DISTANCE, 0f)
                    currentDistanceKm = distance
                    updateDistanceDisplay(distance)
                }
                StepTrackingService.ACTION_SPEED_UPDATE -> {
                    val speed = intent.getFloatExtra(StepTrackingService.EXTRA_SPEED, 0f)
                    currentSpeedKmh = speed
                    updateSpeedDisplay(speed)
                }
                StepTrackingService.ACTION_STEPS_UPDATE -> {
                    val steps = intent.getIntExtra(StepTrackingService.EXTRA_STEPS, 0)
                    currentSteps = steps
                    updateStepsDisplay(steps)
                }
                StepTrackingService.ACTION_TRACKING_STATUS -> {
                    isTracking = intent.getBooleanExtra(StepTrackingService.EXTRA_IS_TRACKING, false)
                    isPaused = intent.getBooleanExtra(StepTrackingService.EXTRA_IS_PAUSED, false)
                    updateUI()
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        sharedPreferences = requireActivity().getSharedPreferences("step_sync_prefs", Context.MODE_PRIVATE)

        // Get current user ID
        val app = activity?.application as? MyApplication
        currentUserId = app?.firebaseAuth?.currentUser?.uid ?: ""

        val unitPreference = sharedPreferences.getString("units", "Kilometers (km)")
        
        timeTextView = view.findViewById(R.id.text_time)
        avgSpeedTextView = view.findViewById(R.id.text_avgspeed)
        distanceTextView = view.findViewById(R.id.text_distance)
        stepsTextView = view.findViewById(R.id.text_steps)
        recordButton = view.findViewById(R.id.button_record)
        pauseButton = view.findViewById(R.id.button_pause)

        updateDisplayUnits(unitPreference ?: "Kilometers (km)")

        recordButton.setOnClickListener {
            if (isTracking) {
                MaterialAlertDialogBuilder(requireActivity())
                    .setTitle("Save Activity")
                    .setMessage("Would you like to save your current activity?")
                    .setNegativeButton("Cancel") { dialog, _ ->
                        dialog.cancel()
                    }
                    .setPositiveButton("Finish") { _, _ ->
                        stopTracking()
                    }
                    .show()
            } else {
                startTracking()
            }
        }

        pauseButton.setOnClickListener {
            pauseTrackingToggle()
        }

        checkTrackingStatus()
        checkAndRequestPermissions()
    }

    override fun onResume() {
        super.onResume()

        val intentFilter = IntentFilter().apply {
            addAction(StepTrackingService.ACTION_TIME_UPDATE)
            addAction(StepTrackingService.ACTION_DISTANCE_UPDATE)
            addAction(StepTrackingService.ACTION_SPEED_UPDATE)
            addAction(StepTrackingService.ACTION_STEPS_UPDATE)
            addAction(StepTrackingService.ACTION_TRACKING_STATUS)
            addAction("com.android.stepsync.UNITS_CHANGED")
        }
        LocalBroadcastManager.getInstance(requireContext())
            .registerReceiver(trackingUpdateReceiver, intentFilter)
            
        // Also register for global broadcasts (for unit changes from settings)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            requireActivity().registerReceiver(
                unitsChangedReceiver, 
                IntentFilter("com.android.stepsync.UNITS_CHANGED"),
                Context.RECEIVER_NOT_EXPORTED
            )
        } else {
            requireActivity().registerReceiver(
                unitsChangedReceiver, 
                IntentFilter("com.android.stepsync.UNITS_CHANGED")
            )
        }

        checkTrackingStatus()
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

    private fun checkAndRequestPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        if (ContextCompat.checkSelfPermission(
                requireContext(), Manifest.permission.ACTIVITY_RECOGNITION
            ) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.ACTIVITY_RECOGNITION)
        }

        if (ContextCompat.checkSelfPermission(
                requireContext(), Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        if (ContextCompat.checkSelfPermission(
                requireContext(), Manifest.permission.FOREGROUND_SERVICE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.FOREGROUND_SERVICE_LOCATION)
        }

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                permissionsToRequest.toTypedArray(),
                PERMISSION_REQUEST_ACTIVITY_RECOGNITION
            )
        }
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            PERMISSION_REQUEST_ACTIVITY_RECOGNITION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "Activity recognition permission granted")
                } else {
                    Log.d(TAG, "Activity recognition permission denied")
                }
            }
            else -> super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    private fun startTracking() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACTIVITY_RECOGNITION
            ) == PackageManager.PERMISSION_GRANTED) {

            // Reset the current activity steps count
            sharedPreferences.edit().putInt("${currentUserId}_current_activity_steps", 0).apply()

            val serviceIntent = Intent(requireContext(), StepTrackingService::class.java).apply {
                action = StepTrackingService.ACTION_START_TRACKING
            }
            ContextCompat.startForegroundService(requireContext(), serviceIntent)
            isTracking = true
            updateUI()
        } else {
            checkAndRequestPermissions()
        }
    }

    private fun stopTracking() {
        // Clear the current activity steps since we're done
        sharedPreferences.edit().putInt("${currentUserId}_current_activity_steps", 0).apply()
        
        val serviceIntent = Intent(requireContext(), StepTrackingService::class.java).apply {
            action = StepTrackingService.ACTION_STOP_TRACKING
        }
        requireContext().startService(serviceIntent)
        isTracking = false
        updateUI()

        saveActivityToDatabase()
    }
    
    private fun saveActivityToDatabase() {
        Log.d(TAG, "saveActivityToDatabase called - timeSeconds: $currentTimeSeconds, distanceKm: $currentDistanceKm, steps: $currentSteps")
        if (currentTimeSeconds > 10) {
            val currentUser = (requireActivity().application as MyApplication).firebaseAuth.currentUser
            
            if (currentUser != null) {
                val userId = currentUser.uid
                val activityId = UUID.randomUUID().toString()
                val timestamp = System.currentTimeMillis()
                
                Log.d(TAG, "Saving activity: userId=$userId, timeSeconds=$currentTimeSeconds, distanceKm=$currentDistanceKm, timestamp=$timestamp")
                
                // Make sure speed is calculated correctly even if it wasn't updated during tracking
                val calculatedSpeed = if (currentTimeSeconds > 0) {
                    (currentDistanceKm / (currentTimeSeconds / 3600.0f))
                } else currentSpeedKmh
                
                Log.d(TAG, "Raw speed value: $currentSpeedKmh, Calculated speed: $calculatedSpeed")
                
                val activityRecord = ActivityRecord(
                    id = activityId,
                    userId = userId,
                    timestamp = timestamp,
                    durationSeconds = currentTimeSeconds,
                    distanceKm = currentDistanceKm,
                    avgSpeedKmh = calculatedSpeed,
                    steps = currentSteps // Use the actual steps counter
                )
                
                Log.d(TAG, "Activity object created: $activityRecord")

                val dbRef = (requireActivity().application as MyApplication)
                    .database
                    .getReference("user_activities/$userId/$activityId")
                
                Log.d(TAG, "Database reference path: user_activities/$userId/$activityId")
                
                // No need to estimate steps anymore since we have the actual count from the service
                
                dbRef.setValue(activityRecord)
                    .addOnSuccessListener {
                        Log.d(TAG, "Activity saved successfully with ID: $activityId")
                        val intent = Intent(HomeFragment.ACTION_ACTIVITY_COMPLETED)
                        intent.putExtra("activity_steps", currentSteps)
                        LocalBroadcastManager.getInstance(requireContext()).sendBroadcast(intent)
                        Log.d(TAG, "Broadcast sent: ${HomeFragment.ACTION_ACTIVITY_COMPLETED}")
                        
                        // Store a reference to the context/activity to prevent null reference
                        val activityContext = activity
                        
                        Handler(Looper.getMainLooper()).postDelayed({
                            // Check if context or activity is still valid before showing toast
                            if (activityContext != null && isAdded) {
                                Toast.makeText(activityContext, "Activity recorded successfully!", Toast.LENGTH_SHORT).show()
                                if (activityContext is DashboardActivity) {
                                    Log.d(TAG, "Switching to Home Fragment and updating stats")
                                    activityContext.switchToHomeAndUpdateStats()
                                } else {
                                    Log.e(TAG, "Activity is not DashboardActivity: ${activityContext.javaClass.simpleName}")
                                }
                            } else {
                                Log.d(TAG, "Fragment no longer attached, skipping UI updates")
                            }
                        }, 1000)
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Error saving activity: ${e.message}")
                        Toast.makeText(context, "Failed to save activity: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            } else {
                Log.e(TAG, "Cannot save activity: User not logged in")
            }
        } else {
            Log.d(TAG, "Activity not saved: too short (${currentTimeSeconds}s)")
            Toast.makeText(context, "Activity too short. Record for at least 10 second.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun createTestActivity() {
        Log.d(TAG, "Creating test activity for debugging")
        val currentUser = (requireActivity().application as MyApplication).firebaseAuth.currentUser
        
        if (currentUser != null) {
            val userId = currentUser.uid
            val activityId = UUID.randomUUID().toString()
            val timestamp = System.currentTimeMillis()

            val testActivityRecord = ActivityRecord(
                id = activityId,
                userId = userId,
                timestamp = timestamp,
                durationSeconds = 30L,
                distanceKm = 0.2f,
                avgSpeedKmh = 2.4f
            )
            
            Log.d(TAG, "Test activity created: $testActivityRecord")

            val dbRef = (requireActivity().application as MyApplication)
                .database
                .getReference("user_activities/$userId/$activityId")
            
            dbRef.setValue(testActivityRecord)
                .addOnSuccessListener {
                    Log.d(TAG, "Test activity saved successfully")
                    Toast.makeText(context, "Test activity created for debugging", Toast.LENGTH_SHORT).show()

                    verifyDataInDatabase(userId)
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error saving test activity: ${e.message}")
                }
        }
    }
    
    private fun verifyDataInDatabase(userId: String) {
        val dbRef = (requireActivity().application as MyApplication)
            .database
            .getReference("user_activities/$userId")
        
        dbRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                Log.d(TAG, "Database verification - Number of activities: ${snapshot.childrenCount}")
                for (child in snapshot.children) {
                    Log.d(TAG, "Activity found: ${child.key}")
                    val activity = child.getValue(ActivityRecord::class.java)
                    Log.d(TAG, "Activity data: time=${activity?.durationSeconds}s, distance=${activity?.distanceKm}km")
                }
            }
            
            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Database verification failed: ${error.message}")
            }
        })
    }

    private fun checkTrackingStatus() {
        isTracking = sharedPreferences.getBoolean(StepTrackingService.PREF_IS_TRACKING, false)
        isPaused = sharedPreferences.getBoolean(StepTrackingService.PREF_IS_PAUSED, false)
        updateUI()

        if (isTracking) {
            val serviceIntent = Intent(requireContext(), StepTrackingService::class.java).apply {
                action = StepTrackingService.ACTION_REQUEST_STATUS
            }
            requireContext().startService(serviceIntent)
        }
    }

    private fun updateUI() {
        recordButton.text = if (isTracking) "STOP" else "START"
        
        // Show pause button only when tracking is active
        if (isTracking) {
            pauseButton.visibility = View.VISIBLE
            pauseButton.text = if (isPaused) "RESUME" else "PAUSE"
        } else {
            pauseButton.visibility = View.GONE
        }

        if (!isTracking) {
            if (currentTimeSeconds == 0L) {
                updateTimeDisplay(0)
                updateDistanceDisplay(0f)
                updateSpeedDisplay(0f)
                updateStepsDisplay(0)
            }
        }
    }

    private fun updateTimeDisplay(timeInSeconds: Long) {
        val hours = timeInSeconds / 3600
        val minutes = (timeInSeconds % 3600) / 60
        val seconds = timeInSeconds % 60

        timeTextView.text = String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds)
    }

    private fun updateDistanceDisplay(distanceInKm: Float) {
        // Convert based on selected unit
        val converted = when (distanceUnit) {
            "mi" -> distanceInKm * 0.621371f // km to miles
            else -> distanceInKm // Already in km
        }
        
        distanceTextView.text = String.format(Locale.getDefault(), "%.2f", converted)
    }

    private fun updateSpeedDisplay(speedInKmh: Float) {
        // Convert based on selected unit
        val converted = when (distanceUnit) {
            "mi" -> speedInKmh * 0.621371f // km/h to mph
            else -> speedInKmh // Already in km/h
        }

        avgSpeedTextView.text = String.format(Locale.getDefault(), "%.2f", converted)
    }
    
    private fun updateStepsDisplay(steps: Int) {
        val formatter = NumberFormat.getNumberInstance(Locale.getDefault())
        stepsTextView.text = formatter.format(steps)
    }
    
    private fun updateDisplayUnits(unitType: String) {
        distanceUnit = when (unitType) {
            "Miles (mi)" -> "mi"
            else -> "km" // Default to metric
        }

        try {
            val speedUnitLabel = view?.findViewById<TextView>(R.id.text_speed_unit)
            val distanceUnitLabel = view?.findViewById<TextView>(R.id.text_distance_unit)

            speedUnitLabel?.text = if (distanceUnit == "mi") "MPH" else "KM/H"

            distanceUnitLabel?.text = if (distanceUnit == "mi") "MI" else "KM"
        } catch (e: Exception) {
            Log.e(TAG, "Error updating unit labels: ${e.message}")
        }
    }
    
    private fun pauseTrackingToggle() {
        if (isPaused) {
            // Resume tracking
            val serviceIntent = Intent(requireContext(), StepTrackingService::class.java).apply {
                action = StepTrackingService.ACTION_RESUME_TRACKING
            }
            requireContext().startService(serviceIntent)
        } else {
            // Pause tracking
            val serviceIntent = Intent(requireContext(), StepTrackingService::class.java).apply {
                action = StepTrackingService.ACTION_PAUSE_TRACKING
            }
            requireContext().startService(serviceIntent)
        }
        updateUI()
    }
    
    companion object {
        const val ACTION_ACTIVITY_COMPLETED = "com.android.stepsync.ACTIVITY_COMPLETED"
    }
}
