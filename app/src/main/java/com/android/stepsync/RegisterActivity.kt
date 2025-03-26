package com.android.stepsync

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.stepsync.app.MyApplication
import com.android.stepsync.utils.toast

class RegisterActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val edit_username = findViewById<EditText>(R.id.edit_username)
        val edit_email = findViewById<EditText>(R.id.edit_email)
        val edit_password = findViewById<EditText>(R.id.edit_password)
        val edit_confirmpassword = findViewById<EditText>(R.id.edit_confirmpassword)
        val button_register = findViewById<Button>(R.id.button_register)
        val text_login = findViewById<TextView>(R.id.text_login)

        button_register.setOnClickListener {
            val username = edit_username.text
            val email = edit_email.text
            val password = edit_password.text
            val confirmpassword = edit_confirmpassword.text

            if(username.isNullOrEmpty() || password.isNullOrEmpty() || confirmpassword.isNullOrEmpty() || email.isNullOrEmpty()){
                toast("Fields must not be left blank")
                return@setOnClickListener
            } else {
                if(!password.toString().equals(confirmpassword.toString())){
                    toast("Passwords do not match")
                    return@setOnClickListener
                } else {
                    Log.e("CSIT284", "Account created")

                    val app = application as MyApplication
                    app.username = username.toString()
                    app.email = email.toString()
                    app.password = password.toString()

                    startActivity(Intent(this, LoginActivity::class.java))
                }
            }
        }

        text_login.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }
    }
}