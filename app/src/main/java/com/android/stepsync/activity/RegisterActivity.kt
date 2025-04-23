package com.android.stepsync.activity

import android.content.Intent
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.stepsync.R
import com.android.stepsync.app.MyApplication
import com.android.stepsync.utils.toast
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener

class RegisterActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val usernameLayout = findViewById<TextInputLayout>(R.id.layout_username)
        val emailLayout = findViewById<TextInputLayout>(R.id.layout_email)
        val passwordLayout = findViewById<TextInputLayout>(R.id.layout_password)
        val confirmLayout = findViewById<TextInputLayout>(R.id.layout_confirmpassword)

        val editUsername = findViewById<TextInputEditText>(R.id.edit_username)
        val editEmail = findViewById<TextInputEditText>(R.id.edit_email)
        val editPassword = findViewById<TextInputEditText>(R.id.edit_password)
        val editConfirm = findViewById<TextInputEditText>(R.id.edit_confirmpassword)

        val buttonRegister = findViewById<Button>(R.id.button_register)
        val textLogin = findViewById<TextView>(R.id.text_login)

        buttonRegister.setOnClickListener {
            usernameLayout.error = null; emailLayout.error = null
            passwordLayout.error = null; confirmLayout.error = null

            val username = editUsername.text.toString().trim()
            val email = editEmail.text.toString().trim()
            val password = editPassword.text.toString().trim()
            val confirm = editConfirm.text.toString().trim()

            if (username.isEmpty()) {
                usernameLayout.error = "Username must not be empty"
                return@setOnClickListener
            }

            // Email format validation
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                emailLayout.error = "Enter a valid email address"
                return@setOnClickListener
            }

            // Password length validation
            if (password.length < 8) {
                passwordLayout.error = "Password must be at least 8 characters"
                return@setOnClickListener
            }

            // Password match validation
            if (password != confirm) {
                confirmLayout.error = "Passwords do not match"
                return@setOnClickListener
            }

            // Unique username check in Realtime Database
            val dbRef = (application as MyApplication).database.getReference("users")
            dbRef.orderByChild("username").equalTo(username)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            usernameLayout.error = "Username is already taken"
                        } else {
                            usernameLayout.error = null
                            registerUser(email, password, username)
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        toast("Database error: ${error.message}")
                    }
                })
        }

        textLogin.setOnClickListener {
            finish()
        }
    }


    private fun registerUser(email: String, password: String, username: String) {
        val auth = (application as MyApplication).firebaseAuth

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    user?.let {
                        val userData = mapOf(
                            "username" to username,
                            "email" to email,
                            "createdAt" to ServerValue.TIMESTAMP
                        )

                        (application as MyApplication)
                            .database
                            .getReference("users/${user.uid}")
                            .setValue(userData)
                            .addOnSuccessListener {
                                startActivity(Intent(this, LoginActivity::class.java))
                                finish()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(
                                    this,
                                    "Database error: ${e.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                    }
                } else {
                    Toast.makeText(
                        this,
                        "Registration failed: ${task.exception?.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }
}