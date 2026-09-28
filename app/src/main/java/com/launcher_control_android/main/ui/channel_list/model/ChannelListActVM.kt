package com.launcher_control_android.main.ui.channel_list.model

import com.launcher_control_android.R
import androidx.lifecycle.MutableLiveData
import com.launcher_control_android.AppConstants.App.listOfPressure
import com.launcher_control_android.AppConstants.App.listOfSound
import com.launcher_control_android.data.model.response.UnitModel
import com.launcher_control_android.helper.util.PrefUtil
import com.launcher_control_android.main.base.BaseVM
import com.launcher_control_android.main.common.FetchedChannelModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ChannelListActVM @Inject constructor(private val prefs: PrefUtil) : BaseVM() {

    var selectedUnitModel = MutableLiveData<UnitModel>()
    var fetchedUnitModel: FetchedChannelModel? = null
    var fetchedUnitVoltage: String? = null
    var fetchedUnitFirmware: String? = null
    val savedBluetoothDevice = MutableLiveData(prefs.savedBluetoothDevice)
    var autoupdatePressureEnable = MutableLiveData(prefs.autoupdatePressureEnable)

    var fireChannelNo: Int = -1
    var isAutoUpdateOnPause = false
    var fetchDataFromAutoupdate: Boolean = false
    var selectedVolumeStep: Int = 4 // 🎯 Standard: 100% (Stufe 4)

    fun getPrefUtil() = prefs

    fun setVoltageResponse(voltageStr: String?) {
        val arr = voltageStr?.replace("V", "", ignoreCase = true)?.split("-fw")
        fetchedUnitVoltage = arr?.getOrNull(0)
        fetchedUnitFirmware = arr?.getOrNull(1)
    }

    fun resetFetchedUnit() {
        fireChannelNo = -1
        fetchedUnitModel = null
        fetchedUnitVoltage = null
        fetchedUnitFirmware = null
    }

    fun noOfAvailableChannel(): Int {
        var noOfAvailableChannel = 0
        for (n in 1..(selectedUnitModel.value?.noOfChannel ?: 0)) {
            if (fetchedUnitModel?.isChannelAvailable(n) == true) noOfAvailableChannel++
        }
        return noOfAvailableChannel
    }

    fun isAllChannelsAvailable(): Boolean {
        val total = noOfAddedChannel()
        return total > 0 && noOfAvailableChannel() == total
    }

    fun noOfAddedChannel(): Int {
        return selectedUnitModel.value?.noOfChannel ?: 0
    }

    fun getPressure(): String {
        if (selectedUnitModel.value?.isServoVersion == true || fetchedUnitModel?.getBar() == 13) {
            return "Update"
        }
        if (fetchedUnitModel?.isBarFail() == true) {
            return "FAIL"
        }
        val currentBar = fetchedUnitModel?.getBar() ?: return "Update"
        val targetBar = fetchedUnitModel?.getPressure() ?: run {
            val idx = selectedUnitModel.value?.selectedPressure
            if (idx != null && idx in 0..5) idx * 2 else null
        }
        return if (targetBar != null) {
            "$currentBar bar / Set: $targetBar"
        } else {
            "$currentBar bar"
        }
    }

    fun getBatteryPercentText(): String {
        val hex = fetchedUnitModel?.getBatteryHex() ?: 14
        return when (hex) {
            15 -> "100%"
            14 -> "FAIL"
            13 -> "100%"
            12 -> "95%"
            11 -> "85%"
            10 -> "80%"
            9  -> "75%"
            8  -> "65%"
            7  -> "60%"
            6  -> "50%"
            5  -> "40%"
            4  -> "30%"
            3  -> "20%"
            2  -> "10%"
            1  -> "3%"
            0  -> "1%"
            else -> "FAIL"
        }
    }

    fun isCompressorActive(): Boolean {
        return fetchedUnitModel?.isCompressorActive() == true
    }

    fun isServoON(): Boolean {
        return selectedUnitModel.value?.isServoVersion == true || fetchedUnitModel?.getBar() == 13
    }

    fun isSoundOnlyMode(): Boolean {
        return selectedUnitModel.value?.isOnlySoundInstalled() == true
    }

    fun isCompressorLocked(): Boolean {
        val bar = fetchedUnitModel?.getBar() ?: return false
        val targetBar = fetchedUnitModel?.getPressure() ?: ((selectedUnitModel.value?.selectedPressure ?: 0) * 2)
        if (bar >= 13) return false
        return if (fetchedUnitModel?.hasCompressorTelemetry() == true) {
            targetBar > bar && !isCompressorActive()
        } else {
            targetBar > bar && bar <= 1
        }
    }

    fun isTargetPressureReached(): Boolean {
        val bar = fetchedUnitModel?.getBar() ?: return false
        val targetBar = fetchedUnitModel?.getPressure() ?: ((selectedUnitModel.value?.selectedPressure ?: 0) * 2)
        return bar < 13 && targetBar > 0 && bar >= targetBar
    }

    fun getLeftStatusIconResId(): Int {
        return when {
            isServoON() -> com.launcher_control_android.Drawables.remember_me_24
            isSoundOnlyMode() -> com.launcher_control_android.Drawables.ic_volume
            else -> com.launcher_control_android.Drawables.ic_fan
        }
    }

    fun getLeftStatusIconColor(): Int {
        return when {
            fetchedUnitModel?.isBarFail() == true -> R.color.colorRed
            isServoON() || isSoundOnlyMode() -> R.color.colorGreen
            isCompressorLocked() -> R.color.colorRed
            isTargetPressureReached() -> R.color.colorGreen
            isCompressorActive() -> R.color.colorOrange
            else -> R.color.white
        }
    }

    fun getLeftStatusText(): String {
        return when {
            fetchedUnitModel?.isBarFail() == true -> "FAIL"
            isServoON() -> "SERVO\nOK"
            isSoundOnlyMode() -> "SOUND\nOK"
            isCompressorLocked() -> "LOCK"
            isCompressorActive() -> "ON"
            else -> "OFF"
        }
    }

    fun getReloadText(): String {
        val unitNumber = selectedUnitModel.value?.unitNumber ?: 1
        return if (isCompressorLocked() && isAllChannelsAvailable()) {
            "Release Unit $unitNumber"
        } else {
            "Reload Unit $unitNumber"
        }
    }

    fun getHeaderTitleText(servoText: String, autoText: String, manualText: String, autoEnable: Boolean): String {
        val unit = selectedUnitModel.value ?: return manualText
        return when {
            unit.isOnlySoundInstalled() -> "Unit ${unit.unitNumber}\nSOUND CONTROL"
            unit.isServoVersion -> String.format(servoText, unit.unitNumber)
            autoEnable -> autoText
            else -> manualText
        }
    }

    fun getSoundButtonText(soundIndex: Int): String {
        return getSoundName(soundIndex)
    }

    fun setSoundForSelectedUnit(index: Int) {
        selectedUnitModel.value?.selectedSound = index
        when (selectedUnitModel.value?.unitNumber) {
            1 -> {
                val unit1 = prefs.unit1Model
                unit1.selectedSound = index
                prefs.unit1Model = unit1
            }
            2 -> {
                val unit2 = prefs.unit2Model
                unit2.selectedSound = index
                prefs.unit2Model = unit2
            }
            3 -> {
                val unit3 = prefs.unit3Model
                unit3.selectedSound = index
                prefs.unit3Model = unit3
            }
            4 -> {
                val unit4 = prefs.unit4Model
                unit4.selectedSound = index
                prefs.unit4Model = unit4
            }
        }
    }

    fun setPressureForSelectedUnit(index: Int) {
        selectedUnitModel.value?.selectedPressure = index
        when (selectedUnitModel.value?.unitNumber) {
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

    fun refreshSelectedUnit() {
        when (selectedUnitModel.value?.unitNumber) {
            1 -> {
                selectedUnitModel.postValue(prefs.unit1Model)
            }
            2 -> {
                selectedUnitModel.postValue(prefs.unit2Model)
            }
            3 -> {
                selectedUnitModel.postValue(prefs.unit3Model)
            }
            4 -> {
                selectedUnitModel.postValue(prefs.unit4Model)
            }
        }
    }

    fun setPressureInBar(bar: Int?) {
        val selectedPressureIndex = listOfPressure.indexOfFirst { it.replace("bar", "").toIntOrNull() == bar }
        if (selectedPressureIndex != -1) {
            setPressureForSelectedUnit(selectedPressureIndex)
        }
    }


    fun getSoundName(soundIndex: Int): String {
        return listOfSound.getOrNull(soundIndex) ?: ""
    }

    fun getNextAvailableChannel(): Int? {
        val nextAvailableChannel = fetchedUnitModel?.getNextChannel()
        val noOfChannelAdded = selectedUnitModel.value?.noOfChannel ?: 0
        return if (nextAvailableChannel != null && nextAvailableChannel <= noOfChannelAdded) {
            nextAvailableChannel
        } else {
            null
        }
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

    fun toggleAutoupdatePressureEnable() {
        val isEnable = !(autoupdatePressureEnable.value ?: false)
        autoupdatePressureEnable.postValue(isEnable)
        prefs.autoupdatePressureEnable = isEnable
    }
}