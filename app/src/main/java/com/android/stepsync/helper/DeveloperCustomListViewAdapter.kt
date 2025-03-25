package com.android.stepsync.helper

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import com.android.stepsync.R
import com.android.stepsync.data.Developer

class DeveloperCustomListViewAdapter(
    private val context: Context,
    private val developerList: List<Developer>,
    private val onClick: (Developer) -> Unit,
    private val onLongClick: (Developer) -> Unit
): BaseAdapter() {
    override fun getCount(): Int = developerList.size

    override fun getItem(position: Int): Any = developerList[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.customlistview_developer, parent, false)

        val imageview_picture = view.findViewById<ImageView>(R.id.imageview_picture)
        val fullname = view.findViewById<TextView>(R.id.textview_fullname)
        val email = view.findViewById<TextView>(R.id.textview_email)
        val developer = developerList[position]

        imageview_picture.setImageResource(developer.photoSrc)
        fullname.setText("${developer.lastname}, ${developer.firstname} ${developer.middlename}")
        email.setText("${developer.email}")

        view.setOnClickListener{
            onClick(developer)
        }

        view.setOnLongClickListener{
            onLongClick(developer)
            true
        }

        return view
    }
}