package com.android.stepsync.app

import android.app.Application
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import com.android.stepsync.utils.StepSyncConfig
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class MyApplication : Application() {
    lateinit var firebaseAuth: FirebaseAuth
    lateinit var database: FirebaseDatabase

    override fun onCreate() {
        super.onCreate()

        applyThemeSetting()
        
        FirebaseApp.initializeApp(this)
        firebaseAuth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance(StepSyncConfig.DATABASE_URL)

        database.setPersistenceEnabled(true)
    }
    
    private fun applyThemeSetting() {
        val sharedPreferences = getSharedPreferences(StepSyncConfig.PREFS_NAME, Context.MODE_PRIVATE)
        val themeSetting = sharedPreferences.getString(
            StepSyncConfig.KEY_THEME,
            StepSyncConfig.DEFAULT_THEME
        )

        when (themeSetting) {
            "Dark" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }
}
