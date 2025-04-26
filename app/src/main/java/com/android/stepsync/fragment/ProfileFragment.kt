package com.android.stepsync.fragment

import ActivitiesAdapter
import android.content.ContentValues.TAG
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.stepsync.R
import com.android.stepsync.app.MyApplication
import com.android.stepsync.data.ActivityRecord
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ProfileFragment : Fragment(R.layout.fragment_profile) {
    //TODO load recyclerview (to get data from recorded activities on the database)
    private lateinit var recycler: RecyclerView
    private lateinit var adapter: ActivitiesAdapter
    private val activityList = mutableListOf<ActivityRecord>()
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recycler = view.findViewById(R.id.recycler_activities)
        recycler.layoutManager = LinearLayoutManager(requireContext())
        adapter = ActivitiesAdapter(activityList)
        recycler.adapter = adapter

        loadActivitiesFromDatabase()

        val text_username = view.findViewById<TextView>(R.id.text_username)
        val text_created_at = view.findViewById<TextView>(R.id.text_created_at)
        val button_edit = view.findViewById<Button>(R.id.button_edit)

        val app = activity?.application as MyApplication
        val userId = app.firebaseAuth.currentUser?.uid ?: return

        app.database.getReference("users/$userId")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val username = snapshot.child("username").getValue(String::class.java)
                    val ts = snapshot.child("createdAt").getValue(Long::class.java) ?: 0L
                    text_username.text = username.orEmpty()

                    val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                    text_created_at.text = sdf.format(Date(ts))
                }
                override fun onCancelled(error: DatabaseError) {

                }
            })

        button_edit.setOnClickListener {
            //TODO implement using Full-Screen Dialog(DialogFragment)
        }
    }

    private fun loadActivitiesFromDatabase() {
        val user = (requireActivity().application as MyApplication)
            .firebaseAuth
            .currentUser
            ?: return

        val userId = user.uid

        val dbRef = (requireActivity().application as MyApplication)
            .database
            .getReference("user_activities/$userId")

        dbRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                activityList.clear()
                for (child in snapshot.children) {
                    child.getValue(ActivityRecord::class.java)
                        ?.let { activityList.add(it) }
                }
                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Failed to load activities: ${error.message}")
            }
        })
    }
}