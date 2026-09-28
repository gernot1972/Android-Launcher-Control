/*package com.launcher_control_android.main.ui.bluetooth_devices.view

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.launcher_control_android.Layouts
import com.launcher_control_android.databinding.ActBluetoothDevicesBinding
import com.launcher_control_android.helper.rvutil.RvUtil
import com.launcher_control_android.main.base.BaseAct
import com.launcher_control_android.main.base.BaseVM
import com.launcher_control_android.main.base.rv.BaseRvBindingAdapter
import com.launcher_control_android.main.common.ApiRenderState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.launcher_control_android.BR

class BluetoothDevicesAct :
    BaseAct<ActBluetoothDevicesBinding, BaseVM>(Layouts.act_bluetooth_devices) {

    private lateinit var rvUtil: RvUtil

    private val bluetoothLeScanner by lazy {
        (getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter.bluetoothLeScanner
    }
    private var scanning = false

    private val SCAN_PERIOD: Long = 10000


    override val vm: BaseVM? = null

    override val hasProgress: Boolean = true

    override fun init() {
        setRecyclerView()

        // Check to see if the Bluetooth classic feature is available.
        val bluetoothAvailable = packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)

        // Check to see if the BLE feature is available.
        val bluetoothLEAvailable = packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            scanLeDevice()
        } else {

        }
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

    private val leScanCallback: ScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
            rvUtil.addData(listOf(result.device))
        }
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                // Permission is granted. Continue the action or workflow in your
                // app.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    scanLeDevice()
                }
            } else {
                // Explain to the user that the feature is unavailable because the
                // feature requires a permission that the user has denied. At the
                // same time, respect the user's decision. Don't link to system
                // settings in an effort to convince the user to change their
                // decision.
            }
        }


    @RequiresApi(Build.VERSION_CODES.S)
    private fun scanLeDevice() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_SCAN
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            if (!scanning) {
                CoroutineScope(Dispatchers.Default).launch {
                    delay(SCAN_PERIOD)
                    scanning = false
                    bluetoothLeScanner.stopScan(leScanCallback)
                }
                scanning = true
                bluetoothLeScanner.startScan(leScanCallback)
            } else {
                scanning = false
                bluetoothLeScanner.stopScan(leScanCallback)
            }
        } else {
            requestPermissionLauncher.launch(Manifest.permission.BLUETOOTH_SCAN)
        }
    }

    override fun renderState(apiRenderState: ApiRenderState) {

    }

    override fun onClick(v: View) {
        super.onClick(v)

    }
}*/
