package com.android.stepsync.activity

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.android.stepsync.R
import com.android.stepsync.app.MyApplication
import com.android.stepsync.utils.isNotValid
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class ProfileFragment : Fragment(R.layout.fragment_profile) {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val button_edit = view?.findViewById<Button>(R.id.button_edit)

        button_edit?.setOnClickListener {
            showEditDialog()
        }
    }

    private fun showEditDialog() {
        val builder = MaterialAlertDialogBuilder(requireActivity())
        builder.setTitle("Edit")

        val dialogView = layoutInflater.inflate(R.layout.dialog_editprofile,null)
        val edit_username = dialogView.findViewById<EditText>(R.id.edit_username)
        val edit_password = dialogView.findViewById<EditText>(R.id.edit_password)
        builder.setPositiveButton("Save"){ _, _ ->
            if(!edit_username.isNotValid() && !edit_password.isNotValid()){

            }

        }

        builder.setView(dialogView)
        builder.show()
    }
}