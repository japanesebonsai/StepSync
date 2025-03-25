package com.android.stepsync

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.android.stepsync.app.MyApplication
import com.google.android.material.bottomnavigation.BottomNavigationView

class ProfileActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        val text_signout = findViewById<TextView>(R.id.text_signout)
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigationView)

        val app = application as MyApplication
        val informationList = listOf(app.username, "johndoe@example.com")
        val arrayAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            informationList
        )

        val listview = findViewById<ListView>(R.id.listview_profile)
        listview.adapter = arrayAdapter

        text_signout.setOnClickListener(){
            startActivity(Intent(this, LogoutActivity::class.java))
        }

        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_dashboard -> {
                    startActivity(Intent(this, LandingActivity::class.java))
                    true
                }
                R.id.navigation_profile -> true
                R.id.navigation_settings -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                    true
                }
                else -> false
            }
        }

        bottomNavigationView.selectedItemId = R.id.navigation_profile

    }
}