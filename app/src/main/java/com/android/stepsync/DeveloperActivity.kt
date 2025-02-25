package com.android.stepsync

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class DeveloperActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_developer)

        val button_back = findViewById<Button>(R.id.button_back)
        button_back.setOnClickListener {
            finish()
        }
    }
}