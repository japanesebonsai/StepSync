package com.android.stepsync.activity

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.android.stepsync.R


class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val text_developer = findViewById<TextView>(R.id.text_developer)

        text_developer?.setOnClickListener{
            startActivity(Intent(this, DeveloperActivity::class.java))
        }
    }
}