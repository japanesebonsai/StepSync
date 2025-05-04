package com.android.stepsync.activity

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.android.stepsync.R
import com.android.stepsync.data.Developer
import com.android.stepsync.helper.DeveloperCustomListViewAdapter
import com.android.stepsync.utils.toast

class DeveloperActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_developer)

        val listview_developer = findViewById<ListView>(R.id.listview_developer)

        val developerList = listOf(
            Developer(
                firstname = "John Jacob", 
                middlename = "Matildo", 
                lastname = "Muli", 
                email = "johnjacob.muli@cit.edu",
                photoSrc = R.drawable.developer_picture1,
                hobbies = "Reading, video games, and exploring internet culture and emerging technologies",
                bio = "A strong advocate for lifelong learning, with a deep interest in technology, literature, and digital innovation.",
                age = 20,
                personalInfo = "Aspiring computer scientist with a passion for continuous growth and a curious mindset toward solving real-world problems through code."
            ),
            Developer(
                firstname = "Primo Christian", 
                middlename = "", 
                lastname = "Montejo", 
                email = "primochristian.montejo@cit.edu",
                photoSrc = R.drawable.profile1_icon,
                hobbies = "Hobbies, Hobbies, Hobbies, Hobbies",
                bio = "gdfgasdgasdfsad fsaf asdfasd fsdafsadfsdafsdfsf asfasfsdf",
                age = 20,
                personalInfo = "dafsdgasdgasdgasdfasd asgasdf sadf asdfas dfasdfasdfasdfa fasf sdfsd"
            )
        )

        val adapter = DeveloperCustomListViewAdapter(
            this,
            developerList,
            onClick = { developer ->
                showDeveloperInfoDialog(developer)
            },
            onLongClick = { developer ->
                toast("${developer.firstname} was long clicked")
            })

        listview_developer.adapter = adapter

        val button_back = findViewById<Button>(R.id.button_back)

        button_back.setOnClickListener {
            finish()
        }
    }
    
    private fun showDeveloperInfoDialog(developer: Developer) {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_developer_info)
        
        // Set dialog width to match parent with some margin
        val window = dialog.window
        window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.9).toInt(),
            android.view.WindowManager.LayoutParams.WRAP_CONTENT
        )
        
        // Set up views
        val imageView = dialog.findViewById<ImageView>(R.id.dialog_developer_image)
        val nameTextView = dialog.findViewById<TextView>(R.id.dialog_developer_name)
        val emailTextView = dialog.findViewById<TextView>(R.id.dialog_developer_email)
        val bioTextView = dialog.findViewById<TextView>(R.id.dialog_developer_bio)
        val hobbiesTextView = dialog.findViewById<TextView>(R.id.dialog_developer_hobbies)
        val ageTextView = dialog.findViewById<TextView>(R.id.dialog_developer_age)
        val personalInfoTextView = dialog.findViewById<TextView>(R.id.dialog_developer_personal_info)
        val closeButton = dialog.findViewById<Button>(R.id.dialog_developer_close_button)
        
        // Populate data
        imageView.setImageResource(developer.photoSrc)
        nameTextView.text = "${developer.firstname} ${developer.middlename} ${developer.lastname}".trim()
        emailTextView.text = developer.email
        bioTextView.text = developer.bio
        hobbiesTextView.text = developer.hobbies
        ageTextView.text = developer.age.toString()
        personalInfoTextView.text = developer.personalInfo
        
        // Set up close button
        closeButton.setOnClickListener {
            dialog.dismiss()
        }
        
        // Show dialog
        dialog.show()
    }
}