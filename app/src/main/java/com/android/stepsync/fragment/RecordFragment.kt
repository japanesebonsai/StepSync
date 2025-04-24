package com.android.stepsync.fragment

import android.Manifest
import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
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
import com.android.stepsync.fragment.HomeFragment
import com.android.stepsync.helper.StepTrackingService
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import java.util.Locale
import java.util.UUID

class RecordFragment : Fragment(R.layout.fragment_record) {
    //TODO record activities in database using map (include activity created date)
    //TODO pause activity
    private val TAG = "RecordFragment"

    private lateinit var timeTextView: TextView
    private lateinit var avgSpeedTextView: TextView
    private lateinit var distanceTextView: TextView
    private lateinit var recordButton: Button

    private var isTracking = false
    private val PERMISSION_REQUEST_ACTIVITY_RECOGNITION = 1001

    private var currentTimeSeconds: Long = 0
    private var currentDistanceKm: Float = 0f
    private var currentSpeedKmh: Float = 0f

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
                StepTrackingService.ACTION_TRACKING_STATUS -> {
                    isTracking = intent.getBooleanExtra(StepTrackingService.EXTRA_IS_TRACKING, false)
                    updateUI()
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        timeTextView = view.findViewById(R.id.text_time)
        avgSpeedTextView = view.findViewById(R.id.text_avgspeed)
        distanceTextView = view.findViewById(R.id.text_distance)
        recordButton = view.findViewById(R.id.button_record)

        recordButton.setOnClickListener {
            if (isTracking) {
                stopTracking()
            } else {
                startTracking()
            }
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
            addAction(StepTrackingService.ACTION_TRACKING_STATUS)
        }
        LocalBroadcastManager.getInstance(requireContext())
            .registerReceiver(trackingUpdateReceiver, intentFilter)

        checkTrackingStatus()
    }

    override fun onPause() {
        super.onPause()

        LocalBroadcastManager.getInstance(requireContext())
            .unregisterReceiver(trackingUpdateReceiver)
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
        val serviceIntent = Intent(requireContext(), StepTrackingService::class.java).apply {
            action = StepTrackingService.ACTION_STOP_TRACKING
        }
        requireContext().startService(serviceIntent)
        isTracking = false
        updateUI()

        saveActivityToDatabase()
    }
    
    private fun saveActivityToDatabase() {
        Log.d(TAG, "saveActivityToDatabase called - timeSeconds: $currentTimeSeconds, distanceKm: $currentDistanceKm")
        if (currentTimeSeconds > 1) {
            val currentUser = (requireActivity().application as MyApplication).firebaseAuth.currentUser
            
            if (currentUser != null) {
                val userId = currentUser.uid
                val activityId = UUID.randomUUID().toString()
                val timestamp = System.currentTimeMillis()
                
                Log.d(TAG, "Saving activity: userId=$userId, timeSeconds=$currentTimeSeconds, distanceKm=$currentDistanceKm, timestamp=$timestamp")
                
                val activityRecord = ActivityRecord(
                    id = activityId,
                    userId = userId,
                    timestamp = timestamp,
                    durationSeconds = currentTimeSeconds,
                    distanceKm = currentDistanceKm,
                    avgSpeedKmh = currentSpeedKmh
                )
                
                Log.d(TAG, "Activity object created: $activityRecord")

                val dbRef = (requireActivity().application as MyApplication)
                    .database
                    .getReference("user_activities/$userId/$activityId")
                
                Log.d(TAG, "Database reference path: user_activities/$userId/$activityId")
                
                dbRef.setValue(activityRecord)
                    .addOnSuccessListener {
                        Log.d(TAG, "Activity saved successfully with ID: $activityId")
                        val intent = Intent(HomeFragment.ACTION_ACTIVITY_COMPLETED)
                        LocalBroadcastManager.getInstance(requireContext()).sendBroadcast(intent)
                        Log.d(TAG, "Broadcast sent: ${HomeFragment.ACTION_ACTIVITY_COMPLETED}")
                        Handler(Looper.getMainLooper()).postDelayed({
                            Toast.makeText(context, "Activity recorded successfully!", Toast.LENGTH_SHORT).show()
                            if (activity is DashboardActivity) {
                                Log.d(TAG, "Switching to Home Fragment and updating stats")
                                (activity as DashboardActivity).switchToHomeAndUpdateStats()
                            } else {
                                Log.e(TAG, "Activity is not DashboardActivity: ${activity?.javaClass?.simpleName}")
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
            Toast.makeText(context, "Activity too short. Record for at least 1 second.", Toast.LENGTH_SHORT).show()
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
        isTracking = isServiceRunning(StepTrackingService::class.java)
        updateUI()

        if (isTracking) {
            val serviceIntent = Intent(requireContext(), StepTrackingService::class.java).apply {
                action = StepTrackingService.ACTION_REQUEST_STATUS
            }
            requireContext().startService(serviceIntent)
        }
    }

    private fun isServiceRunning(serviceClass: Class<*>): Boolean {
        val manager = requireContext().getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        return manager.getRunningServices(Integer.MAX_VALUE)
            .any { it.service.className == serviceClass.name }
    }

    private fun updateUI() {
        recordButton.text = if (isTracking) "STOP" else "START"

        if (!isTracking) {
            if (currentTimeSeconds == 0L) {
                updateTimeDisplay(0)
                updateDistanceDisplay(0f)
                updateSpeedDisplay(0f)
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
        distanceTextView.text = String.format(Locale.getDefault(), "%.2f", distanceInKm)
    }

    private fun updateSpeedDisplay(speedInKmh: Float) {
        avgSpeedTextView.text = String.format(Locale.getDefault(), "%.2f", speedInKmh)
    }
    
    companion object {
        const val ACTION_ACTIVITY_COMPLETED = "com.android.stepsync.ACTIVITY_COMPLETED"
    }
}