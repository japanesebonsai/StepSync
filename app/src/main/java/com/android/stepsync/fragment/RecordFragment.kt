package com.android.stepsync.activity


import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.fragment.app.Fragment
import com.android.stepsync.R
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority


class RecordFragment : Fragment(), SensorEventListener {

    private lateinit var textTime: TextView
    private lateinit var textAvgSpeed: TextView
    private lateinit var textDistance: TextView
    private lateinit var buttonRecord: Button

    private val sensorManager by lazy {
        requireContext().getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }
    private val stepSensor: Sensor? by lazy {
        sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
    }
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var locationCallback: LocationCallback? = null

    private var isRecording = false
    private var initialStepCount: Float = -1f
    private var totalDistance: Float = 0f  // meters
    private var previousLocation: Location? = null
    private var startTime: Long = 0L
    private val handler = Handler(Looper.getMainLooper())
    private val timerRunnable = object : Runnable {
        override fun run() {
            val elapsed = System.currentTimeMillis() - startTime
            textTime.text = formatElapsedTime(elapsed)
            updateAvgSpeed(elapsed)
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_record, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        textTime = view.findViewById(R.id.text_time)
        textAvgSpeed = view.findViewById(R.id.text_avgspeed)
        textDistance = view.findViewById(R.id.text_distance)
        buttonRecord = view.findViewById(R.id.button_record)

        fusedLocationClient =
            LocationServices.getFusedLocationProviderClient(requireContext())

        buttonRecord.setOnClickListener {
            if (isRecording) stopTracking() else startTracking()
        }
    }

    private fun startTracking() {
        isRecording = true
        buttonRecord.text = "STOP"
        initialStepCount = -1f
        totalDistance = 0f
        previousLocation = null
        startTime = System.currentTimeMillis()
        handler.post(timerRunnable)
        stepSensor?.let { sensor ->
            sensorManager.registerListener(
                this, sensor, SensorManager.SENSOR_DELAY_NORMAL
            )
        }
        startLocationUpdates()
    }

    private fun stopTracking() {
        isRecording = false
        buttonRecord.text = "START"
        handler.removeCallbacks(timerRunnable)
        sensorManager.unregisterListener(this)
        stopLocationUpdates()
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (isRecording && event?.sensor?.type == Sensor.TYPE_STEP_COUNTER) {
            val totalSteps = event.values[0]
            if (initialStepCount < 0) initialStepCount = totalSteps
            // You can use stepsInSession = totalSteps - initialStepCount if needed
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(
                requireContext(), Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                1
            )
            return
        }
        // Use new LocationRequest.Builder API
        val request = LocationRequest.Builder(5000L)
            .setMinUpdateIntervalMillis(2000L)
            .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { loc ->
                    if (isRecording) {
                        previousLocation?.let { prev ->
                            val delta = prev.distanceTo(loc)
                            totalDistance += delta
                            textDistance.text = String.format(
                                "%.2f", totalDistance / 1000f
                            )
                        }
                        previousLocation = loc
                    }
                }
            }
        }
        fusedLocationClient.requestLocationUpdates(
            request, locationCallback!!, Looper.getMainLooper()
        )
    }

    private fun stopLocationUpdates() {
        locationCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
        }
    }

    private fun updateAvgSpeed(elapsedMillis: Long) {
        if (isRecording && elapsedMillis > 0) {
            // m/s to km/h: *3.6
            val speedMs = totalDistance / (elapsedMillis / 1000f)
            val speedKmh = speedMs * 3.6f
            textAvgSpeed.text = String.format("%.2f", speedKmh)
        }
    }

    private fun formatElapsedTime(elapsedMillis: Long): String {
        val hours = elapsedMillis / 3600000
        val minutes = (elapsedMillis % 3600000) / 60000
        val seconds = (elapsedMillis % 60000) / 1000
        return String.format("%d:%02d:%02d", hours, minutes, seconds)
    }
}