package com.android.stepsync.app

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class MyApplication : Application() {
    private val TAG = "StepSync"
    
    lateinit var firebaseAuth: FirebaseAuth
    lateinit var database: FirebaseDatabase

    override fun onCreate() {
        super.onCreate()

        applyThemeSetting()
        
        FirebaseApp.initializeApp(this)
        firebaseAuth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance("https://stepsync-d21c1-default-rtdb.asia-southeast1.firebasedatabase.app/")

        database.setPersistenceEnabled(true)

        verifyDatabaseConnectivity()
    }
    
    private fun applyThemeSetting() {
        val sharedPreferences = getSharedPreferences("step_sync_prefs", Context.MODE_PRIVATE)
        val themeSetting = sharedPreferences.getString("theme", "Light")

        when (themeSetting) {
            "Dark" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
        
        Log.d(TAG, "Applied theme setting: $themeSetting")
    }
    
    private fun verifyDatabaseConnectivity() {
        val connectedRef = database.getReference(".info/connected")
        connectedRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val connected = snapshot.getValue(Boolean::class.java) ?: false
                if (connected) {
                    Log.d(TAG, "Connected to Firebase Database")
                } else {
                    Log.w(TAG, "Not connected to Firebase Database")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Database connection listener was cancelled: ${error.message}")
            }
        })

        val currentUser = firebaseAuth.currentUser
        if (currentUser != null) {
            Log.d(TAG, "User is authenticated: ${currentUser.uid}")

            database.getReference("user_activities/${currentUser.uid}")
                .limitToFirst(1)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        Log.d(TAG, "Successfully read from user_activities/${currentUser.uid}, snapshot exists: ${snapshot.exists()}, childCount: ${snapshot.childrenCount}")
                    }
                    
                    override fun onCancelled(error: DatabaseError) {
                        Log.e(TAG, "Database permission error: ${error.message}")
                    }
                })
        } else {
            Log.d(TAG, "No user is currently authenticated")
        }
    }
}