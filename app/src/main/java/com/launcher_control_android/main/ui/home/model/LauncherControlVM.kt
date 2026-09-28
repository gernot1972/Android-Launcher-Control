package com.launcher_control_android.main.ui.home.model

import androidx.lifecycle.MutableLiveData
import com.launcher_control_android.AppConstants.App.listOfPressure
import com.launcher_control_android.data.model.response.DeviceModel
import com.launcher_control_android.data.model.response.UnitModel
import com.launcher_control_android.helper.util.PrefUtil
import com.launcher_control_android.helper.util.getVoltagePercentage
import com.launcher_control_android.helper.util.logE
import com.launcher_control_android.main.base.BaseVM
import com.launcher_control_android.main.common.FetchedChannelModel
import com.launcher_control_android.main.ui.home.GatewayConnectionStatus
import com.launcher_control_android.main.ui.home.LauncherControlUIStateModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class LauncherControlVM @Inject constructor(val prefs: PrefUtil) : BaseVM() {

    val uiState: MutableLiveData<LauncherControlUIStateModel> = MutableLiveData<LauncherControlUIStateModel>()
    val toolbarTitle = MutableLiveData<String>("Launcher Control")

    init {
        reloadState()
    }

//    var selectedUnitModel = MutableLiveData<UnitModel>()
//    var fetchedUnitModel = MutableLiveData<FetchedChannelModel?>()
    var disarmedUnitFetchedData = MutableLiveData<FetchedChannelModel?>()
    var gatewaySignalStrength = MutableLiveData(prefs.savedBluetoothDevice?.signalStrength ?: 0)
    var fetchedUnitVoltage: String? = null
    var fetchedUnitFirmware: String? = null
//    var selectedUnitTvId: Int? = null
    var fetchDataFromAutoupdate: Boolean = false
    var selectedSoundIndex: Int = 0
    var isAutoUpdateOnPause = false
    var isDisarmPause = false
    var currentFiredChannel: Int? = null

    fun getPrefUtil() = prefs

    fun reloadState() {
        val isGatewaySetup = prefs.savedBluetoothDevice != null
        val isUnitSetup = prefs.unit1Model.isVisible() || prefs.unit2Model.isVisible() || prefs.unit3Model.isVisible() || prefs.unit4Model.isVisible()

        uiState.value = LauncherControlUIStateModel(
            isGatewaySetup = isGatewaySetup,
            isUnitSetup = isUnitSetup,
            gatewayConnectionStatus = uiState.value?.gatewayConnectionStatus
                ?: GatewayConnectionStatus.NOT_CONNECTED,
            selectedUnit = uiState.value?.selectedUnit,
            fetchedUnitModel = uiState.value?.fetchedUnitModel
        )
    }

    fun setGatewayConnected() {
        uiState.value = uiState.value?.copy(gatewayConnectionStatus = GatewayConnectionStatus.CONNECTED)
    }

    fun setGatewayConnecting() {
        uiState.value = uiState.value?.copy(gatewayConnectionStatus = GatewayConnectionStatus.CONNECTING)
    }

    fun setGatewayDisconnected() {
        uiState.value = uiState.value?.copy(gatewayConnectionStatus = GatewayConnectionStatus.NOT_CONNECTED)
    }

    fun setSelectedUnit(unitModel: UnitModel?) {
        disarmedUnitFetchedData.value = null
        uiState.value = uiState.value?.copy(
            selectedUnit = unitModel,
            fetchedUnitModel = null,
            isDisarmed = false
        )
    }

    fun setFetchedUnitData(fetchedUnitData: FetchedChannelModel?) {
        val isCurrentlyDisarmed = uiState.value?.isDisarmed == true || disarmedUnitFetchedData.value != null
        if (isCurrentlyDisarmed && fetchedUnitData != null) {
            disarmedUnitFetchedData.value = fetchedUnitData
            uiState.value = uiState.value?.copy(
                fetchedUnitModel = fetchedUnitData,
                isDisarmed = true
            )
        } else {
            disarmedUnitFetchedData.value = null
            uiState.value = uiState.value?.copy(
                fetchedUnitModel = fetchedUnitData,
                isDisarmed = false
            )
        }
    }

    fun setUnitDisarmed(isDisarmed: Boolean) {
        if (isDisarmed) {
            val currentData = uiState.value?.fetchedUnitModel ?: disarmedUnitFetchedData.value
            disarmedUnitFetchedData.value = currentData
            uiState.value = uiState.value?.copy(
                fetchedUnitModel = currentData,
                isDisarmed = true
            )
        } else {
            val restoredData = disarmedUnitFetchedData.value ?: uiState.value?.fetchedUnitModel
            disarmedUnitFetchedData.value = null
            uiState.value = uiState.value?.copy(
                fetchedUnitModel = restoredData,
                isDisarmed = false
            )
        }
    }

    fun resetFetchedUnit() {
        setFetchedUnitData(null)
        setVoltageResponse(null)
    }

    fun getSoundName(index: Int): String {
        return when(index) {
            0 -> "Duck"
            1 -> "Pheasant"
            2 -> "Goose"
            3 -> "Brrr"
            4 -> "Gunshot"
            5 -> "Magpie"
            else -> ""
        }
    }

    fun setVoltageResponse(voltageStr: String?) {
        val arr = voltageStr?.replace("V", "", ignoreCase = true)?.split("-fw")
        fetchedUnitVoltage = arr?.getOrNull(0)
        fetchedUnitFirmware = arr?.getOrNull(1)
    }

    fun getGatewayDevice(): DeviceModel? {
        return prefs.savedBluetoothDevice
    }

    fun getFetchedVoltagePercentage(): Int {
        return getVoltagePercentage(fetchedUnitVoltage?.toDoubleOrNull() ?: 0.0)
    }

    fun getUnitModel(unitNumber: Int): UnitModel? {
        return when (unitNumber) {
            1 -> prefs.unit1Model
            2 -> prefs.unit2Model
            3 -> prefs.unit3Model
            4 -> prefs.unit4Model
            else -> null
        }
    }

    fun hasStayArmedActive(): Boolean {
        return disarmedUnitFetchedData.value == null
    }

    fun setSoundForSelectedUnit(index: Int) {
        val currentUnit = uiState.value?.selectedUnit ?: return
        currentUnit.selectedSound = index
        when (currentUnit.unitNumber) {
            1 -> { val unit1 = prefs.unit1Model; unit1.selectedSound = index; prefs.unit1Model = unit1 }
            2 -> { val unit2 = prefs.unit2Model; unit2.selectedSound = index; prefs.unit2Model = unit2 }
            3 -> { val unit3 = prefs.unit3Model; unit3.selectedSound = index; prefs.unit3Model = unit3 }
            4 -> { val unit4 = prefs.unit4Model; unit4.selectedSound = index; prefs.unit4Model = unit4 }
        }
        // 🎯 Erzeugt sofort ein neues UIState-Event für das DataBinding
        uiState.value = uiState.value?.copy(selectedUnit = currentUnit)
    }

    fun setPressureForSelectedUnit(index: Int) {
        uiState.value?.selectedUnit?.selectedPressure = index
        when (uiState.value?.selectedUnit?.unitNumber) {
            1 -> {
                val unit1 = prefs.unit1Model
                unit1.selectedPressure = index
                prefs.unit1Model = unit1
            }
            2 -> {
                val unit2 = prefs.unit2Model
                unit2.selectedPressure = index
                prefs.unit2Model = unit2
            }
            3 -> {
                val unit3 = prefs.unit3Model
                unit3.selectedPressure = index
                prefs.unit3Model = unit3
            }
            4 -> {
                val unit4 = prefs.unit4Model
                unit4.selectedPressure = index
                prefs.unit4Model = unit4
            }
        }
    }

    fun updateUnitPref(unit: UnitModel, which: Int) {
        when (which) {
            1 -> prefs.unit1Model = unit
            2 -> prefs.unit2Model = unit
            3 -> prefs.unit3Model = unit
            4 -> prefs.unit4Model = unit
        }
    }

    fun setPressureInBar(bar: Int?) {
        val selectedPressureIndex = listOfPressure.indexOfFirst { it.replace("bar", "").toIntOrNull() == bar }
        if (selectedPressureIndex != -1) {
            setPressureForSelectedUnit(selectedPressureIndex)
        }
    }
}