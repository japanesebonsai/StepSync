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
import androidx.core.app.NotificationCompat
import com.android.stepsync.R
import com.android.stepsync.activity.DashboardActivity
import com.android.stepsync.app.MyApplication
import com.android.stepsync.utils.StepSyncConfig
import com.android.stepsync.utils.TrackingFormatters
import java.util.concurrent.TimeUnit

class StepTrackingService : Service(), SensorEventListener {
    companion object {
        const val NOTIFICATION_ID = 1001
        const val NOTIFICATION_CHANNEL_ID = "tracking_channel"

        const val ACTION_START_TRACKING = "com.android.stepsync.START_TRACKING"
        const val ACTION_STOP_TRACKING = "com.android.stepsync.STOP_TRACKING"
        const val ACTION_PAUSE_TRACKING = "com.android.stepsync.PAUSE_TRACKING"
        const val ACTION_RESUME_TRACKING = "com.android.stepsync.RESUME_TRACKING"
        const val ACTION_REQUEST_STATUS = "com.android.stepsync.REQUEST_STATUS"

        const val ACTION_TIME_UPDATE = "com.android.stepsync.TIME_UPDATE"
        const val ACTION_DISTANCE_UPDATE = "com.android.stepsync.DISTANCE_UPDATE"
        const val ACTION_SPEED_UPDATE = "com.android.stepsync.SPEED_UPDATE"
        const val ACTION_STEPS_UPDATE = "com.android.stepsync.STEPS_UPDATE"
        const val ACTION_TRACKING_STATUS = "com.android.stepsync.TRACKING_STATUS"

        const val EXTRA_TIME = "extra_time"
        const val EXTRA_DISTANCE = "extra_distance"
        const val EXTRA_SPEED = "extra_speed"
        const val EXTRA_STEPS = "extra_steps"
        const val EXTRA_IS_TRACKING = "extra_is_tracking"
        const val EXTRA_IS_PAUSED = "extra_is_paused"
        const val EXTRA_USER_ID = "extra_user_id"

        const val PREFS_NAME = StepSyncConfig.PREFS_NAME
        const val PREF_IS_TRACKING = "is_tracking"
        const val PREF_IS_PAUSED = "is_paused"

        private const val UPDATE_INTERVAL_MS = 1000L
    }

    private var isTracking = false
    private var isPaused = false
    private var startTimeMillis: Long = 0
    private var pausedTimeMillis: Long = 0
    private var totalPausedMillis: Long = 0
    private var elapsedTimeSeconds: Long = 0
    private var initialStepCount: Int = -1
    private var currentSteps: Int = 0
    private var totalDistanceKm: Float = 0f
    private var currentSpeedKmh: Float = 0f
    private var stepLengthMeters: Float = StepSyncConfig.DEFAULT_STEP_LENGTH_CM / 100f
    private var currentUserId: String = ""

    private lateinit var sensorManager: SensorManager
    private var stepSensor: Sensor? = null

    private val handler = Handler(Looper.getMainLooper())
    private val updateRunnable = object : Runnable {
        override fun run() {
            if (isTracking) {
                updateTracking()
                handler.postDelayed(this, UPDATE_INTERVAL_MS)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        
        val app = application as? MyApplication
        currentUserId = app?.firebaseAuth?.currentUser?.uid ?: ""
        
        loadStepLengthFromSettings()
    }

    private fun loadStepLengthFromSettings() {
        val sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val userStepLengthCm = sharedPreferences.getInt(
            StepSyncConfig.KEY_STEP_LENGTH,
            StepSyncConfig.DEFAULT_STEP_LENGTH_CM
        )
        stepLengthMeters = userStepLengthCm / 100f
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_TRACKING -> startTracking()
            ACTION_STOP_TRACKING -> stopTracking()
            ACTION_PAUSE_TRACKING -> pauseTracking()
            ACTION_RESUME_TRACKING -> resumeTracking()
            ACTION_REQUEST_STATUS -> broadcastCurrentStatus()
            StepSyncConfig.ACTION_UNITS_CHANGED -> {
                loadStepLengthFromSettings()
            }
        }

        return START_STICKY
    }

    private fun startTracking() {
        if (!isTracking) {
            createNotificationChannel()
            val notification = createNotification()
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION or
                        android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH
                )
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
                )
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
            sensorManager.unregisterListener(this)

            handler.removeCallbacks(updateRunnable)

            isTracking = false

            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()

            broadcastTrackingStatus()
        }
    }

    private fun pauseTracking() {
        if (isTracking && !isPaused) {
            sensorManager.unregisterListener(this)
            handler.removeCallbacks(updateRunnable)
            pausedTimeMillis = System.currentTimeMillis()
            isPaused = true

            updateNotification()
            broadcastTrackingStatus()
        }
    }

    private fun resumeTracking() {
        if (isTracking && isPaused) {
            val pauseDuration = System.currentTimeMillis() - pausedTimeMillis
            totalPausedMillis += pauseDuration

            stepSensor?.let {
                sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
            }

            isPaused = false
            handler.post(updateRunnable)
            updateNotification()
            broadcastTrackingStatus()
        }
    }

    private fun updateTracking() {
        if (!isPaused) {
            elapsedTimeSeconds = TimeUnit.MILLISECONDS.toSeconds(
                System.currentTimeMillis() - startTimeMillis - totalPausedMillis
            )

            if (currentSteps > 0 && elapsedTimeSeconds > 0) {
                currentSpeedKmh = (totalDistanceKm / (elapsedTimeSeconds / 3600.0f))
            }

            broadcastTimeUpdate()
            broadcastDistanceUpdate()
            broadcastSpeedUpdate()
            broadcastStepsUpdate()

            updateNotification()
        }
    }

    private fun broadcastCurrentStatus() {
        broadcastTrackingStatus()
        broadcastTimeUpdate()
        broadcastDistanceUpdate()
        broadcastSpeedUpdate()
        broadcastStepsUpdate()
    }

    private fun broadcastTrackingStatus() {
        saveTrackingStatus()

        val intent = Intent(ACTION_TRACKING_STATUS).apply {
            putExtra(EXTRA_IS_TRACKING, isTracking)
            putExtra(EXTRA_IS_PAUSED, isPaused)
            putExtra(EXTRA_USER_ID, currentUserId)
        }
        sendStepSyncBroadcast(intent)
    }

    private fun saveTrackingStatus() {
        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(PREF_IS_TRACKING, isTracking)
            .putBoolean(PREF_IS_PAUSED, isPaused)
            .apply()
    }

    private fun broadcastTimeUpdate() {
        val intent = Intent(ACTION_TIME_UPDATE).apply {
            putExtra(EXTRA_TIME, elapsedTimeSeconds)
            putExtra(EXTRA_USER_ID, currentUserId)
        }
        sendStepSyncBroadcast(intent)
    }

    private fun broadcastDistanceUpdate() {
        val intent = Intent(ACTION_DISTANCE_UPDATE).apply {
            putExtra(EXTRA_DISTANCE, totalDistanceKm)
            putExtra(EXTRA_USER_ID, currentUserId)
        }
        sendStepSyncBroadcast(intent)
    }

    private fun broadcastSpeedUpdate() {
        val intent = Intent(ACTION_SPEED_UPDATE).apply {
            putExtra(EXTRA_SPEED, currentSpeedKmh)
            putExtra(EXTRA_USER_ID, currentUserId)
        }
        sendStepSyncBroadcast(intent)
    }

    private fun broadcastStepsUpdate() {
        val intent = Intent(ACTION_STEPS_UPDATE).apply {
            putExtra(EXTRA_STEPS, currentSteps)
            putExtra(EXTRA_USER_ID, currentUserId)
        }
        sendStepSyncBroadcast(intent)
    }

    private fun sendStepSyncBroadcast(intent: Intent) {
        intent.setPackage(packageName)
        sendBroadcast(intent)
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

            if (additionalSteps > 0) {
                val additionalDistanceMeters = additionalSteps * stepLengthMeters
                totalDistanceKm += (additionalDistanceMeters / 1000f)
                broadcastDistanceUpdate()
                broadcastSpeedUpdate()
                broadcastStepsUpdate()
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
        val sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val unitPreference = sharedPreferences.getString(
            StepSyncConfig.KEY_UNITS,
            StepSyncConfig.DEFAULT_UNITS
        )
        val distanceUnit = TrackingFormatters.distanceUnitCode(unitPreference)
        val distanceText = TrackingFormatters.formatDistance(totalDistanceKm, distanceUnit)
        val status = if (isPaused) "PAUSED" else "Recording"
        
        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("StepSync - $status")
            .setContentText("Distance: $distanceText")
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
