package com.launcher_control_android.main.ui.bluetooth_devices.view

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.app.ActivityCompat
import com.launcher_control_android.AppConstants.App.SERVICE_UUID
import com.launcher_control_android.BR
import com.launcher_control_android.BuildConfig
import com.launcher_control_android.Layouts
import com.launcher_control_android.R
import com.launcher_control_android.data.model.response.DeviceModel
import com.launcher_control_android.databinding.ActBluetoothDevicesBinding
import com.launcher_control_android.helper.rvutil.RvUtil
import com.launcher_control_android.helper.util.showAlertDialog
import com.launcher_control_android.main.base.BaseAct
import com.launcher_control_android.main.base.rv.BaseRvBindingAdapter
import com.launcher_control_android.main.common.ApiRenderState
import com.launcher_control_android.main.ui.bluetooth_devices.model.BluetoothDevicesActVM
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class BluetoothDevicesAct :
    BaseAct<ActBluetoothDevicesBinding, BluetoothDevicesActVM>(Layouts.act_bluetooth_devices) {

    private lateinit var rvUtil: RvUtil

    private val scanner by lazy { bluetoothAdapter.bluetoothLeScanner }

    private val bluetoothAdapter by lazy {
        (getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter
    }

    private var isScanning = false
    private val scanPeriod = 10_000L   // stop after 10 seconds

    private val scanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            val address = device.address ?: return
            val deviceName = device.name ?: return //?: "Unknown"

            if (vm.bluetoothDeviceList.none { it.address == address }) {
                val model = DeviceModel(
                    name = deviceName,
                    address = address,
                    signalStrength = result.rssi
                )
                rvUtil.addData(listOf(model))
            }
        }

        override fun onScanFailed(errorCode: Int) {
            hideProgress()
            showToast("BLE Scan failed: $errorCode")
        }
    }

    override val vm: BluetoothDevicesActVM by viewModels()

    override val hasProgress: Boolean = true

    override fun init() {
        setRecyclerView()

        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            showAlertDialog(
                title = getString(R.string.bluetooth_not_available_title),
                message = getString(R.string.bluetooth_not_available_message),
                isCancelable = false,
                positiveBtnText = getString(R.string.ok)
            ) { finishAffinity() }
            return
        }

        startBleScanIfPermitted()
    }

    private fun setRecyclerView() {
        rvUtil = RvUtil(
            isInitialisation = true,
            rv = binding.rvDeviceList,
            adapter = BaseRvBindingAdapter(
                layoutId = Layouts.item_device,
                list = vm.bluetoothDeviceList,
                br = BR.device,
                clickListener = { view, item, pos ->
                    prefs.savedBluetoothDevice = item
                    stopBleScan()
                    finish()
                }
            ),
            dataViews = listOf(binding.rvDeviceList),
            noDataViews = listOf(binding.layNoData.root)
        )
    }

    private fun startBleScanIfPermitted() {
        if (hasBlePermissions()) startBleScan()
        else requestPermissionLauncher.launch(requiredPermissions())
    }

    @SuppressLint("MissingPermission")
    private fun startBleScan() {
        if (!bluetoothAdapter.isEnabled) {
            turnOnBluetoothLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
            return
        }
        if (isScanning) return

        showProgress()
        isScanning = true
        val myServiceUuid = ParcelUuid.fromString(SERVICE_UUID.toString())
        val filters = listOf(
            ScanFilter.Builder()
                .setServiceUuid(myServiceUuid)
                .build()
        )
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()
        scanner.startScan(if (BuildConfig.DEBUG) null else filters, settings, scanCallback)

        // Stop after timeout
        Handler(Looper.getMainLooper()).postDelayed({ stopBleScan() }, scanPeriod)
    }

    @SuppressLint("MissingPermission")
    private fun stopBleScan() {
        if (!isScanning) return
        isScanning = false
        hideProgress()
        scanner.stopScan(scanCallback)
        if (vm.bluetoothDeviceList.isEmpty()) showToast(R.string.device_not_found)
    }

    override fun renderState(apiRenderState: ApiRenderState) {

    }

    // Permissions -------------------------------------------------------------
    @SuppressLint("InlinedApi")
    private fun hasBlePermissions(): Boolean {
        val ctx = this
        val scanOk = ActivityCompat.checkSelfPermission(ctx, Manifest.permission.BLUETOOTH_SCAN) ==
                PackageManager.PERMISSION_GRANTED
        val connectOk =
            ActivityCompat.checkSelfPermission(ctx, Manifest.permission.BLUETOOTH_CONNECT) ==
                    PackageManager.PERMISSION_GRANTED
        val locationOk =
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S)
                ActivityCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) ==
                        PackageManager.PERMISSION_GRANTED
            else true
        return scanOk && connectOk && locationOk
    }

    @SuppressLint("InlinedApi")
    private fun requiredPermissions() = buildList {
        add(Manifest.permission.BLUETOOTH_SCAN)
        add(Manifest.permission.BLUETOOTH_CONNECT)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S)
            add(Manifest.permission.ACCESS_FINE_LOCATION)
    }.toTypedArray()

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted ->
            if (granted.values.all { it }) {
                startBleScan()
            }
            else {
                finishAct()
                showToast(R.string.permission_required)
            }
        }

    private val turnOnBluetoothLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                startActivity(intent)
                finishAct()
                overridePendingTransition(0,0)
            }
            else {
                showToast(R.string.please_on_bluetooth_to_use_this_features)
                startBleScan()
            }
        }

    override fun onPause() {
        super.onPause()
        stopBleScan()
    }
}