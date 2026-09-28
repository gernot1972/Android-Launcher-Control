package com.launcher_control_android

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class App : Application() {

    override fun onCreate() {
        super.onCreate()
        initTasks()
    }

    private fun initTasks() {

    }
}
