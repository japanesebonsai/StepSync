package com.android.stepsync.activity

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.stepsync.R
import com.android.stepsync.app.MyApplication
import com.android.stepsync.utils.isNotValid
import com.android.stepsync.utils.toast


class LoginActivity : AppCompatActivity() {
    private lateinit var prefs: SharedPreferences
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        prefs = getSharedPreferences("login_prefs", MODE_PRIVATE)

        val text_register = findViewById<TextView>(R.id.text_register)
        val button_login = findViewById<Button>(R.id.button_login)
        val edit_email = findViewById<EditText>(R.id.edit_email)
        val edit_password = findViewById<EditText>(R.id.edit_password)
        val checkRemember = findViewById<CheckBox>(R.id.checkbox_rememberme)

        if (prefs.getBoolean("remember", false)) {
            prefs.getString("email", null)?.let { savedEmail ->
                prefs.getString("password", null)?.let { savedPassword ->
                    if (savedEmail.isNotEmpty() && savedPassword.isNotEmpty()) {
                        loginUser(savedEmail, savedPassword)
                        return
                    }
                }
            }
        }

        text_register.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        button_login.setOnClickListener {
            val email = edit_email.text.toString()
            val password = edit_password.text.toString()

            if (edit_email.isNotValid() || edit_password.isNotValid()) {
                toast("Email and password must not be empty")
                return@setOnClickListener
            }

            if (checkRemember.isChecked) {
                prefs.edit()
                    .putBoolean("remember", true)
                    .putString("email", email)
                    .putString("password", password)
                    .apply()
            } else {
                prefs.edit().clear().apply()
            }

            loginUser(email, password)
        }
    }

    private fun loginUser(email: String, password: String) {
        val auth = (application as MyApplication).firebaseAuth

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    startActivity(Intent(this, DashboardActivity::class.java))
                    finish()
                } else {
                    Toast.makeText(this, "Authentication failed: ${task.exception?.message}",
                        Toast.LENGTH_SHORT).show()
                }
            }
    }
}