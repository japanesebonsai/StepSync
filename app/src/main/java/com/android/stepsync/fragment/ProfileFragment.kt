package com.android.stepsync.activity

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.android.stepsync.R
import com.android.stepsync.app.MyApplication
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ProfileFragment : Fragment(R.layout.fragment_profile) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val text_username = view.findViewById<TextView>(R.id.text_username)
        val text_created_at = view.findViewById<TextView>(R.id.text_created_at)

        val app = activity?.application as MyApplication
        val userId = app.firebaseAuth.currentUser?.uid ?: return

        app.database.getReference("users/$userId")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val username = snapshot.child("username").getValue(String::class.java)
                    val ts = snapshot.child("createdAt").getValue(Long::class.java) ?: 0L
                    text_username.text = username.orEmpty()

                    val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                    text_created_at.text = sdf.format(Date(ts))
                }
                override fun onCancelled(error: DatabaseError) {

                }
            })
    }
}