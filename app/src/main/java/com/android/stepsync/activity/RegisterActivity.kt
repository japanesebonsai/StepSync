package com.android.stepsync.activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.android.stepsync.R
import com.android.stepsync.app.MyApplication
import com.android.stepsync.utils.isNotValid
import com.android.stepsync.utils.toast
import com.google.android.material.textfield.TextInputEditText

class RegisterActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val edit_username = findViewById<EditText>(R.id.edit_username)
        val edit_email = findViewById<EditText>(R.id.edit_email)
        val edit_password = findViewById<TextInputEditText>(R.id.edit_password)
        val edit_confirmpassword = findViewById<TextInputEditText>(R.id.edit_confirmpassword)
        val button_register = findViewById<Button>(R.id.button_register)
        val text_login = findViewById<TextView>(R.id.text_login)

        button_register.setOnClickListener {
            val username = edit_username.text
            val email = edit_email.text
            val password = edit_password.text
            val confirmpassword = edit_confirmpassword.text

            if(edit_username.isNotValid() || edit_password.isNotValid() || edit_confirmpassword.isNotValid()|| edit_email.isNotValid()){
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