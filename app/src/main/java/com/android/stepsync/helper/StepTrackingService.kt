package com.android.stepsync.helper

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.android.stepsync.R
import com.android.stepsync.activity.DashboardActivity
import java.util.concurrent.TimeUnit

class StepTrackingService : Service(), SensorEventListener {
    private val TAG = "StepTrackingService"

    companion object {
        const val NOTIFICATION_ID = 1001
        const val NOTIFICATION_CHANNEL_ID = "tracking_channel"

        const val ACTION_START_TRACKING = "com.android.stepsync.START_TRACKING"
        const val ACTION_STOP_TRACKING = "com.android.stepsync.STOP_TRACKING"
        const val ACTION_REQUEST_STATUS = "com.android.stepsync.REQUEST_STATUS"

        const val ACTION_TIME_UPDATE = "com.android.stepsync.TIME_UPDATE"
        const val ACTION_DISTANCE_UPDATE = "com.android.stepsync.DISTANCE_UPDATE"
        const val ACTION_SPEED_UPDATE = "com.android.stepsync.SPEED_UPDATE"
        const val ACTION_TRACKING_STATUS = "com.android.stepsync.TRACKING_STATUS"

        const val EXTRA_TIME = "extra_time"
        const val EXTRA_DISTANCE = "extra_distance"
        const val EXTRA_SPEED = "extra_speed"
        const val EXTRA_IS_TRACKING = "extra_is_tracking"

        private const val STEP_LENGTH_METERS = 0.65f
        private const val UPDATE_INTERVAL_MS = 1000L
    }

    private var isTracking = false
    private var startTimeMillis: Long = 0
    private var elapsedTimeSeconds: Long = 0
    private var initialStepCount: Int = -1
    private var currentSteps: Int = 0
    private var totalDistanceKm: Float = 0f
    private var currentSpeedKmh: Float = 0f
    
    // FOR DEBUGGING
    private val debugMode = false // CHANGE TO TRUE TO GENERATE VIRTUAL STEPS
    private var debugStepsAdded = 0

    private lateinit var sensorManager: SensorManager
    private var stepSensor: Sensor? = null

    private val handler = Handler(Looper.getMainLooper())
    private val updateRunnable = object : Runnable {
        override fun run() {
            if (isTracking) {
                updateTracking()
                if (debugMode) {
                    addDebugSteps()
                }
                handler.postDelayed(this, UPDATE_INTERVAL_MS)
            }
        }
    }

    private fun addDebugSteps() {
        val stepsToAdd = (3..5).random()
        debugStepsAdded += stepsToAdd

        val additionalDistanceMeters = stepsToAdd * STEP_LENGTH_METERS
        totalDistanceKm += (additionalDistanceMeters / 1000f)

        broadcastDistanceUpdate()
        broadcastSpeedUpdate()
        
        Log.d(TAG, "Added $stepsToAdd debug steps, total: $debugStepsAdded, distance: $totalDistanceKm km")
    }

    override fun onCreate() {
        super.onCreate()
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_TRACKING -> startTracking()
            ACTION_STOP_TRACKING -> stopTracking()
            ACTION_REQUEST_STATUS -> broadcastCurrentStatus()
        }

        return START_STICKY
    }

    private fun startTracking() {
        if (!isTracking) {
            Log.d(TAG, "Starting tracking service")

            createNotificationChannel()
            val notification = createNotification()
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(NOTIFICATION_ID, notification, 
                    android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION or
                    android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH)
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }

            initialStepCount = -1
            currentSteps = 0
            totalDistanceKm = 0f
            currentSpeedKmh = 0f
            startTimeMillis = System.currentTimeMillis()
            elapsedTimeSeconds = 0

            stepSensor?.let {
                sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
            }

            isTracking = true
            handler.post(updateRunnable)

            broadcastTrackingStatus()
        }
    }

    private fun stopTracking() {
        if (isTracking) {
            Log.d(TAG, "Stopping tracking service")

            sensorManager.unregisterListener(this)

            handler.removeCallbacks(updateRunnable)

            isTracking = false

            stopForeground(true)
            stopSelf()

            broadcastTrackingStatus()
        }
    }

    private fun updateTracking() {
        elapsedTimeSeconds = TimeUnit.MILLISECONDS.toSeconds(
            System.currentTimeMillis() - startTimeMillis
        )

        if (currentSteps > 0 && elapsedTimeSeconds > 0) {
            currentSpeedKmh = (totalDistanceKm / (elapsedTimeSeconds / 3600.0f))
        }

        broadcastTimeUpdate()
        broadcastDistanceUpdate()
        broadcastSpeedUpdate()

        updateNotification()
    }

    private fun broadcastCurrentStatus() {
        broadcastTrackingStatus()
        broadcastTimeUpdate()
        broadcastDistanceUpdate()
        broadcastSpeedUpdate()
    }

    private fun broadcastTrackingStatus() {
        val intent = Intent(ACTION_TRACKING_STATUS).apply {
            putExtra(EXTRA_IS_TRACKING, isTracking)
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    private fun broadcastTimeUpdate() {
        val intent = Intent(ACTION_TIME_UPDATE).apply {
            putExtra(EXTRA_TIME, elapsedTimeSeconds)
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    private fun broadcastDistanceUpdate() {
        val intent = Intent(ACTION_DISTANCE_UPDATE).apply {
            putExtra(EXTRA_DISTANCE, totalDistanceKm)
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    private fun broadcastSpeedUpdate() {
        val intent = Intent(ACTION_SPEED_UPDATE).apply {
            putExtra(EXTRA_SPEED, currentSpeedKmh)
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_STEP_COUNTER) {
            val steps = event.values[0].toInt()

            if (initialStepCount == -1) {
                initialStepCount = steps
            }

            val newSteps = steps - initialStepCount
            val additionalSteps = newSteps - currentSteps
            currentSteps = newSteps

            Log.d(TAG, "Step detected: Additional: $additionalSteps, Total: $currentSteps")

            if (additionalSteps > 0) {
                val additionalDistanceMeters = additionalSteps * STEP_LENGTH_METERS
                totalDistanceKm += (additionalDistanceMeters / 1000f)
                Log.d(TAG, "Distance updated: +${additionalDistanceMeters}m, Total: ${totalDistanceKm}km")
                broadcastDistanceUpdate()
                broadcastSpeedUpdate()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {

    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Tracking Service"
            val descriptionText = "Shows tracking progress"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(NOTIFICATION_CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val notificationIntent = Intent(this, DashboardActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("StepSync")
            .setContentText("Recording your steps...")
            .setSmallIcon(R.drawable.ic_notif_icon)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification() {
        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("StepSync")
            .setContentText("Distance: ${String.format("%.2f", totalDistanceKm)} km")
            .setSmallIcon(R.drawable.ic_notif_icon)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()

        if (isTracking) {
            sensorManager.unregisterListener(this)
            handler.removeCallbacks(updateRunnable)
        }
    }
}