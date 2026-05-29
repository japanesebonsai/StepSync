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
import com.android.stepsync.utils.StepSyncConfig
import com.android.stepsync.utils.TrackingFormatters
import java.text.NumberFormat
import java.util.Locale

class ActivitiesAdapter(
    private val context: Context,
    private val items: List<ActivityRecord>
) : RecyclerView.Adapter<ActivitiesAdapter.ViewHolder>() {

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences(StepSyncConfig.PREFS_NAME, Context.MODE_PRIVATE)
    private val distanceUnit: String = TrackingFormatters.distanceUnitCode(
        sharedPreferences.getString(StepSyncConfig.KEY_UNITS, StepSyncConfig.DEFAULT_UNITS)
    )
    private val stepLengthCm: Int = sharedPreferences.getInt(
        StepSyncConfig.KEY_STEP_LENGTH,
        StepSyncConfig.DEFAULT_STEP_LENGTH_CM
    )
    private val stepsPerKm: Int = TrackingFormatters.stepsPerKm(stepLengthCm)
    private val stepFormatter: NumberFormat = NumberFormat.getNumberInstance(Locale.getDefault())

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

        holder.timeTv.text = TrackingFormatters.formatCompactDuration(act.durationSeconds)
        holder.distanceTv.text = TrackingFormatters.formatDistance(act.distanceKm, distanceUnit)
        holder.paceTv.text = TrackingFormatters.formatPace(
            act.distanceKm,
            act.durationSeconds,
            sharedPreferences.getString(StepSyncConfig.KEY_UNITS, StepSyncConfig.DEFAULT_UNITS)
        )

        val steps = if (act.steps > 0) act.steps else (act.distanceKm * stepsPerKm).toInt()
        holder.stepsTv.text = stepFormatter.format(steps)
    }
}
