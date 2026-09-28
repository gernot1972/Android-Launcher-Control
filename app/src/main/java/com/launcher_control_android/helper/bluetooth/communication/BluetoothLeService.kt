package com.launcher_control_android.helper.bluetooth.communication

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import com.launcher_control_android.data.model.response.isSoundCommand
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.os.Handler
import android.os.Looper
import com.launcher_control_android.AppConstants.App.CHARACTERISTIC_UUID
import com.launcher_control_android.AppConstants.App.SERVICE_UUID
import com.launcher_control_android.helper.bluetooth.hexDecodedData
import com.launcher_control_android.helper.util.logE
import com.launcher_control_android.helper.util.logW
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID


class BluetoothLeService private constructor(val context: Context) {

    companion object {
        private const val STATE_CONNECTED = 0
        private const val STATE_DISCONNECTED = 1
        private const val STATE_SERVICE_DISCOVERED = 2

        private var instance: BluetoothLeService? = null
        fun getInstance(context: Context): BluetoothLeService {
            if (instance == null) {
                instance = BluetoothLeService(context)
            }
            return instance!!
        }
    }

    var waitingForRes: String? = null

    private var bluetoothCommunicationListener: BluetoothCommunicationListener? = null

    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothGatt: BluetoothGatt? = null
    private var connectionState = STATE_DISCONNECTED
    private val timeoutMillis = 1000L
    private val handler = Handler(Looper.getMainLooper())
    private val timeoutRunnable = Runnable {
        bluetoothCommunicationListener?.onCharacteristicChangedTimeout()
    }

    fun initialize(): Boolean {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (bluetoothAdapter == null) {
            "Unable to obtain a BluetoothAdapter.".logE()
            return false
        }
        return true
    }

    private val bluetoothGattCallback = object : BluetoothGattCallback() {

        override fun onReadRemoteRssi(
            gatt: BluetoothGatt,
            rssi: Int,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                bluetoothCommunicationListener?.onReadRemoteRssi(gatt, rssi, status)
            }
        }

        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                if (connectionState == STATE_DISCONNECTED) {
                    bluetoothGatt?.discoverServices()
                    connectionState = STATE_CONNECTED
                    bluetoothCommunicationListener?.onDeviceConnectionChanged(
                        gatt,
                        isConnected = true
                    )
                }
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                connectionState = STATE_DISCONNECTED
                bluetoothCommunicationListener?.onDeviceConnectionChanged(gatt, isConnected = false)
            }
        }

        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            var descriptor: BluetoothGattDescriptor? = null
            var isIndicate: Boolean? = null
            var isNotificationSet: Boolean? = null
            if (connectionState == STATE_CONNECTED) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    val service =
                        gatt?.services?.find { service -> service.characteristics.any { it.uuid == CHARACTERISTIC_UUID } }
                    if (service != null) {
                        SERVICE_UUID = service.uuid
                        connectionState = STATE_SERVICE_DISCOVERED

                        val characteristic =
                            service.characteristics?.find { it.uuid == CHARACTERISTIC_UUID }
                        if (characteristic != null) {
                            isNotificationSet =
                                gatt.setCharacteristicNotification(characteristic, true)
                            descriptor =
                                characteristic.descriptors?.find { it.uuid == UUID.fromString("00002902-0000-1000-8000-00805f9b34fb") }
                            isIndicate =
                                0 != (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_INDICATE)
                            if (descriptor != null) {
                                if (isIndicate) {
                                    descriptor.value =
                                        BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
                                    bluetoothGatt?.writeDescriptor(descriptor)
                                } else {
                                    descriptor.value =
                                        BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                                    bluetoothGatt?.writeDescriptor(descriptor)
                                }
                            } else {
                                bluetoothCommunicationListener?.onServicesDiscovered(
                                    gatt,
                                    isServiceFound = true
                                )
                            }
                        }
                    } else {
                        bluetoothCommunicationListener?.onServicesDiscovered(
                            gatt,
                            isServiceFound = false
                        )
                        "onServicesDiscovered received: $status".logW()
                    }
                } else {
                    bluetoothCommunicationListener?.onServicesDiscovered(
                        gatt,
                        isServiceFound = false
                    )
                    "onServicesDiscovered received: $status".logW()
                }
            }
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int
        ) {
            super.onCharacteristicWrite(gatt, characteristic, status)
            if (status == BluetoothGatt.GATT_SUCCESS) {
                bluetoothCommunicationListener?.onCharacteristicWrite(
                    gatt,
                    characteristic,
                    isSuccess = true
                )
                checkCharacteristicValue(gatt, characteristic)
//                broadcastUpdate(ACTION_COMMAND_SENT_SUCCESS)
            } else {
                bluetoothCommunicationListener?.onCharacteristicWrite(
                    gatt,
                    characteristic,
                    isSuccess = false
                )
//                broadcastUpdate(ACTION_COMMAND_SENT_FAILURE)
            }
            "onCharacteristicWrite: $status".logE()
        }

        @SuppressLint("MissingPermission")
        private fun checkCharacteristicValue(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?
        ) {
            GlobalScope.launch {
                delay(2000)
                if (characteristic != null) {
                    gatt?.readCharacteristic(characteristic)
                }
            }
        }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt?,
            descriptor: BluetoothGattDescriptor?,
            status: Int
        ) {
            super.onDescriptorWrite(gatt, descriptor, status)
            bluetoothCommunicationListener?.onServicesDiscovered(
                gatt,
                isServiceFound = true
            )
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int
        ) {
            super.onCharacteristicRead(gatt, characteristic, status)
            val value = characteristic?.value ?: return
            if (status == BluetoothGatt.GATT_SUCCESS) {
                bluetoothCommunicationListener?.onCharacteristicRead(
                    gatt,
                    characteristic,
                    value,
                    isSuccess = true
                )
            } else {
                bluetoothCommunicationListener?.onCharacteristicRead(
                    gatt,
                    characteristic,
                    value,
                    isSuccess = false
                )
            }
            "onCharacteristicReadDeprecated".logE()
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
            status: Int
        ) {
            super.onCharacteristicRead(gatt, characteristic, value, status)
            if (status == BluetoothGatt.GATT_SUCCESS) {
                bluetoothCommunicationListener?.onCharacteristicRead(
                    gatt,
                    characteristic,
                    value,
                    isSuccess = true
                )
            } else {
                bluetoothCommunicationListener?.onCharacteristicRead(
                    gatt,
                    characteristic,
                    value,
                    isSuccess = false
                )
            }
            "onCharacteristicRead".logE()
        }

        @Deprecated("Deprecated in Java")
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?
        ) {
            handler.removeCallbacks(timeoutRunnable)
            super.onCharacteristicChanged(gatt, characteristic)

            characteristic?.value?.let {
                bluetoothCommunicationListener?.onCharacteristicChanged(gatt, characteristic, it)
            }
            "onCharacteristicChanged Deprecated".logE()
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            handler.removeCallbacks(timeoutRunnable)
            super.onCharacteristicChanged(gatt, characteristic, value)
            bluetoothCommunicationListener?.onCharacteristicChanged(gatt, characteristic, value)
            "onCharacteristicChanged".logE()
        }
    }

    @SuppressLint("MissingPermission")
    fun connect(address: String): Boolean {
        bluetoothAdapter?.let { adapter ->
            try {
                val device = adapter.getRemoteDevice(address)
                // connect to the GATT server on the device
                bluetoothGatt = device.connectGatt(context, false, bluetoothGattCallback, 2)
                return true
            } catch (exception: IllegalArgumentException) {
//                broadcastUpdate(ACTION_COMMAND_SENT_FAILURE)
                return false
            }
            // connect to the GATT server on the device
        } ?: run {
            "BluetoothAdapter not initialized".logE()
            return false
        }
    }

    fun getSupportedGattServices(): List<BluetoothGattService?>? {
        return bluetoothGatt?.services
    }

    fun isBluetoothEnabled(): Boolean = bluetoothAdapter?.isEnabled ?: false

    fun isDeviceConnected() = connectionState != STATE_DISCONNECTED

    fun setCallback(callback: BluetoothCommunicationListener) {
        this.bluetoothCommunicationListener = callback
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @SuppressLint("MissingPermission")
    fun sendCommand(command: String) {
        val characteristic =
            bluetoothGatt?.getService(SERVICE_UUID)?.getCharacteristic(CHARACTERISTIC_UUID)

        if (characteristic == null) {
            bluetoothCommunicationListener?.onCharacteristicNotFound()
            return
        }

        characteristic.let {
            it.setValue(command.hexDecodedData())
            val success = bluetoothGatt?.writeCharacteristic(it) ?: false
            "Write status: $command $success".logE()
            if (!isSoundCommand(command)) {
                setTimeOutCallBack(success)
            }
            if (!success) {
                scope.launch {
                    delay(100)
                    sendCommand(command)
                }
            }
        } ?: setTimeOutCallBack()
    }

    @SuppressLint("MissingPermission")
    fun readData(command: String) {
        val characteristic =
            bluetoothGatt?.getService(SERVICE_UUID)?.getCharacteristic(CHARACTERISTIC_UUID)

        if (characteristic == null) {
            bluetoothCommunicationListener?.onCharacteristicNotFound()
            return
        }

        characteristic.let {
            it.setValue(command.hexDecodedData())
            val success = bluetoothGatt?.readCharacteristic(it)
            "Read status: $success".logE()
            setTimeOutCallBack(success)
        } ?: setTimeOutCallBack()
    }

    @SuppressLint("MissingPermission")
    fun readRemoteRssi() {
        bluetoothGatt?.readRemoteRssi()
    }

    private fun setTimeOutCallBack(success: Boolean? = null) {
        if (success == true) {
            handler.postDelayed(timeoutRunnable, timeoutMillis)
        } else {
//            handler.
        }
    }

    @SuppressLint("MissingPermission")
    fun disconnectBluetoothDevice() {
        bluetoothGatt?.disconnect()
        bluetoothGatt?.close()
        bluetoothGatt = null
        connectionState = STATE_DISCONNECTED
        bluetoothCommunicationListener?.onDeviceConnectionChanged(
            null,
            isConnected = false
        )
    }
}