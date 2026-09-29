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
import com.launcher_control_android.helper.bluetooth.communication.BluetoothLeService
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
        setupRemoteSettingsUI()
        integrateDemoButton()
        fetchData()
        binding.toolbar.btnBack.setOnClickListener {
            finish()
        }
    }

    private fun setupRemoteSettingsUI() {

        // 1. AutoLock Switch
        binding.switchAutolockRemote.isChecked = prefs.autoLockRemote
        binding.switchAutolockRemote.setOnCheckedChangeListener { _, isChecked ->
            prefs.autoLockRemote = isChecked
        }

        // Klick-Logik für Ausklappen / Einklappen von "Program Gateway"
        binding.tvHeaderProgramGateway.setOnClickListener {
            val isExpanded = binding.layoutProgramGateway.isVisible
            binding.layoutProgramGateway.isVisible = !isExpanded
            binding.tvHeaderProgramGateway.text = if (!isExpanded) "Program Gateway ▲" else "Program Gateway ▶"
        }

        // 2. Standard Sound Spinner (Sounds 1..6)
        android.widget.ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            AppConstants.App.listOfSound
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.spinnerStdSound.adapter = adapter
        }

        val savedSoundIndex = (prefs.remoteStandardSound - 1).coerceIn(0, 5)
        binding.spinnerStdSound.setSelection(savedSoundIndex)

        binding.spinnerStdSound.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                val soundNumber = position + 1
                if (prefs.remoteStandardSound != soundNumber) {
                    prefs.remoteStandardSound = soundNumber
                }
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }

        // 3. Standard Sound&Fire Spinner (Makros 1..4)
        val macroList = listOf("Macro 1", "Macro 2", "Macro 3", "Macro 4")
        android.widget.ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            macroList
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.spinnerStdMacro.adapter = adapter
        }

        val savedMacroIndex = (prefs.remoteStandardMacro - 1).coerceIn(0, 3)
        binding.spinnerStdMacro.setSelection(savedMacroIndex)

        binding.spinnerStdMacro.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                val macroNumber = position + 1
                if (prefs.remoteStandardMacro != macroNumber) {
                    prefs.remoteStandardMacro = macroNumber
                }
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }

        // 4. Remote View Spinner (Prog-T0 = SIMPLE, Prog-T1 = Advanced)
        val viewList = listOf("SIMPLE Mode", "PRO Mode")
        android.widget.ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            viewList
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.spinnerRemoteView.adapter = adapter
        }
        binding.spinnerRemoteView.setSelection(prefs.remoteViewType.coerceIn(0, 1))

        // 5. Remote Volume Spinner (Prog-V1 .. Prog-V4)
        val volList = listOf("15% / Quiet", "30% / Normal", "50% / Loud", "100% / Full")
        android.widget.ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            volList
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.spinnerRemoteVolume.adapter = adapter
        }
        binding.spinnerRemoteVolume.setSelection((prefs.remoteVolume - 1).coerceIn(0, 3))

        // 6. Remote Kill-Timer Spinner (Prog-K0 .. Prog-K5)
        val killList = listOf("5 min", "10 min", "15 min", "20 min", "25 min", "30 min")
        android.widget.ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            killList
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.spinnerRemoteKill.adapter = adapter
        }
        binding.spinnerRemoteKill.setSelection(prefs.remoteKillTimer.coerceIn(0, 5))
    }

    override fun onDeviceConnectionChange(isConnected: Boolean) {
        super.onDeviceConnectionChange(isConnected)
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
            bluetoothService = BluetoothLeService.getInstance(applicationContext)
            bluetoothService?.setCallback(this@ConnectionConfigAct)

            if (vm.lastVoltageResponse != null) {
                setVoltage(vm.lastVoltageResponse)
            } else {
                binding.tvVoltage.text = "Device Voltage: loading..."
                binding.tvVoltage.isVisible = true
                binding.tvFirmware.text = "Firmware: loading..."
                binding.tvFirmware.isVisible = true
                binding.ivVoltage.isVisible = false
            }
        }
    }

    private val selectDeviceResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { _ ->
        vm.lastVoltageResponse = null
        fetchData()
        if (prefs.savedBluetoothDevice != null) {
            startBluetoothService { }
        }
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

            R.id.btn_send_std_sound -> {
                val soundNumber = binding.spinnerStdSound.selectedItemPosition + 1
                val soundName = AppConstants.App.listOfSound.getOrNull(binding.spinnerStdSound.selectedItemPosition) ?: ""
                prefs.remoteStandardSound = soundNumber
                if (bluetoothService?.isDeviceConnected() == true) {
                    sendCommand("Prog-S$soundNumber")
                    showToast("Set Standard > $soundName < on Remote")
                } else showToast("UNABLE TO SEND TO REMOTE!")
            }

            R.id.btn_send_std_macro -> {
                val macroNumber = binding.spinnerStdMacro.selectedItemPosition + 1
                val macroList = listOf("Macro 1", "Macro 2", "Macro 3", "Macro 4")
                val macroName = macroList.getOrNull(binding.spinnerStdMacro.selectedItemPosition) ?: "Macro $macroNumber"
                prefs.remoteStandardMacro = macroNumber
                if (bluetoothService?.isDeviceConnected() == true) {
                    sendCommand("Prog-M$macroNumber")
                    showToast("Set Standard > $macroName < on Remote")
                } else showToast("UNABLE TO SEND TO REMOTE!")
            }

            R.id.btn_send_remote_view -> {
                val mode = binding.spinnerRemoteView.selectedItemPosition // 0 = T0, 1 = T1
                prefs.remoteViewType = mode
                val modeName = if (mode == 0) "SIMPLE Mode" else "Pro Mode"
                if (bluetoothService?.isDeviceConnected() == true) {
                    sendCommand("Prog-T$mode")
                    showToast("Set > $modeName < on Remote")
                } else showToast("UNABLE TO SEND TO REMOTE!")
            }

            R.id.btn_send_remote_volume -> {
                val volNumber = binding.spinnerRemoteVolume.selectedItemPosition + 1
                val volList = listOf("15% - QUIET", "30% - NORMAL", "50% - LOUD", "100% - FULL")
                val volName = volList.getOrNull(binding.spinnerRemoteVolume.selectedItemPosition) ?: "Volume $volNumber"
                val vol = binding.spinnerRemoteVolume.selectedItemPosition + 1 // 1..4 -> V1..V4
                prefs.remoteVolume = vol
                if (bluetoothService?.isDeviceConnected() == true) {
                    sendCommand("Prog-V$vol")
                    showToast("Set Standard > $volName < on Remote")
                } else showToast("UNABLE TO SEND TO REMOTE!")
            }

            R.id.btn_send_remote_kill -> {
                val killNumber = binding.spinnerRemoteKill.selectedItemPosition + 1
                val killList = listOf("5 min", "10 min", "15 min", "20 min", "25 min", "30 min")
                val killName = killList.getOrNull(binding.spinnerRemoteKill.selectedItemPosition) ?: "Timer $killNumber"
                val killIdx = binding.spinnerRemoteKill.selectedItemPosition // 0..5 -> K0..K5
                prefs.remoteKillTimer = killIdx
                if (bluetoothService?.isDeviceConnected() == true) {
                    sendCommand("Prog-K$killIdx")
                    showToast("Set Autoshutdown > $killName < on Remote")
                } else showToast("UNABLE TO SEND TO REMOTE!")
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
                vm.lastVoltageResponse = null
                binding.tvVoltage.isVisible = false
                binding.tvFirmware.isVisible = false
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
        binding.layoutProgramGateway.isVisible = false
        binding.tvHeaderProgramGateway.text = "Program Gateway ▶"
        binding.tvVoltage.isVisible = false
        binding.tvFirmware.isVisible = false
        binding.ivVoltage.isVisible = false
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
            if (response.startsWith("v", ignoreCase = true)) {
                val fetchedUnitVoltage = response.replace("v", "", ignoreCase = true)
                setVoltage(fetchedUnitVoltage)
            } else {
                val unit = getWaitingForResUnit() ?: return@runOnUiThread
                if (bluetoothService?.waitingForRes == "1F" || bluetoothService?.waitingForRes == "2F" || bluetoothService?.waitingForRes == "3F" || bluetoothService?.waitingForRes == "4F") {
                    if (AppConstants.CommandResponse.isSuccess(response)) {
                        showToast(String.format(getString(Strings.unit_test_completed_successfully), unit))
                    } else {
                        showToast(String.format(getString(Strings.unit_test_completed_failed), unit))
                    }
                }
            }
        }
    }

    private fun setVoltage(voltageStr: String?) {
        vm.lastVoltageResponse = voltageStr
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