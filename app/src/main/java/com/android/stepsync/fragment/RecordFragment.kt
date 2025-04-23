package com.android.stepsync.activity

import android.Manifest
import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.android.stepsync.R
import com.android.stepsync.helper.StepTrackingService
import java.util.Locale

class RecordFragment : Fragment(R.layout.fragment_record) {
    private val TAG = "RecordFragment"

    private lateinit var timeTextView: TextView
    private lateinit var avgSpeedTextView: TextView
    private lateinit var distanceTextView: TextView
    private lateinit var recordButton: Button

    private var isTracking = false
    private val PERMISSION_REQUEST_ACTIVITY_RECOGNITION = 1001

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
            updateTimeDisplay(0)
            updateDistanceDisplay(0f)
            updateSpeedDisplay(0f)
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
}