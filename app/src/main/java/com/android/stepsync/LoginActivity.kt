package com.android.stepsync

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class LoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val text_register = findViewById<TextView>(R.id.text_register)
        val button_login = findViewById<Button>(R.id.button_login)
        val edit_username = findViewById<EditText>(R.id.edit_username)
        val edit_password = findViewById<EditText>(R.id.edit_password)

        text_register.setOnClickListener {
            Log.e("CSIT284", "Proceeding to register page")
            val intent_register = Intent(this, RegisterActivity::class.java)
            startActivity(intent_register)
        }

        button_login.setOnClickListener {
            val username = edit_username.text.toString()
            val password = edit_password.text.toString()

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Username and password must not be left empty", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            val intent_landing = Intent(this, LandingActivity::class.java)
            Log.e("CSIT284", "Proceeding to landing page")
            intent_landing.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent_landing)
            finish()
        }

    }
}