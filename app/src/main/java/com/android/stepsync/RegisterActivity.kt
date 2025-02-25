package com.android.stepsync

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class RegisterActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val edit_username = findViewById<EditText>(R.id.edit_username)
        val edit_password = findViewById<EditText>(R.id.edit_password)
        val edit_confirmpassword = findViewById<EditText>(R.id.edit_confirmpassword)
        val button_register = findViewById<Button>(R.id.button_register)
        val button_login = findViewById<Button>(R.id.button_login)
        val intent_login = Intent(this, LoginActivity::class.java)

        button_register.setOnClickListener {
            val username = edit_username.text
            val password = edit_password.text
            val confirmpassword = edit_confirmpassword.text

            if(username.isNullOrEmpty() || password.isNullOrEmpty() || confirmpassword.isNullOrEmpty()){
                Toast.makeText(this , "Fields must not be left blank", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            } else {
                if(!password.toString().equals(confirmpassword.toString())){
                    Toast.makeText(this , "Passwords do not match", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                } else {
                    Log.e("CSIT284", "Account created")
                    startActivity(intent_login)
                    finish()
                }
            }

        }

        button_login.setOnClickListener {
            startActivity(intent_login)
        }
    }
}