package com.android.stepsync.fragment

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.android.stepsync.R
import com.android.stepsync.activity.DashboardActivity
import com.android.stepsync.app.MyApplication
import com.android.stepsync.data.ActivityRecord
import com.android.stepsync.helper.ActivitySyncBuffer
import com.android.stepsync.helper.StepTrackingService
import com.android.stepsync.utils.StepSyncConfig
import com.android.stepsync.utils.TrackingFormatters
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.text.NumberFormat
import java.util.Locale
import java.util.UUID

class RecordFragment : Fragment(R.layout.fragment_record) {
    private lateinit var timeTextView: TextView
    private lateinit var avgSpeedTextView: TextView
    private lateinit var distanceTextView: TextView
    private lateinit var stepsTextView: TextView
    private lateinit var recordButton: Button
    private lateinit var sharedPreferences: SharedPreferences
    private var distanceUnit = StepSyncConfig.DISTANCE_UNIT_KM

    private var isTracking = false
    private var isPaused = false

    private var currentTimeSeconds: Long = 0
    private var currentDistanceKm: Float = 0f
    private var currentSpeedKmh: Float = 0f
    private var currentSteps: Int = 0
    
    private lateinit var pauseButton: Button
    private var currentUserId: String = ""

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        if (results.values.all { it }) {
            Log.d(TAG, "Tracking permissions granted")
        } else {
            Log.d(TAG, "One or more tracking permissions denied")
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
                StepSyncConfig.ACTION_UNITS_CHANGED -> {
                    val unitType = intent.getStringExtra(StepSyncConfig.EXTRA_UNIT_TYPE)
                    updateDisplayUnits(unitType)
                    updateTimeDisplay(currentTimeSeconds)
                    updateDistanceDisplay(currentDistanceKm)
                    updateSpeedDisplay(currentSpeedKmh)
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        sharedPreferences = requireActivity().getSharedPreferences(StepSyncConfig.PREFS_NAME, Context.MODE_PRIVATE)

        val app = activity?.application as? MyApplication
        currentUserId = app?.firebaseAuth?.currentUser?.uid ?: ""

        val unitPreference = sharedPreferences.getString(StepSyncConfig.KEY_UNITS, StepSyncConfig.DEFAULT_UNITS)
        
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
            addAction(StepSyncConfig.ACTION_UNITS_CHANGED)
        }
        ContextCompat.registerReceiver(
            requireContext(),
            trackingUpdateReceiver,
            intentFilter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

        checkTrackingStatus()
    }

    override fun onPause() {
        super.onPause()

        try {
            requireActivity().unregisterReceiver(trackingUpdateReceiver)
        } catch (_: IllegalArgumentException) {
        }
    }

    private fun checkAndRequestPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            ContextCompat.checkSelfPermission(
                requireContext(), Manifest.permission.ACTIVITY_RECOGNITION
            ) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.ACTIVITY_RECOGNITION)
        }

        if (ContextCompat.checkSelfPermission(
                requireContext(), Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                requireContext(), Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        if (permissionsToRequest.isNotEmpty()) {
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    private fun startTracking() {
        if (hasRequiredTrackingPermissions()) {
            sharedPreferences.edit()
                .putInt(userKey(StepSyncConfig.KEY_CURRENT_ACTIVITY_STEPS), 0)
                .apply()

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
        sharedPreferences.edit()
            .putInt(userKey(StepSyncConfig.KEY_CURRENT_ACTIVITY_STEPS), 0)
            .apply()
        
        val serviceIntent = Intent(requireContext(), StepTrackingService::class.java).apply {
            action = StepTrackingService.ACTION_STOP_TRACKING
        }
        requireContext().startService(serviceIntent)
        isTracking = false
        updateUI()

        saveActivityToDatabase()
    }
    
    private fun saveActivityToDatabase() {
        if (currentTimeSeconds > 10) {
            val currentUser = (requireActivity().application as MyApplication).firebaseAuth.currentUser
            
            if (currentUser != null) {
                val userId = currentUser.uid
                val activityId = UUID.randomUUID().toString()
                val timestamp = System.currentTimeMillis()

                val calculatedSpeed = if (currentTimeSeconds > 0) {
                    (currentDistanceKm / (currentTimeSeconds / 3600.0f))
                } else currentSpeedKmh

                val activityRecord = ActivityRecord(
                    id = activityId,
                    userId = userId,
                    timestamp = timestamp,
                    durationSeconds = currentTimeSeconds,
                    distanceKm = currentDistanceKm,
                    avgSpeedKmh = calculatedSpeed,
                    steps = currentSteps
                )

                val app = requireActivity().application as MyApplication
                val syncBuffer = ActivitySyncBuffer(requireContext())
                syncBuffer.enqueue(activityRecord)

                val dataSyncEnabled = sharedPreferences.getBoolean(
                    StepSyncConfig.KEY_DATA_SYNC,
                    StepSyncConfig.DEFAULT_DATA_SYNC
                )
                if (!dataSyncEnabled) {
                    Toast.makeText(context, "Activity saved locally. Auto sync is off.", Toast.LENGTH_SHORT).show()
                    handleActivitySaved()
                    return
                }

                syncBuffer.flush(app.database.reference, userId,
                    onSuccess = {
                        handleActivitySaved()
                    },
                    onFailure = { e ->
                        Log.e(TAG, "Error saving activity: ${e.message}")
                        Toast.makeText(context, "Saved locally. Sync will retry next time.", Toast.LENGTH_SHORT).show()
                    }
                )
            } else {
                Toast.makeText(context, "Sign in again to save this activity.", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Activity too short. Record for at least 10 seconds.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleActivitySaved() {
        val intent = Intent(HomeFragment.ACTION_ACTIVITY_COMPLETED)
        intent.putExtra("activity_steps", currentSteps)
        intent.setPackage(requireContext().packageName)
        requireContext().sendBroadcast(intent)

        val activityContext = activity ?: return
        if (!isAdded) return

        Toast.makeText(activityContext, "Activity recorded successfully!", Toast.LENGTH_SHORT).show()
        (activityContext as? DashboardActivity)?.switchToHomeAndUpdateStats()
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
        timeTextView.text = TrackingFormatters.formatClockDuration(timeInSeconds)
    }

    private fun updateDistanceDisplay(distanceInKm: Float) {
        val converted = TrackingFormatters.convertDistanceFromKm(distanceInKm, distanceUnit)
        distanceTextView.text = String.format(Locale.getDefault(), "%.2f", converted)
    }

    private fun updateSpeedDisplay(speedInKmh: Float) {
        val converted = TrackingFormatters.convertSpeedFromKmh(speedInKmh, distanceUnit)
        avgSpeedTextView.text = String.format(Locale.getDefault(), "%.2f", converted)
    }
    
    private fun updateStepsDisplay(steps: Int) {
        val formatter = NumberFormat.getNumberInstance(Locale.getDefault())
        stepsTextView.text = formatter.format(steps)
    }
    
    private fun updateDisplayUnits(unitType: String?) {
        distanceUnit = TrackingFormatters.distanceUnitCode(unitType)

        try {
            val speedUnitLabel = view?.findViewById<TextView>(R.id.text_speed_unit)
            val distanceUnitLabel = view?.findViewById<TextView>(R.id.text_distance_unit)

            speedUnitLabel?.text = TrackingFormatters.speedUnitLabel(distanceUnit).uppercase(Locale.getDefault())
            distanceUnitLabel?.text = distanceUnit.uppercase(Locale.getDefault())
        } catch (e: Exception) {
            Log.e(TAG, "Error updating unit labels: ${e.message}")
        }
    }
    
    private fun pauseTrackingToggle() {
        val action = if (isPaused) {
            StepTrackingService.ACTION_RESUME_TRACKING
        } else {
            StepTrackingService.ACTION_PAUSE_TRACKING
        }

        val serviceIntent = Intent(requireContext(), StepTrackingService::class.java).apply {
            this.action = action
        }
        requireContext().startService(serviceIntent)
        updateUI()
    }

    private fun hasRequiredTrackingPermissions(): Boolean {
        val activityRecognitionGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACTIVITY_RECOGNITION
            ) == PackageManager.PERMISSION_GRANTED

        val locationGranted = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        return activityRecognitionGranted && locationGranted
    }

    private fun userKey(key: String): String {
        return StepSyncConfig.userScopedKey(currentUserId, key)
    }

    companion object {
        private const val TAG = "RecordFragment"
        const val ACTION_ACTIVITY_COMPLETED = "com.android.stepsync.ACTIVITY_COMPLETED"
    }
}
