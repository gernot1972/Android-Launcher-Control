package com.launcher_control_android.main.ui.connection_config.model

import androidx.lifecycle.MutableLiveData
import com.launcher_control_android.helper.util.PrefUtil
import com.launcher_control_android.main.base.BaseVM
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ConnectionConfigVM @Inject constructor(private val prefs: PrefUtil) : BaseVM() {
    val savedBluetoothDevice = MutableLiveData(prefs.savedBluetoothDevice)
}