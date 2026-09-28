package com.launcher_control_android.main.ui.connection_config.view

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.launcher_control_android.AppConstants
import com.launcher_control_android.Layouts
import com.launcher_control_android.R
import com.launcher_control_android.Strings
import com.launcher_control_android.databinding.ActConnectionConfigBinding
import com.launcher_control_android.helper.bluetooth.communication.BluetoothCommunicationAct
import com.launcher_control_android.helper.util.getVoltageImageResId
import com.launcher_control_android.helper.util.showAlertDialog
import com.launcher_control_android.helper.util.startActivityForResult
import com.launcher_control_android.main.common.ApiRenderState
import com.launcher_control_android.main.ui.bluetooth_devices.view.BluetoothDevicesAct
import com.launcher_control_android.main.ui.connection_config.model.ConnectionConfigVM
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

@AndroidEntryPoint
class ConnectionConfigAct :
    BluetoothCommunicationAct<ActConnectionConfigBinding, ConnectionConfigVM>(Layouts.act_connection_config) {

    override val vm: ConnectionConfigVM by viewModels()

    override val hasProgress: Boolean = true

    override fun init() {
        setObserver()
        integrateDemoButton()
        fetchData()
        binding.toolbar.btnBack.setOnClickListener {
            finish()
        }
    }

    private fun setObserver() {
        vm.savedBluetoothDevice.observe(this) {
            if (it != null) {
                deviceAddress = it.address
                showSelectedGatewayUI()
            } else {
                showNoGatewaySelectedUI()
            }
        }
    }

    override fun renderState(apiRenderState: ApiRenderState) {

    }

    private fun fetchData() {
        val savedDevice = prefs.savedBluetoothDevice
        if (savedDevice != null) {
            vm.savedBluetoothDevice.postValue(savedDevice)
            deviceAddress = savedDevice.address
            binding.root.post {
                sendCommand("1E")
            }
        }
    }

    private val selectDeviceResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { _ ->
        fetchData()
    }

    override fun onClick(v: View) {
        super.onClick(v)
        when (v.id) {
            R.id.btn_back -> {
                finish()
            }

            R.id.btn_select_device -> {
                startActivityForResult(BluetoothDevicesAct::class.java, selectDeviceResultLauncher)
            }

            R.id.btn_delete -> {
                openDeleteDeviceDialog()
            }

            R.id.btn_unit_1 -> {
                prefs.unit1Model.testHexCode()?.let { sendCommand(it) }
            }

            R.id.btn_unit_2 -> {
                prefs.unit2Model.testHexCode()?.let { sendCommand(it) }
            }

            R.id.btn_unit_3 -> {
                prefs.unit3Model.testHexCode()?.let { sendCommand(it) }
            }

            R.id.btn_unit_4 -> {
                prefs.unit4Model.testHexCode()?.let { sendCommand(it) }
            }
        }
    }

    private fun openDeleteDeviceDialog() {
        showAlertDialog(
            title = getString(R.string.delete_device),
            message = getString(R.string.delete_device_confirmation_msd),
            isCancelable = false,
            positiveBtnText = getString(R.string.delete),
            positiveClickListener = {
                binding.tvVoltage.isVisible = false
                binding.ivVoltage.isVisible = false
                stopBluetoothService()
                prefs.savedBluetoothDevice = null
                vm.savedBluetoothDevice.postValue(null)
            },
            negativeBtnText = getString(R.string.cancel),
        )
    }

    private fun showNoGatewaySelectedUI() {
        binding.groupNoGatewaySelected.isVisible = true
        binding.groupSelectedGateway.isSelected = false
        binding.btnUnit1.isVisible = false
        binding.btnUnit2.isVisible = false
        binding.btnUnit3.isVisible = false
        binding.btnUnit4.isVisible = false
    }

    private fun showSelectedGatewayUI() {
        binding.groupNoGatewaySelected.isVisible = false
        binding.groupSelectedGateway.isSelected = true
    }

    override fun onCharacteristicChanged(
        gatt: BluetoothGatt?,
        characteristic: BluetoothGattCharacteristic?,
        value: ByteArray
    ) {
        super.onCharacteristicChanged(gatt, characteristic, value)
        runOnUiThread {
            hideProgress()
            val response = value.decodeToString().lowercase()
            val unit = getWaitingForResUnit() ?: return@runOnUiThread
            val responseStartWithUorV = response.startsWith("v", ignoreCase = true) || response.startsWith("u", ignoreCase = true)
            if (bluetoothService?.waitingForRes == "1E" && responseStartWithUorV) {
                if (response.startsWith("v", ignoreCase = true)) {
                    val fetchedUnitVoltage = response.replace("v", "", ignoreCase = true)
                    setVoltage(fetchedUnitVoltage)
                }
            } else if (!responseStartWithUorV && (bluetoothService?.waitingForRes == "1F" || bluetoothService?.waitingForRes == "2F" || bluetoothService?.waitingForRes == "3F" || bluetoothService?.waitingForRes == "4F")) {
                if (AppConstants.CommandResponse.isSuccess(response)) {
                    showToast(String.format(getString(Strings.unit_test_completed_successfully), unit))
                } else {
                    showToast(String.format(getString(Strings.unit_test_completed_failed), unit))
                }
            }
        }
    }

    private fun setVoltage(voltageStr: String?) {
        val arr = voltageStr?.replace("V", "", ignoreCase = true)?.split("-fw")
        val fetchedUnitVoltage = arr?.getOrNull(0) ?: ""
        val fetchedUnitFirmware = arr?.getOrNull(1) ?: ""
        val isVoltageVisible = fetchedUnitVoltage.isNotBlank()
        val isFirmwareVisible = fetchedUnitFirmware.isNotBlank()
        binding.tvVoltage.isVisible = isVoltageVisible
        binding.tvFirmware.isVisible = isFirmwareVisible
        try {
            binding.tvVoltage.text = String.format(getString(Strings.device_voltage), "${fetchedUnitVoltage}V")
            binding.tvFirmware.text = String.format(getString(Strings.device_firmware), fetchedUnitFirmware)
            fetchedUnitVoltage.toDoubleOrNull()?.let {
                binding.ivVoltage.isVisible = isVoltageVisible
                binding.ivVoltage.setImageResource(getVoltageImageResId(it) )
            }
        } catch (_: Exception) {}
    }

    private fun integrateDemoButton() {
        binding.btnDemo.setOnClickListener {
            showProgress()
            lifecycleScope.launch {
                delay(1000.milliseconds)
                showToast(String.format(getString(Strings.channel_fired_successfully), 1))
                hideProgress()

                delay(500.milliseconds)

                showProgress()
                delay(1000.milliseconds)
                showToast(String.format(getString(Strings.channel_fired_successfully), 2))
                hideProgress()

                delay(500.milliseconds)

                showProgress()
                delay(1000.milliseconds)
                showToast(String.format(getString(Strings.signal_sent_to_unit), 1))
                hideProgress()

                delay(500.milliseconds)

                showProgress()
                delay(1000.milliseconds)
                showToast(String.format(getString(Strings.channel_fired_successfully), 3))
                hideProgress()

                delay(500.milliseconds)

                showProgress()
                delay(1000.milliseconds)
                showToast(String.format(getString(Strings.signal_sent_to_unit), 1))
                hideProgress()

                delay(500.milliseconds)

                showProgress()
                delay(1000.milliseconds)
                showToast(String.format(getString(Strings.signal_sent_to_unit), 1))
                hideProgress()

                delay(500.milliseconds)

                showProgress()
                delay(1000.milliseconds)
                showToast(String.format(getString(Strings.channel_fired_successfully), 4))
                hideProgress()

                delay(500.milliseconds)

                showProgress()
                delay(1000.milliseconds)
                showToast(String.format(getString(Strings.channel_fired_successfully), 5))
                hideProgress()

                delay(500.milliseconds)

                showProgress()
                delay(1000.milliseconds)
                showToast(String.format(getString(Strings.signal_sent_to_unit), 1))
                hideProgress()

                delay(1000.milliseconds)
                showToast("Demo completed")
            }
        }
    }
}