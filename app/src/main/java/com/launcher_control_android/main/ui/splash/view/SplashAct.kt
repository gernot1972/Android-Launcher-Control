package com.launcher_control_android.main.ui.splash.view

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.launcher_control_android.R
import com.launcher_control_android.helper.util.startActivity
import com.launcher_control_android.main.ui.home.view.HomeAct
import com.launcher_control_android.main.ui.home.view.LauncherControlAct

class SplashAct : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.act_splash)

//        startActivity(HomeAct::class.java)
        startActivity(LauncherControlAct::class.java)
        finish()
    }
}
//FFE1
//RSSI => -80.6 min to -71 max.
//peripheral vs central