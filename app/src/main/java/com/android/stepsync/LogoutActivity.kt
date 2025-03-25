package com.android.stepsync

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class LogoutActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_logout)

        val button_cancel = findViewById<Button>(R.id.button_cancel)
        val button_logout = findViewById<Button>(R.id.button_logout)

        button_cancel.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        button_logout.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }
    }
}