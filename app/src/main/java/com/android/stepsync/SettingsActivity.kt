package com.android.stepsync

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val text_developer = findViewById<TextView>(R.id.text_developer)
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigationView)

        text_developer.setOnClickListener{
            startActivity(Intent(this, DeveloperActivity::class.java))
        }

        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_dashboard -> {
                    startActivity(Intent(this, LandingActivity::class.java))
                    true
                }
                R.id.navigation_profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java))
                    true
                }
                R.id.navigation_settings -> true
                else -> false
            }
        }

        bottomNavigationView.selectedItemId = R.id.navigation_settings

    }
}