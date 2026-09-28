package com.launcher_control_android.helper.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.Settings
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject

class NetworkUtil
@Inject
constructor(@ApplicationContext var context: Context) {

    fun isNetworkAvailable(): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        @Suppress("MissingPermission")
        val activeNetworkInfo = connectivityManager.activeNetworkInfo
        return activeNetworkInfo != null && activeNetworkInfo.isConnected
    }

    fun isInFlightMode(): Boolean {
        return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1)
            Settings.System.getInt(
                context.contentResolver,
                Settings.System.AIRPLANE_MODE_ON,
                0
            ) != 0
        else
            Settings.Global.getInt(
                context.contentResolver,
                Settings.Global.AIRPLANE_MODE_ON,
                0
            ) != 0
    }

    fun changeWifiState(shouldWifiBeEnabled: Boolean) {
        val wifiManager =
            context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        wifiManager.isWifiEnabled = shouldWifiBeEnabled
    }

}

/**
 * workaround to fix injecting [NetworkUtil] in normal classes
 *
 * in hilt, field injection is not supported for all classes
 * https://developer.android.com/training/dependency-injection/hilt-android#not-supported
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface NwService {
    fun nwUtil(): NetworkUtil
}
