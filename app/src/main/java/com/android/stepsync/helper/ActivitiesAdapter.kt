import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.android.stepsync.R
import com.android.stepsync.data.ActivityRecord

class ActivitiesAdapter(
    private val items: List<ActivityRecord>
) : RecyclerView.Adapter<ActivitiesAdapter.ViewHolder>() {

    companion object {

        private const val STEPS_PER_KM = 1312
    }

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

        val hours   = act.durationSeconds / 3600
        val minutes = (act.durationSeconds % 3600) / 60
        holder.timeTv.text = "${hours}h ${minutes}m"

        holder.distanceTv.text = "${"%.2f".format(act.distanceKm)} km"

        val paceMinPerKm = if (act.avgSpeedKmh > 0f) 60.0 / act.avgSpeedKmh else 0.0
        val paceMin = paceMinPerKm.toInt()
        val paceSec = ((paceMinPerKm - paceMin) * 60).toInt()
        holder.paceTv.text = "%d:%02d/km".format(paceMin, paceSec)

        val steps = (act.distanceKm * STEPS_PER_KM).toInt()
        holder.stepsTv.text = "$steps"
    }
}
