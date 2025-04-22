package com.android.stepsync.activity

import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.android.stepsync.R
import com.android.stepsync.app.MyApplication


class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val text_developer = findViewById<TextView>(R.id.text_developer)
        val layout_logout = findViewById<LinearLayout>(R.id.layout_logout)

        text_developer.setOnClickListener{
            startActivity(Intent(this, DeveloperActivity::class.java))
        }

        layout_logout.setOnClickListener{
            val app = application as MyApplication
            app.firebaseAuth.signOut()

            getSharedPreferences("login_prefs", MODE_PRIVATE).edit().clear().apply()

            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
}