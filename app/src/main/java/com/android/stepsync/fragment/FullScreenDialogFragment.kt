package com.android.stepsync.fragment

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.android.stepsync.R
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener


class FullScreenDialogFragment : DialogFragment() {
    private lateinit var imageViewEditPicture1: ImageView
    private lateinit var imageViewEditPicture2: ImageView
    private lateinit var imageViewEditPicture3: ImageView
    private lateinit var imageViewEditPicture4: ImageView
    private lateinit var editUsername: TextInputEditText
    private lateinit var editPassword: TextInputEditText
    private lateinit var buttonSave: Button
    private lateinit var cardViewEditPicture1: MaterialCardView
    private lateinit var cardViewEditPicture2: MaterialCardView
    private lateinit var cardViewEditPicture3: MaterialCardView
    private lateinit var cardViewEditPicture4: MaterialCardView
    private lateinit var toolbar: MaterialToolbar
    private lateinit var usernameLayout: TextInputLayout
    private lateinit var passwordLayout: TextInputLayout

    private var selectedProfilePicture: Int = R.drawable.profile1_icon
    private var originalUsername: String = ""
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {

        return inflater.inflate(R.layout.dialog_editprofile, container, false)
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        imageViewEditPicture1 = view.findViewById(R.id.imageview_editpicture1)
        imageViewEditPicture2 = view.findViewById(R.id.imageview_editpicture2)
        imageViewEditPicture3 = view.findViewById(R.id.imageview_editpicture3)
        imageViewEditPicture4 = view.findViewById(R.id.imageview_editpicture4)
        editUsername = view.findViewById(R.id.edit_username)
        editPassword = view.findViewById(R.id.edit_password)
        buttonSave = view.findViewById(R.id.buttonSave)
        toolbar = view.findViewById(R.id.toolbar)
        cardViewEditPicture1 = view.findViewById(R.id.cardview_editpicture1)
        cardViewEditPicture2 = view.findViewById(R.id.cardview_editpicture2)
        cardViewEditPicture3 = view.findViewById(R.id.cardview_editpicture3)
        cardViewEditPicture4 = view.findViewById(R.id.cardview_editpicture4)
        usernameLayout = view.findViewById<TextInputLayout>(R.id.layout_username)
        passwordLayout = view.findViewById<TextInputLayout>(R.id.layout_password)

        toolbar.setNavigationOnClickListener {
            dismiss()
        }

        setProfilePictureSelection()

        FirebaseAuth.getInstance().currentUser?.uid?.let { uid ->
            FirebaseDatabase.getInstance()
                .getReference("users/$uid")
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        originalUsername = snapshot.child("username").getValue(String::class.java).orEmpty()
                        editUsername.setText(originalUsername)
                    }
                    override fun onCancelled(error: DatabaseError) {}
                })
        }

        buttonSave.setOnClickListener {
            validateAndSaveProfile()
        }
    }

    private fun setProfilePictureSelection() {
        cardViewEditPicture1.setOnClickListener {
            selectProfilePicture(
                R.drawable.profile1_icon,
                cardViewEditPicture1
            )
        }
        cardViewEditPicture2.setOnClickListener {
            selectProfilePicture(
                R.drawable.profile2_icon,
                cardViewEditPicture2
            )
        }
        cardViewEditPicture3.setOnClickListener {
            selectProfilePicture(
                R.drawable.profile3_icon,
                cardViewEditPicture3
            )
        }
        cardViewEditPicture4.setOnClickListener {
            selectProfilePicture(
                R.drawable.profile4_icon,
                cardViewEditPicture4
            )
        }
    }

    private fun selectProfilePicture(pictureId: Int, cardView: MaterialCardView) {
        resetProfilePictureSelection()
        cardView.setStrokeColor(Color.parseColor("#5e17eb"))
        selectedProfilePicture = pictureId
    }

    private fun resetProfilePictureSelection() {
        cardViewEditPicture1.strokeColor = Color.TRANSPARENT
        cardViewEditPicture2.strokeColor = Color.TRANSPARENT
        cardViewEditPicture3.strokeColor = Color.TRANSPARENT
        cardViewEditPicture4.strokeColor = Color.TRANSPARENT
    }

    private fun validateAndSaveProfile() {
        val newUsername = editUsername.text.toString().trim()
        val newPassword = editPassword.text.toString().trim()

        if (newUsername.isEmpty()) {
            usernameLayout.error = "Username must not be empty"
            return
        }
        if (newUsername.length > 16) {
            usernameLayout.error = "Username must be 16 characters at most"
            return
        }
        if (newPassword.length < 8 && newPassword.isNotEmpty()) {
            passwordLayout.error = "Password must be at least 8 characters"
            return
        }

        val usernameChanged = newUsername != originalUsername
        if (usernameChanged) {
            checkUsernameChangeLimit { isAllowed ->
                if (isAllowed) saveProfile(newUsername, newPassword, usernameChanged)
                else usernameLayout.error = "Username can only be changed once a week"
            }
        } else {
            saveProfile(newUsername, newPassword, false)
        }
    }

    private fun checkUsernameChangeLimit(callback: (Boolean) -> Unit) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            Toast.makeText(requireContext(), "Not logged in", Toast.LENGTH_SHORT).show()
            callback(false)
            return
        }

        val dbRef = FirebaseDatabase.getInstance()
            .getReference("users")
            .child(currentUser.uid)

        dbRef.get()
            .addOnSuccessListener { snapshot ->
                val lastChange = snapshot.child("lastUsernameChange").getValue(Long::class.java) ?: 0L
                val weekInMillis = 7 * 24 * 60 * 60 * 1000L
                callback(System.currentTimeMillis() - lastChange >= weekInMillis)
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error checking limit: ${e.message}", Toast.LENGTH_SHORT).show()
                callback(false)
            }
    }


    private fun saveProfile(username: String, password: String, usernameChanged: Boolean) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        val userId = currentUser.uid
        val dbRef = FirebaseDatabase.getInstance().getReference("users").child(userId)

        val updates = mutableMapOf<String, Any>(
            "profilePicture" to selectedProfilePicture
        )

        if(password.isNotEmpty()){
            updates["password"] = password
        }

        if (usernameChanged) {
            updates["username"] = username
            updates["lastUsernameChange"] = ServerValue.TIMESTAMP
        }


        dbRef.updateChildren(updates)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Profile updated successfully", Toast.LENGTH_SHORT).show()
                dismiss()
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Failed to save profile: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }


}

