package com.android.stepsync.activity

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.android.stepsync.R
import com.android.stepsync.app.MyApplication
import com.google.android.material.dialog.MaterialAlertDialogBuilder



class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val text_developer = findViewById<TextView>(R.id.text_developer)
        val layout_logout = findViewById<LinearLayout>(R.id.layout_logout)
        val button_back = findViewById<Button>(R.id.button_back)

        text_developer.setOnClickListener{
            startActivity(Intent(this, DeveloperActivity::class.java))
        }

        button_back.setOnClickListener {
            finish()
        }

        layout_logout.setOnClickListener{
            MaterialAlertDialogBuilder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setNegativeButton("Cancel") { dialog, which ->
                    dialog.cancel()
                }
                .setPositiveButton("Logout") { dialog, which ->
                    val app = application as MyApplication
                    app.firebaseAuth.signOut()

                    getSharedPreferences("login_prefs", MODE_PRIVATE).edit().clear().apply()

                    startActivity(Intent(this, LoginActivity::class.java))
                    finish()
                }
                .show()

        }
    }
}