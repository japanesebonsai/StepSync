package com.android.stepsync.activity


import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.android.stepsync.R
import com.android.stepsync.viewmodel.StepCounterViewModel
import com.android.stepsync.service.StepCounterService


class RecordFragment : Fragment(R.layout.fragment_record), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var stepSensor: Sensor? = null
    private var isRecording = false
    private var initialSteps = 0f
    private var currentSteps = 0
    private var startTime = 0L

    private lateinit var timeTextView: TextView
    private lateinit var speedTextView: TextView
    private lateinit var distanceTextView: TextView
    private lateinit var recordButton: Button

    private lateinit var stepCounterViewModel: StepCounterViewModel

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize views using the provided view
        timeTextView = view.findViewById(R.id.text_time)
        speedTextView = view.findViewById(R.id.text_avgspeed)
        distanceTextView = view.findViewById(R.id.text_distance)
        recordButton = view.findViewById(R.id.button_record)

        // Initialize ViewModel
        stepCounterViewModel = ViewModelProvider(requireActivity()).get(StepCounterViewModel::class.java)

        // Sensor setup
        sensorManager = requireContext().getSystemService(SensorManager::class.java)
        stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

        if (stepSensor == null) {
            Toast.makeText(context, "No step counter sensor found!", Toast.LENGTH_LONG).show()
        }

        recordButton.setOnClickListener { toggleRecording() }

        stepCounterViewModel.latestTotalSteps.observe(viewLifecycleOwner) { totalSteps ->
            if (isRecording) {
                currentSteps = (totalSteps - initialSteps).toInt()
                updateUI()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun toggleRecording() {
        isRecording = !isRecording
        if (isRecording) {
            startRecording()
            recordButton.text = "STOP"
        } else {
            stopRecording()
            recordButton.text = "START"
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun startRecording() {
        initialSteps = stepCounterViewModel.latestTotalSteps
        startTime = System.currentTimeMillis()
        startForegroundService()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun startForegroundService() {
        val serviceIntent = Intent(requireContext(), StepCounterService::class.java)
        serviceIntent.putExtra("startTime", startTime)
        serviceIntent.putExtra("initialSteps", initialSteps)
        requireContext().startForegroundService(serviceIntent)
    }

    private fun stopRecording() {
        val serviceIntent = Intent(requireContext(), StepCounterService::class.java)
        requireContext().stopService(serviceIntent)
    }

    override fun onResume() {
        super.onResume()
        stepSensor?.let { sensor ->
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI)
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_STEP_COUNTER) {
            stepCounterViewModel.latestTotalSteps = event.values[0]
            if (isRecording) {
                currentSteps = (stepCounterViewModel.latestTotalSteps - initialSteps).toInt()
                updateUI()
            }
        }
    }

    private fun updateUI() {
        activity?.runOnUiThread {
            val currentTime = System.currentTimeMillis() - startTime
            updateTimeDisplay(currentTime)

            val distanceKm = (currentSteps * 0.7f) / 1000
            val avgSpeed = if (currentTime > 0) distanceKm / (currentTime / 3600000f) else 0f

            distanceTextView.text = "%.1f".format(distanceKm)
            speedTextView.text = "%.1f".format(avgSpeed)
        }
    }

    private fun updateTimeDisplay(millis: Long) {
        val hours = millis / 3600000
        val minutes = (millis % 3600000) / 60000
        val seconds = (millis % 60000) / 1000
        timeTextView.text = String.format("%d:%02d:%02d", hours, minutes, seconds)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}