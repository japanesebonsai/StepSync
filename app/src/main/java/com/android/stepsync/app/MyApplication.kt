package com.android.stepsync.app

import android.app.Application

class MyApplication : Application() {
    var username : String = "admin"
    var password : String = "123"

    override fun onCreate(){
        super.onCreate()
    }

}