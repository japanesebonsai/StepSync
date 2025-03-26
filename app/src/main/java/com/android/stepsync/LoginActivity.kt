package com.android.stepsync

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.stepsync.app.MyApplication
import com.android.stepsync.utils.isNotValid
import com.android.stepsync.utils.toast

class LoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val text_register = findViewById<TextView>(R.id.text_register)
        val button_login = findViewById<Button>(R.id.button_login)
        val edit_username = findViewById<EditText>(R.id.edit_username)
        val edit_password = findViewById<EditText>(R.id.edit_password)

        val sharedPref = getSharedPreferences("myPref", Context.MODE_PRIVATE)
        val editor = sharedPref.edit()

        text_register.setOnClickListener {
            Log.e("CSIT284", "Proceeding to register page")
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        button_login.setOnClickListener {
            val username = edit_username.text.toString()
            val password = edit_password.text.toString()

            if (edit_username.isNotValid() || edit_password.isNotValid()) {
                toast("Username and password must not be left empty")
                return@setOnClickListener
            }
            val app = application as MyApplication
            if(app.username != username || app.password != password){
                toast("Invalid username or password")
                return@setOnClickListener
            }

            Log.e("CSIT284", "Proceeding to landing page")
            startActivity(Intent(this, LandingActivity::class.java))
        }

    }
}