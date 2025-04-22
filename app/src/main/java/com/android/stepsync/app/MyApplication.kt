package com.android.stepsync.app

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class MyApplication : Application() {
    lateinit var firebaseAuth: FirebaseAuth
    lateinit var database: FirebaseDatabase

    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        firebaseAuth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance("https://stepsync-d21c1-default-rtdb.asia-southeast1.firebasedatabase.app/")
    }
}