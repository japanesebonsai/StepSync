package com.android.stepsync

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import com.android.stepsync.data.Developer
import com.android.stepsync.helper.DeveloperCustomListViewAdapter

class DeveloperActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_developer)

        val listview_developer = findViewById<ListView>(R.id.listview_developer)

        val developerList = listOf(
            Developer("John Jacob", "Matildo", "Muli", "johnjacob.muli@cit.edu", R.drawable.user_icon),
            Developer("Primo Christian", "", "Montejo", "primochristian.montejo@cit.edu", R.drawable.user_icon)
        )

        val adapter = DeveloperCustomListViewAdapter(this, developerList)
        listview_developer.adapter = adapter

        val button_back = findViewById<Button>(R.id.button_back)

        button_back.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }
}