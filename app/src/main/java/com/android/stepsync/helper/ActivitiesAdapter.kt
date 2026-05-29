package com.android.stepsync.helper

import android.content.Context
import android.content.SharedPreferences
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.android.stepsync.R
import com.android.stepsync.data.ActivityRecord

class ActivitiesAdapter(
    private val context: Context,
    private val items: List<ActivityRecord>
) : RecyclerView.Adapter<ActivitiesAdapter.ViewHolder>() {

    private val sharedPreferences: SharedPreferences = context.getSharedPreferences("step_sync_prefs", Context.MODE_PRIVATE)
    private val distanceUnit: String = if (sharedPreferences.getString("units", "Kilometers (km)") == "Miles (mi)") "mi" else "km"
    private val stepLengthCm: Int = sharedPreferences.getInt("step_length", 65) // Default 65cm
    private val stepsPerKm: Int = (100000 / stepLengthCm) // 100,000 cm per km / step length in cm

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val timeTv: TextView = view.findViewById(R.id.text_time)
        val distanceTv: TextView  = view.findViewById(R.id.text_distance)
        val paceTv: TextView     = view.findViewById(R.id.text_pace)
        val stepsTv: TextView     = view.findViewById(R.id.text_steps)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_activity, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val act = items[position]

        val hours = act.durationSeconds / 3600
        val minutes = (act.durationSeconds % 3600) / 60
        val seconds = act.durationSeconds % 60
        
        // Format time to include seconds, especially for short durations
        val timeText = when {
            hours > 0 -> "${hours}h ${minutes}m ${seconds}s"
            minutes > 0 -> "${minutes}m ${seconds}s"
            else -> "${seconds}s"
        }
        holder.timeTv.text = timeText

        // Convert distance based on selected unit
        val distance = if (distanceUnit == "mi") {
            act.distanceKm * 0.621371f // km to miles
        } else {
            act.distanceKm // Already in km
        }
        holder.distanceTv.text = "${"%.2f".format(distance)} $distanceUnit"

        // Pace display should also reflect the unit
        val paceUnit = if (distanceUnit == "mi") "/mi" else "/km"
        val paceMinPerUnit = if (act.avgSpeedKmh > 0f) {
            if (distanceUnit == "mi") {
                60.0 / (act.avgSpeedKmh * 0.621371) // min/mi
            } else {
                60.0 / act.avgSpeedKmh // min/km
            }
        } else 0.0
        
        val paceMin = paceMinPerUnit.toInt()
        val paceSec = ((paceMinPerUnit - paceMin) * 60).toInt()
        holder.paceTv.text = "%d:%02d$paceUnit".format(paceMin, paceSec)

        val steps = if (act.steps > 0) act.steps else (act.distanceKm * stepsPerKm).toInt()
        holder.stepsTv.text = "$steps"
    }
}
