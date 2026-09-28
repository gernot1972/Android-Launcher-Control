package com.launcher_control_android.main.ui.main_configuration.view

import androidx.activity.viewModels
import androidx.lifecycle.MutableLiveData
import com.launcher_control_android.Layouts
import com.launcher_control_android.data.model.response.UnitModel
import com.launcher_control_android.databinding.ActMainConfigurationBinding
import com.launcher_control_android.databinding.LayIntervalSettingsBinding
import com.launcher_control_android.databinding.LayRandomSoundRangeBinding
import com.launcher_control_android.databinding.LayRandomSoundSwitchBinding
import com.launcher_control_android.databinding.LayUnitSettingsBinding
import android.view.View
import com.launcher_control_android.R
import com.launcher_control_android.main.base.BaseAct
import com.launcher_control_android.main.common.ApiRenderState
import com.launcher_control_android.main.ui.main_configuration.model.MainConfigurationActVM
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainConfigurationAct :
    BaseAct<ActMainConfigurationBinding, MainConfigurationActVM>(Layouts.act_main_configuration) {

    override val vm: MainConfigurationActVM by viewModels()

    override val hasProgress: Boolean = false

    override fun init() {
        setListener()
    }

    override fun onClick(v: View) {
        super.onClick(v)
        when (v.id) {
            R.id.btn_back -> {
                finish()
            }
        }
    }

    private fun setListener() {
        setUnitAction(binding.layUnit1, vm.unit1, 1)
        setUnitAction(binding.layUnit2, vm.unit2, 2)
        setUnitAction(binding.layUnit3, vm.unit3, 3)
        setUnitAction(binding.layUnit4, vm.unit4, 4)

        setIntervalAction(binding.laySoundCount, vm.noOfSoundCount, 1)
        setIntervalAction(binding.laySoundDelay, vm.delayInSound, 2)
        setIntervalAction(binding.layFireDelay, vm.delayInFire, 3)
        setIntervalAction(binding.layStayArmed, vm.armedInterval, 4)
        setIntervalAction(binding.layAutoupdateInterval, vm.autoupdatePressureDelay, 5)

        setRandomSoundRange(binding.layRandomSoundCount, vm.randomSoundCountEnable, vm.randomSoundCountMin, vm.randomSoundCountMax, 1)
        setRandomSoundRange(binding.layRandomSoundDelay, vm.randomSoundDelayEnable, vm.randomSoundDelayMin, vm.randomSoundDelayMax, 2)
        setRandomSoundRange(binding.layRandomFireDelay, vm.randomFireDelayEnable, vm.randomFireDelayMin, vm.randomFireDelayMax, 3)

        setRandomSoundSwitch(binding.layRandomSoundSwitch1, 1)
        setRandomSoundSwitch(binding.layRandomSoundSwitch2, 2)
        setRandomSoundSwitch(binding.layRandomSoundSwitch3, 3)
        setRandomSoundSwitch(binding.layRandomSoundSwitch4, 4)
        setRandomSoundSwitch(binding.layRandomSoundSwitch5, 5)
        setRandomSoundSwitch(binding.layRandomSoundSwitch6, 6)

        binding.switchAdvancedControlIsStandard.setOnClickListener {
            val updatedFlag = vm.advancedControlIsStandardEnable.value != true
            vm.advancedControlIsStandardEnable.postValue(updatedFlag)
            vm.updateAdvancedControlIsStandardPref(updatedFlag)
        }
        binding.switchAutoupdatePressure.setOnClickListener {
            val updatedFlag = vm.autoupdatePressureEnable.value != true
            vm.autoupdatePressureEnable.postValue(updatedFlag)
            vm.updateAutoupdatePressureEnablePref(updatedFlag)
        }
        binding.switchGunshotAfterLaunch.setOnClickListener {
            val updatedFlag = vm.gunshotAfterLaunchEnable.value != true
            vm.gunshotAfterLaunchEnable.postValue(updatedFlag)
            vm.updateGunshotAfterLaunchEnablePref(updatedFlag)
        }
        binding.switchRandomizeSoundAndFire.setOnClickListener {
            val updatedFlag = vm.randomizedSoundAndFireEnable.value != true
            vm.randomizedSoundAndFireEnable.postValue(updatedFlag)
            vm.updateRandomizedSoundAndFirePref(updatedFlag)
        }
        binding.switchRandomizeSound.setOnClickListener {
            val updatedFlag = vm.randomizedSoundEnable.value != true
            vm.randomizedSoundEnable.postValue(updatedFlag)
            vm.updateRandomizedSoundPref(updatedFlag)
        }
        binding.switchSameSoundForMultipleSounds.setOnClickListener {
            val updatedFlag = vm.canSameSoundForMultipleSounds.value != true
            vm.canSameSoundForMultipleSounds.postValue(updatedFlag)
            vm.updateSameSoundForMultipleSoundsPref(updatedFlag)
        }
        binding.switchShowBluetoothAudio.setOnClickListener {
            val updatedFlag = vm.isShowBluetoothAudio.value != true
            vm.isShowBluetoothAudio.postValue(updatedFlag)
            vm.updateShowBluetoothAudioPref(updatedFlag)
        }
    }

    private fun setUnitAction(unitBinding: LayUnitSettingsBinding, unit: UnitModel, which: Int) {
        unitBinding.ivMinus.setOnClickListener {
            val noOfChannel = unit.noOfChannel
            if (noOfChannel >= 2) {
                unit.noOfChannel = noOfChannel - 2
                unitBinding.unit = unit
                vm.updateUnitPref(unit, which)
            }
        }
        unitBinding.ivPlus.setOnClickListener {
            val noOfChannel = unit.noOfChannel
            if (noOfChannel <= 10) {
                unit.noOfChannel = noOfChannel + 2
                unitBinding.unit = unit
                vm.updateUnitPref(unit, which)
            }
        }
        unitBinding.switchSoundOption.setOnClickListener {
            unit.isSoundOptionInstalled = !unit.isSoundOptionInstalled
            unitBinding.unit = unit
            vm.updateUnitPref(unit, which)
        }
        unitBinding.switchServoVersion.setOnClickListener {
            unit.isServoVersion = !unit.isServoVersion
            unitBinding.unit = unit
            vm.updateUnitPref(unit, which)
        }
    }

    private fun setIntervalAction(intervalBinding: LayIntervalSettingsBinding, mutableCount: MutableLiveData<Int>, which: Int) {
        intervalBinding.ivMinus.setOnClickListener {
            val count = mutableCount.value ?: 1
            val isValidCount = when(which){
                1 -> count >= 1
                4 -> count >= 4
                5 -> count >= 6
                else -> count >=2
            }
            if (isValidCount) {
                val updatedCount = count - 1
                mutableCount.postValue(updatedCount)
                vm.updateIntervalPref(updatedCount, which)
            }
        }
        intervalBinding.ivPlus.setOnClickListener {
            val count = mutableCount.value ?: 1
            val isValidCount = when(which){
                4 -> count <= 59
                else -> count <= 9
            }
            if (isValidCount) {
                val updatedCount = count + 1
                mutableCount.postValue(updatedCount)
                vm.updateIntervalPref(updatedCount, which)
            }
        }
    }

    private fun setRandomSoundRange(randomSoundRangeBinding: LayRandomSoundRangeBinding, mutableEnable: MutableLiveData<Boolean>, mutableCountMin: MutableLiveData<Int>, mutableCountMax: MutableLiveData<Int>, which: Int) {
        randomSoundRangeBinding.switchRandomizeSound.setOnClickListener {
            val isEnable = randomSoundRangeBinding.switchRandomizeSound.isChecked
            mutableEnable.postValue(isEnable)
            vm.updateRandomSoundEnable(isEnable, which)
        }

        randomSoundRangeBinding.ivMinusMin.setOnClickListener {
            val count = mutableCountMin.value ?: 1
            val isValidCount = when(which){
                1 -> count >= 2
                else -> count >= 3
            }
            if (isValidCount) {
                val updatedCount = count - 1
                mutableCountMin.postValue(updatedCount)
                vm.updateRandomSoundMin(updatedCount, which)
            }
        }
        randomSoundRangeBinding.ivPlusMin.setOnClickListener {
            val count = mutableCountMin.value ?: 1
            val isValidCount = when(which){
                1 -> count <= 4
                else -> count <= 9
            }
            if (isValidCount) {
                val updatedCount = count + 1
                mutableCountMin.postValue(updatedCount)
                vm.updateRandomSoundMin(updatedCount, which)

                if (updatedCount > (mutableCountMax.value ?: 1)) {
                    mutableCountMax.postValue(updatedCount)
                    vm.updateRandomSoundMax(updatedCount, which)
                }
            }
        }

        randomSoundRangeBinding.ivMinusMax.setOnClickListener {
            val count = mutableCountMax.value ?: 1
            val isValidCount = when(which){
                1 -> count >= 2
                else -> count >= 3
            }
            if (isValidCount) {
                val updatedCount = count - 1
                mutableCountMax.postValue(updatedCount)
                vm.updateRandomSoundMax(updatedCount, which)

                if (updatedCount < (mutableCountMin.value ?: 1)) {
                    mutableCountMin.postValue(updatedCount)
                    vm.updateRandomSoundMin(updatedCount, which)
                }
            }
        }
        randomSoundRangeBinding.ivPlusMax.setOnClickListener {
            val count = mutableCountMax.value ?: 1
            val isValidCount = when(which){
                1 -> count <= 4
                else -> count <= 9
            }
            if (isValidCount) {
                val updatedCount = count + 1
                mutableCountMax.postValue(updatedCount)
                vm.updateRandomSoundMax(updatedCount, which)
            }
        }
    }

    private fun setRandomSoundSwitch(soundSwitchBinding: LayRandomSoundSwitchBinding, which: Int) {
        val soundIndex = which - 1
        soundSwitchBinding.switchRandomizeSound.isChecked = vm.isSoundSelectedForRandomize(soundIndex)
        soundSwitchBinding.switchRandomizeSound.setOnClickListener {
            val isEnable = soundSwitchBinding.switchRandomizeSound.isChecked
            val setOfSound = prefs.selectedSoundForRandomize
            if (isEnable) {
                setOfSound.add(soundIndex.toString())
            } else {
                setOfSound.remove(soundIndex.toString())
            }
            prefs.selectedSoundForRandomize = setOfSound
        }
    }

    override fun renderState(apiRenderState: ApiRenderState) {

    }
}