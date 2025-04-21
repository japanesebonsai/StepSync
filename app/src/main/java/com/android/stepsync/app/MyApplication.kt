package com.android.stepsync.app

import android.app.Application
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class MyApplication : Application() {
    var username : String = "admin"
    var email : String = "admin"
    var password : String = "admin"
    lateinit var database: FirebaseDatabase
    lateinit var auth : FirebaseAuth

    override fun onCreate() {
        super.onCreate()
    }
}