package com.launcher_control_android.helper.bluetooth.communication

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic

interface BluetoothCommunicationListener {
    fun onReadRemoteRssi(gatt: BluetoothGatt?, rssi: Int, status: Int)
    fun onDeviceConnectionChanged(gatt: BluetoothGatt?, isConnected: Boolean)
    fun onServicesDiscovered(gatt: BluetoothGatt?, isServiceFound: Boolean)
    fun onCharacteristicWrite(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, isSuccess: Boolean)
    fun onCharacteristicRead(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, value: ByteArray, isSuccess: Boolean)
    fun onCharacteristicChanged(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, value: ByteArray)
    fun onCharacteristicChangedTimeout()
    fun onCharacteristicNotFound()
}