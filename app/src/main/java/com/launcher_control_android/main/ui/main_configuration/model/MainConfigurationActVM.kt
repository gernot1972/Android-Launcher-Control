package com.launcher_control_android.main.ui.main_configuration.model

import androidx.lifecycle.MutableLiveData
import com.launcher_control_android.data.model.response.UnitModel
import com.launcher_control_android.helper.util.PrefUtil
import com.launcher_control_android.main.base.BaseVM
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject


@HiltViewModel
class MainConfigurationActVM @Inject constructor(private val prefs: PrefUtil) : BaseVM() {

    val appVersion: String = com.launcher_control_android.BuildConfig.VERSION_NAME

    var unit1 = prefs.unit1Model
    var unit2 = prefs.unit2Model
    var unit3 = prefs.unit3Model
    var unit4 = prefs.unit4Model

    var noOfSoundCount = MutableLiveData(prefs.soundCount)
    var delayInSound = MutableLiveData(prefs.soundDelay)
    var delayInFire = MutableLiveData(prefs.fireDelay)
    var armedInterval = MutableLiveData(prefs.armedInterval)
    var autoupdatePressureEnable = MutableLiveData(prefs.autoupdatePressureEnable)
    var autoupdatePressureDelay = MutableLiveData(prefs.autoupdatePressureDelay)
    var gunshotAfterLaunchEnable = MutableLiveData(prefs.gunshotAfterLaunchEnable)

    var advancedControlIsStandardEnable = MutableLiveData(prefs.advancedControlIsStandardEnable)
    var randomizedSoundAndFireEnable = MutableLiveData(prefs.randomizedSoundAndFireEnable)
    var randomSoundCountEnable = MutableLiveData(prefs.randomSoundCountEnable)
    var randomSoundCountMin = MutableLiveData(prefs.randomSoundCountMin)
    var randomSoundCountMax = MutableLiveData(prefs.randomSoundCountMax)
    var randomSoundDelayEnable = MutableLiveData(prefs.randomSoundDelayEnable)
    var randomSoundDelayMin = MutableLiveData(prefs.randomSoundDelayMin)
    var randomSoundDelayMax = MutableLiveData(prefs.randomSoundDelayMax)
    var randomFireDelayEnable = MutableLiveData(prefs.randomFireDelayEnable)
    var randomFireDelayMin = MutableLiveData(prefs.randomFireDelayMin)
    var randomFireDelayMax = MutableLiveData(prefs.randomFireDelayMax)
    var randomizedSoundEnable = MutableLiveData(prefs.randomizedSoundEnable)
    var canSameSoundForMultipleSounds = MutableLiveData(prefs.canSameSoundForMultipleSounds)

    val isShowBluetoothAudio = MutableLiveData<Boolean>()

    fun updateUnitPref(unit: UnitModel, which: Int) {
        when (which) {
            1 -> prefs.unit1Model = unit
            2 -> prefs.unit2Model = unit
            3 -> prefs.unit3Model = unit
            4 -> prefs.unit4Model = unit
        }
    }

    fun updateIntervalPref(count: Int, which: Int) {
        when (which) {
            1 -> prefs.soundCount = count
            2 -> prefs.soundDelay = count
            3 -> prefs.fireDelay = count
            4 -> prefs.armedInterval = count
            5 -> prefs.autoupdatePressureDelay = count
        }
    }

    fun updateAdvancedControlIsStandardPref(enable: Boolean) {
        prefs.advancedControlIsStandardEnable = enable
    }

    fun updateAutoupdatePressureEnablePref(enable: Boolean) {
        prefs.autoupdatePressureEnable = enable
    }

    fun updateGunshotAfterLaunchEnablePref(enable: Boolean) {
        prefs.gunshotAfterLaunchEnable = enable
    }

    fun updateRandomizedSoundAndFirePref(enable: Boolean) {
        prefs.randomizedSoundAndFireEnable = enable
    }

    fun updateRandomizedSoundPref(enable: Boolean) {
        prefs.randomizedSoundEnable = enable
    }

    fun updateSameSoundForMultipleSoundsPref(enable: Boolean) {
        prefs.canSameSoundForMultipleSounds = enable
    }

    fun updateShowBluetoothAudioPref(showAudio: Boolean) {
        prefs.showBluetoothAudio = showAudio
    }

    fun updateRandomSoundEnable(isEnable: Boolean, which: Int) {
        when (which) {
            1 -> prefs.randomSoundCountEnable = isEnable
            2 -> prefs.randomSoundDelayEnable = isEnable
            3 -> prefs.randomFireDelayEnable = isEnable
        }
    }

    fun updateRandomSoundMin(count: Int, which: Int) {
        when (which) {
            1 -> prefs.randomSoundCountMin = count
            2 -> prefs.randomSoundDelayMin = count
            3 -> prefs.randomFireDelayMin = count
        }
    }

    fun updateRandomSoundMax(count: Int, which: Int) {
        when (which) {
            1 -> prefs.randomSoundCountMax = count
            2 -> prefs.randomSoundDelayMax = count
            3 -> prefs.randomFireDelayMax = count
        }
    }

    fun isSoundSelectedForRandomize(soundId: Int): Boolean {
        val selectedSoundForRandomize = prefs.selectedSoundForRandomize
        return selectedSoundForRandomize.contains(soundId.toString())
    }

    fun canInChangeNoOfChannel(): Boolean {
        val totalNoOfChannel =
            prefs.unit1Model.noOfChannel + prefs.unit2Model.noOfChannel + prefs.unit3Model.noOfChannel + prefs.unit4Model.noOfChannel
        return totalNoOfChannel < 12
    }
}