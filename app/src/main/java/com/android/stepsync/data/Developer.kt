package com.android.stepsync.data

import com.android.stepsync.R

data class Developer(
    var firstname: String = "",
    var middlename : String = "",
    var lastname : String = "",
    var email : String = "",
    var photoSrc : Int = R.drawable.user_icon
)
