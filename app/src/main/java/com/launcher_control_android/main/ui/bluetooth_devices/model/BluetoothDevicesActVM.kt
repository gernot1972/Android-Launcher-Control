package com.launcher_control_android.main.ui.bluetooth_devices.model

import com.launcher_control_android.data.model.response.DeviceModel
import com.launcher_control_android.main.base.BaseVM

class BluetoothDevicesActVM: BaseVM() {
    val bluetoothDeviceList = mutableListOf<DeviceModel>()
}