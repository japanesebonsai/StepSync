package com.android.stepsync.activity

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ListView
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
            Developer("John Jacob", "Matildo", "Muli", "johnjacob.muli@cit.edu",
                R.drawable.profile2_icon
            ),
            Developer("Primo Christian", "", "Montejo", "primochristian.montejo@cit.edu",
                R.drawable.profile1_icon
            )
        )

        val adapter = DeveloperCustomListViewAdapter(
            this,
            developerList,
            onClick = { developer ->
                toast("${developer.firstname} was clicked")
            },
            onLongClick = { developer ->
                toast("${developer.firstname} was long clicked")
            })


        listview_developer.adapter = adapter

        val button_back = findViewById<Button>(R.id.button_back)

        button_back.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }
}