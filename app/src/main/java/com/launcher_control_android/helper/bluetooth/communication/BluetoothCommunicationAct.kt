package com.launcher_control_android.helper.bluetooth.communication

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.content.Intent
import android.content.pm.PackageManager
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.LayoutRes
import androidx.core.app.ActivityCompat
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.lifecycleScope
import com.launcher_control_android.AppConstants.App.CHARACTERISTIC_UUID
import com.launcher_control_android.AppConstants.App.SERVICE_UUID
import com.launcher_control_android.R
import com.launcher_control_android.Strings
import com.launcher_control_android.helper.util.logE
import com.launcher_control_android.helper.util.showAlertDialog
import com.launcher_control_android.main.base.BaseAct
import com.launcher_control_android.main.base.BaseVM
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch


abstract class BluetoothCommunicationAct<binding : ViewDataBinding, VM : BaseVM>(
    @LayoutRes private val layoutId: Int
) : BaseAct<binding, VM>(layoutId), BluetoothCommunicationListener {
    protected var bluetoothService: BluetoothLeService? = null
    protected var deviceAddress = "Assign Remote Device Address"
    private var permissionCallback: (() -> Unit)? = null
    private var serviceCallback: (() -> Unit)? = null
    private var rssiJob: Job? = null

    fun onDeviceConnectionChange(isConnected: Boolean) {

    }

    protected fun startBluetoothService(callback: () -> Unit) {
        this.serviceCallback = callback
        bluetoothService = BluetoothLeService.getInstance(applicationContext)
        if (bluetoothService?.isDeviceConnected() == true) {
            bluetoothService?.setCallback(this@BluetoothCommunicationAct)
            callback.invoke()
            return
        }
        showProgress()
        if (bluetoothService?.initialize() != true) {
            "Unable to initialize Bluetooth".logE()
            showToast(Strings.bluetooth_not_available_title)
            hideProgress()
            finish()
        }
        bluetoothService?.setCallback(this@BluetoothCommunicationAct)
        val result = bluetoothService?.connect(deviceAddress)
        if (result == true) {
            showToast(Strings.trying_to_connect)
        } else {
            showToast(Strings.connection_fail)
        }
    }

    protected fun stopBluetoothService() {
        try {
            BluetoothLeService.getInstance(applicationContext).disconnectBluetoothDevice()
            hideProgress()
            bluetoothService = null
            serviceCallback = null
        } catch (e: Exception) {

        }
    }

    protected fun checkBluetooth(callback: () -> Unit) {
        permissionCallback = callback
        val hasBluetooth = packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)
        if (!hasBluetooth) {
            showAlertDialog(
                title = getString(Strings.bluetooth_not_available_title),
                message = getString(Strings.bluetooth_not_available_message),
                isCancelable = false,
                positiveBtnText = getString(Strings.ok),
                positiveClickListener = {
                    finishAffinity()
                }
            )
            return
        }

        /**
         * Check to see if the BLE feature is available.
         */
        val bluetoothAvailable = packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)
        val bluetoothLEAvailable =
            packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)
        if (bluetoothAvailable || bluetoothLEAvailable) {
//            if (checkRequiredPermission()) {
//                permissionCallback?.invoke()
//            } else {
                requestPermissionLauncher.launch(
                    arrayOf(
                        android.Manifest.permission.BLUETOOTH_CONNECT,
                        android.Manifest.permission.BLUETOOTH_SCAN
                    )
                )
//            }
        } else {
            this.showToast(getString(R.string.bluetooth_feature_not_available_in_this_device))
        }
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val isGranted = permissions.values.all { it }
            if (isGranted) {
                if (!BluetoothLeService.getInstance(applicationContext).isBluetoothEnabled()) {
                    "enabling_bluetooth".logE()
                    turnOnBluetoothLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
                } else {
                    permissionCallback?.invoke()
                }
            } else {
                showToast(Strings.please_allow_bluetooth_permission_to_use_this_features)
            }
        }

    private var turnOnBluetoothLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == RESULT_OK) {
                permissionCallback?.invoke()
            } else {
                this.showToast(getString(R.string.please_on_bluetooth_to_use_this_features))
            }
        }

    private fun checkRequiredPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            this@BluetoothCommunicationAct,
            android.Manifest.permission.BLUETOOTH_CONNECT
        ) == PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(
            this@BluetoothCommunicationAct,
            android.Manifest.permission.BLUETOOTH_SCAN
        ) == PackageManager.PERMISSION_GRANTED
    }

    protected fun sendCommand(command: String) {
        checkBluetooth {
            startBluetoothService {
                showProgress()
                bluetoothService?.sendCommand(command)
                bluetoothService?.waitingForRes = command
            }
        }
    }

    protected fun readData(command: String) {
        checkBluetooth {
            startBluetoothService {
                showProgress()
                bluetoothService?.readData(command)
            }
        }
    }

    protected fun startGatewaySignalStrengthUpdate() {
        if (rssiJob?.isActive == true) return
        rssiJob = lifecycleScope.launch(Dispatchers.IO) {
            while (isActive) {
                bluetoothService?.let { service ->
                    if (service.isDeviceConnected()) {
                        service.readRemoteRssi()
                    }
                }
                delay(2000)
            }
        }
    }

    protected fun stopGatewaySignalStrengthUpdate() {
        rssiJob?.cancel()
        rssiJob = null
    }

    override fun onDeviceConnectionChanged(gatt: BluetoothGatt?, isConnected: Boolean) {
        runOnUiThread {
            hideProgress()
            onDeviceConnectionChange(isConnected)
            if (isConnected) {
                showToast(Strings.device_connected)
            } else {
                showToast(Strings.device_disconnected)
            }
        }
    }

    override fun onServicesDiscovered(gatt: BluetoothGatt?, isServiceFound: Boolean) {
        runOnUiThread {
            hideProgress()
            if (isServiceFound) {
                serviceCallback?.invoke()
            } else {
                showToast(String.format(getString(Strings.peripheral_service_not_found), SERVICE_UUID))
            }
        }
    }

    override fun onCharacteristicWrite(
        gatt: BluetoothGatt?,
        characteristic: BluetoothGattCharacteristic?,
        isSuccess: Boolean
    ) {
        runOnUiThread {
            hideProgress()
            /*if (isSuccess) {
                vm?.waitingForRes = characteristic?.value?.decodeToString()
            }*/
        }
    }

    override fun onCharacteristicRead(
        gatt: BluetoothGatt?,
        characteristic: BluetoothGattCharacteristic?,
        value: ByteArray,
        isSuccess: Boolean
    ) {
        runOnUiThread {
            hideProgress()
            /*if (isSuccess) {
                vm?.waitingForRes = value.decodeToString()
            }*/
        }
    }

    override fun onReadRemoteRssi(gatt: BluetoothGatt?, rssi: Int, status: Int) {  }

    override fun onCharacteristicChanged(
        gatt: BluetoothGatt?,
        characteristic: BluetoothGattCharacteristic?,
        value: ByteArray
    ) {
        runOnUiThread { hideProgress() }
    }

    override fun onCharacteristicChangedTimeout() {
        bluetoothService?.waitingForRes = null
        runOnUiThread {
            hideProgress()
            showToast(Strings.operation_timed_out)
        }
    }

    override fun onCharacteristicNotFound() {
        runOnUiThread {
            hideProgress()
            showToast(String.format(getString(Strings.peripheral_charac_not_found), CHARACTERISTIC_UUID))
        }
    }

    protected fun showDialog(title: String, message: String, onPositiveClick: () -> Unit) {
        binding.root.post {
            showAlertDialog(
                title = title,
                message = message,
                isCancelable = false,
                positiveBtnText = getString(Strings.yes),
                negativeBtnText = getString(Strings.no),
                positiveClickListener = {
                    onPositiveClick.invoke()
                },
                negativeClickListener = {
                    finishAct()
                }
            )
        }
    }

    protected fun getWaitingForResUnit(): Int? {
        val command = bluetoothService?.waitingForRes
        val firstChar = command?.firstOrNull()?.toString()
        val unit = when (firstChar) {
            "1", "A", "a" -> 1
            "2", "B", "b" -> 2
            "3", "C", "c" -> 3
            "4", "D", "d" -> 4
            else -> 0 //null
        }
        return unit
    }
}