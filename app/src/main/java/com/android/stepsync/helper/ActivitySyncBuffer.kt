package com.android.stepsync.helper

import android.content.Context
import com.android.stepsync.data.ActivityRecord
import com.android.stepsync.utils.StepSyncConfig
import com.google.firebase.database.DatabaseReference
import org.json.JSONArray
import org.json.JSONObject

class ActivitySyncBuffer(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        StepSyncConfig.PREFS_NAME,
        Context.MODE_PRIVATE
    )

    fun enqueue(record: ActivityRecord) {
        val pending = readPending().toMutableList()
        pending.removeAll { it.id == record.id }
        pending.add(record)
        writePending(pending)
    }

    fun flush(
        databaseRoot: DatabaseReference,
        userId: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val pending = readPending()
        if (pending.isEmpty()) {
            onSuccess()
            return
        }

        val updates = pending.associate { record ->
            "user_activities/$userId/${record.id}" to record
        }

        databaseRoot.updateChildren(updates)
            .addOnSuccessListener {
                writePending(emptyList())
                onSuccess()
            }
            .addOnFailureListener { error ->
                onFailure(error)
            }
    }

    private fun readPending(): List<ActivityRecord> {
        val rawJson = preferences.getString(KEY_PENDING_ACTIVITY_RECORDS, "[]") ?: "[]"
        val records = mutableListOf<ActivityRecord>()
        val array = JSONArray(rawJson)

        for (index in 0 until array.length()) {
            val item = array.getJSONObject(index)
            records.add(
                ActivityRecord(
                    id = item.getString("id"),
                    userId = item.getString("userId"),
                    timestamp = item.getLong("timestamp"),
                    durationSeconds = item.getLong("durationSeconds"),
                    distanceKm = item.getDouble("distanceKm").toFloat(),
                    avgSpeedKmh = item.getDouble("avgSpeedKmh").toFloat(),
                    steps = item.getInt("steps")
                )
            )
        }

        return records
    }

    private fun writePending(records: List<ActivityRecord>) {
        val array = JSONArray()
        records.forEach { record ->
            array.put(
                JSONObject()
                    .put("id", record.id)
                    .put("userId", record.userId)
                    .put("timestamp", record.timestamp)
                    .put("durationSeconds", record.durationSeconds)
                    .put("distanceKm", record.distanceKm.toDouble())
                    .put("avgSpeedKmh", record.avgSpeedKmh.toDouble())
                    .put("steps", record.steps)
            )
        }

        preferences.edit()
            .putString(KEY_PENDING_ACTIVITY_RECORDS, array.toString())
            .putInt(KEY_PENDING_ACTIVITY_COUNT, records.size)
            .apply()
    }

    companion object {
        const val KEY_PENDING_ACTIVITY_RECORDS = "pending_activity_records"
        const val KEY_PENDING_ACTIVITY_COUNT = "pending_activity_count"
    }
}
