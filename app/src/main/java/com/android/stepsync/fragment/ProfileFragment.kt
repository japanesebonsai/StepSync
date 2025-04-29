package com.android.stepsync.fragment

import ActivitiesAdapter
import android.content.ContentValues.TAG
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.stepsync.R
import com.android.stepsync.app.MyApplication
import com.android.stepsync.data.ActivityRecord
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ProfileFragment : Fragment(R.layout.fragment_profile) {
    private lateinit var recycler: RecyclerView
    private lateinit var adapter: ActivitiesAdapter
    private val activityList = mutableListOf<ActivityRecord>()
    private lateinit var userRef: DatabaseReference
    private lateinit var userListener: ValueEventListener
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recycler = view.findViewById(R.id.recycler_activities)
        recycler.layoutManager = LinearLayoutManager(requireContext())
        adapter = ActivitiesAdapter(requireContext(), activityList)
        recycler.adapter = adapter

        loadActivitiesFromDatabase()

        val text_username = view.findViewById<TextView>(R.id.text_username)
        val text_created_at = view.findViewById<TextView>(R.id.text_created_at)
        val button_edit = view.findViewById<Button>(R.id.button_edit)
        val imageview_picture = view.findViewById<ImageView>(R.id.imageview_picture)

        val app = activity?.application as MyApplication
        val userId = app.firebaseAuth.currentUser?.uid ?: return

        userRef = app.database.getReference("users/$userId")
        userListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val username = snapshot.child("username").getValue(String::class.java).orEmpty()
                val ts = snapshot.child("createdAt").getValue(Long::class.java) ?: 0L
                val profilePicResId = snapshot.child("profilePicture").getValue(Int::class.java)

                text_username.text = username
                val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                text_created_at.text = sdf.format(Date(ts))

                profilePicResId?.let {
                    imageview_picture.setImageResource(it)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Failed to load profile data: ${error.message}")
            }
        }
        userRef.addValueEventListener(userListener)

        button_edit.setOnClickListener {
            val dialog = FullScreenDialogFragment()
            dialog.show(childFragmentManager, "FullScreenDialog")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (::userRef.isInitialized && ::userListener.isInitialized) {
            userRef.removeEventListener(userListener)
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