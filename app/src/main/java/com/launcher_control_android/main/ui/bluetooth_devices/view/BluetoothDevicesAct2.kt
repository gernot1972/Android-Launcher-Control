/*package com.launcher_control_android.main.ui.bluetooth_devices.view

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import com.launcher_control_android.BR
import com.launcher_control_android.Layouts
import com.launcher_control_android.Strings
import com.launcher_control_android.databinding.ActBluetoothDevicesBinding
import com.launcher_control_android.helper.bluetooth.BluetoothController
import com.launcher_control_android.helper.bluetooth.BluetoothDiscoveryDeviceListener
import com.launcher_control_android.helper.rvutil.RvUtil
import com.launcher_control_android.helper.util.logE
import com.launcher_control_android.helper.util.showAlertDialog
import com.launcher_control_android.main.base.BaseAct
import com.launcher_control_android.main.base.BaseVM
import com.launcher_control_android.main.base.rv.BaseRvBindingAdapter
import com.launcher_control_android.main.common.ApiRenderState


class BluetoothDevicesAct2 :
    BaseAct<ActBluetoothDevicesBinding, BaseVM>(Layouts.act_bluetooth_devices) {

    private lateinit var rvUtil: RvUtil

    private lateinit var bluetooth: BluetoothController
    private lateinit var bluetoothAdapter: BluetoothAdapter

    override val vm: BaseVM? = null

    override val hasProgress: Boolean = true

    override fun init() {
        setRecyclerView()

        // [#11] Ensures that the Bluetooth is available on this device before proceeding.
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

        // Sets up the bluetooth controller.
        bluetoothAdapter = (getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter
        bluetooth = BluetoothController(this, bluetoothAdapter, object: BluetoothDiscoveryDeviceListener {
            override fun onDeviceDiscovered(device: BluetoothDevice?) {
                device?.let {
                    rvUtil.addData(listOf(device))
                }
            }

            override fun onDeviceDiscoveryStarted() {
                showProgress()
            }

            override fun setBluetoothController(bluetooth: BluetoothController?) {

            }

            override fun onDeviceDiscoveryEnd() {
                hideProgress()
            }

            override fun onBluetoothStatusChanged() {

            }

            override fun onBluetoothTurningOn() {

            }

            override fun onDevicePairingEnded() {

            }
        })

        scanLeDevice()

        */
/*//*
/ Check to see if the Bluetooth classic feature is available.
        val bluetoothAvailable = packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)

        // Check to see if the BLE feature is available.
        val bluetoothLEAvailable = packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            scanLeDevice()
        } else {

        }*//*

    }

    private fun setRecyclerView() {
        rvUtil = RvUtil(
            rv = binding.rvDeviceList,
            adapter = BaseRvBindingAdapter(
                layoutId = Layouts.item_device,
                list = mutableListOf<BluetoothDevice>(),
                br = BR.device,
                clickListener = { view, item, pos ->

                }
            ),
            dataViews = listOf(binding.rvDeviceList),
            noDataViews = listOf(binding.layNoData.root)
        )
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                // Permission is granted. Continue the action or workflow in your
                // app.
                scanLeDevice()
            } else {
                // Explain to the user that the feature is unavailable because the
                // feature requires a permission that the user has denied. At the
                // same time, respect the user's decision. Don't link to system
                // settings in an effort to convince the user to change their
                // decision.
            }
        }

    private fun scanLeDevice() {
        // If the bluetooth is not enabled, turns it on.
        if (!bluetooth.isBluetoothEnabled) {
            "enabling_bluetooth".logE()
            bluetooth.turnOnBluetoothAndScheduleDiscovery()
        } else {
            //Prevents the user from spamming the button and thus glitching the UI.
            if (!bluetooth.isDiscovering) {
                // Starts the discovery.
                "device_discovery_started".logE()
                bluetooth.startDiscovery()
            } else {
                "device_discovery_stopped".logE()
                bluetooth.cancelDiscovery()
            }
        }
    }

    override fun renderState(apiRenderState: ApiRenderState) {

    }

    override fun onClick(v: View) {
        super.onClick(v)

    }
}*/