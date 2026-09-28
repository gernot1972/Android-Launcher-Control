package com.launcher_control_android.main.ui.unit_detail.view

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.graphics.Color
import android.os.Build
import android.view.View
import androidx.activity.viewModels
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.databinding.ObservableField
import androidx.lifecycle.lifecycleScope
import com.launcher_control_android.AppConstants
import com.launcher_control_android.Drawables
import com.launcher_control_android.Layouts
import com.launcher_control_android.R
import com.launcher_control_android.Strings
import com.launcher_control_android.data.model.response.UnitModel
import com.launcher_control_android.databinding.ActUnitDetailBinding
import com.launcher_control_android.helper.bluetooth.communication.BluetoothCommunicationAct
import com.launcher_control_android.helper.util.getVoltageImageResId
import com.launcher_control_android.helper.util.startActivity
import com.launcher_control_android.main.common.ApiRenderState
import com.launcher_control_android.main.common.FetchedChannelModel
import com.launcher_control_android.main.ui.channel_list.view.ChannelListAct
import com.launcher_control_android.main.ui.unit_detail.model.UnitDetailActVM
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.milliseconds


@AndroidEntryPoint
class UnitDetailAct :
    BluetoothCommunicationAct<ActUnitDetailBinding, UnitDetailActVM>(Layouts.act_unit_detail) {

    private val showSecondaryProgress = ObservableField(false)

    private var animatorSet: AnimatorSet? = null

    override val hasProgress: Boolean = true

    override val vm: UnitDetailActVM by viewModels()

    override fun init() {
        binding.showSecondaryProgress = showSecondaryProgress
        deviceAddress = vm.savedBluetoothDevice.value?.address ?: ""
        checkIntent()
        setListener()
        binding.ivSound.setOnLongClickListener {
            showDialogFrag(
                SoundOptionsBsd.newInstance(vm.selectedUnitModel.value?.selectedSound ?: 0) {
                    vm.selectedUnitModel.value?.selectedSound = it
                },
            )
            true
        }

        binding.ivLauncher.setOnLongClickListener {
            fireSoundAndChannel()
            true
        }
        setupBlinkAnimation()
    }

    private fun checkIntent() {
        val unitModel = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra(
                AppConstants.Communication.BundleData.INTENT_UNIT_MODEL,
                UnitModel::class.java
            )
        } else {
            intent.getSerializableExtra(AppConstants.Communication.BundleData.INTENT_UNIT_MODEL) as? UnitModel
        }
        vm.selectedUnitModel.postValue(unitModel)
        fetchUnitData(unitModel)
    }

    private fun setListener() {
        binding.tv1.setOnLongClickListener {
            val bundle =
                bundleOf(AppConstants.Communication.BundleData.INTENT_UNIT_MODEL to prefs.unit1Model)
            startActivity(ChannelListAct::class.java, bundle = bundle)
            false
        }

        binding.tv2.setOnLongClickListener {
            val bundle =
                bundleOf(AppConstants.Communication.BundleData.INTENT_UNIT_MODEL to prefs.unit2Model)
            startActivity(ChannelListAct::class.java, bundle = bundle)
            false
        }

        binding.tv3.setOnLongClickListener {
            val bundle =
                bundleOf(AppConstants.Communication.BundleData.INTENT_UNIT_MODEL to prefs.unit3Model)
            startActivity(ChannelListAct::class.java, bundle = bundle)
            false
        }

        binding.tv4.setOnLongClickListener {
            val bundle =
                bundleOf(AppConstants.Communication.BundleData.INTENT_UNIT_MODEL to prefs.unit4Model)
            startActivity(ChannelListAct::class.java, bundle = bundle)
            false
        }
    }

    private fun setupBlinkAnimation() {
        val anim1 = ObjectAnimator.ofArgb(binding.tvFire, "textColor", Color.BLACK, Color.RED)
        val anim2 = ObjectAnimator.ofArgb(binding.tvChannel, "textColor", Color.BLACK, Color.RED)
        val anim3 = ObjectAnimator.ofArgb(binding.tvChannelNo, "textColor", Color.BLACK, Color.RED)

        anim1.repeatCount = ObjectAnimator.INFINITE
        anim2.repeatCount = ObjectAnimator.INFINITE
        anim3.repeatCount = ObjectAnimator.INFINITE

        anim1.repeatMode = ObjectAnimator.REVERSE
        anim2.repeatMode = ObjectAnimator.REVERSE
        anim3.repeatMode = ObjectAnimator.REVERSE

        anim1.setDuration(500)
        anim2.setDuration(500)
        anim3.setDuration(500)

        animatorSet = AnimatorSet()
        animatorSet?.playTogether(anim1, anim2, anim3)
    }

    private fun startBlinkAnimation() {
        if (binding.tvChannel.isVisible && (animatorSet != null) && !(animatorSet!!.isStarted)) {
            animatorSet!!.start()
        }
    }

    private fun stopBlinkAnimation() {
        if (animatorSet?.isStarted == true) {
            animatorSet!!.cancel()
        }
    }

    override fun renderState(apiRenderState: ApiRenderState) {

    }

    override fun onClick(v: View) {
        super.onClick(v)
        when (v.id) {
            R.id.btn_back -> {
                finish()
            }

            R.id.tv_1 -> {
                if (isThisUnitSelected(1)) {
                    finishAct()
                    return
                }
                vm.selectedUnitModel.postValue(prefs.unit1Model)
                fetchUnitData(prefs.unit1Model)
            }

            R.id.tv_2 -> {
                if (isThisUnitSelected(2)) {
                    finishAct()
                    return
                }
                vm.selectedUnitModel.postValue(prefs.unit2Model)
                fetchUnitData(prefs.unit2Model)
            }

            R.id.tv_3 -> {
                if (isThisUnitSelected(3)) {
                    finishAct()
                    return
                }
                vm.selectedUnitModel.postValue(prefs.unit3Model)
                fetchUnitData(prefs.unit3Model)
            }

            R.id.tv_4 -> {
                if (isThisUnitSelected(4)) {
                    finishAct()
                    return
                }
                vm.selectedUnitModel.postValue(prefs.unit4Model)
                fetchUnitData(prefs.unit4Model)
            }

            R.id.iv_launcher -> {
                if (vm.fetchedUnitModel != null) {
                    fireSelectedChannel()
                }
            }

            R.id.iv_voltage -> {
                lifecycleScope.launch {
                    setVoltageVisibility(iconVisible = false)
                    delay(2000.milliseconds)
                    setVoltageVisibility(iconVisible = true)
                }
            }

            R.id.iv_refresh -> {
                reloadUnitData()
            }

            R.id.iv_suffering -> {
                fetchUnitData(vm.selectedUnitModel.value)
            }

            R.id.iv_sound -> {
                fireSoundCommand()
            }
        }
    }

    private fun isThisUnitSelected(unitNumber: Int): Boolean {
        return vm.selectedUnitModel.value?.unitNumber == unitNumber
    }

    override fun onCharacteristicChanged(
        gatt: BluetoothGatt?,
        characteristic: BluetoothGattCharacteristic?,
        value: ByteArray
    ) {
        super.onCharacteristicChanged(gatt, characteristic, value)
        runOnUiThread {
            val response = value.decodeToString().lowercase()
            val command = bluetoothService?.waitingForRes
            val unit = getWaitingForResUnit() ?: return@runOnUiThread
            if (response.startsWith("u", ignoreCase = true)) {
                vm.fetchedUnitModel = FetchedChannelModel(response)
                selectNextChannel()
                showUnitDataFetchedUI()
            } else if (response.startsWith("v", ignoreCase = true)) {
                vm.fetchedUnitVoltage = response.replace("v", "", ignoreCase = true)
                setVoltage()
                setVoltageVisibility()
            } else if (vm.selectedUnitModel.value?.isSoundCommand(bluetoothService?.waitingForRes) == true) {
                showToast(String.format(getString(Strings.signal_sent_to_unit), vm.selectedUnitModel.value?.unitNumber))
            } else {
                if (response == AppConstants.CommandResponse.NO_RESPONSE || response == AppConstants.CommandResponse.NO_REPLY) {
                    showDialog(
                        title = getString(Strings.no_response),
                        message = String.format(getString(Strings.no_response_msg), unit)
                    ) {
                        reloadUnitData()
                    }
                } else if (AppConstants.CommandResponse.isSuccess(response)) {
                    if (vm.selectedUnitModel.value?.isReloadCommand(command) == true) {
                        vm.fetchedUnitModel?.doReload()
                        selectNextChannel()
                        showUnitDataFetchedUI()
                        showToast(Strings.unit_successfully_initialized)
                    } else {
                        val channel = vm.currentFiredChannel ?: vm.fetchedUnitModel?.getNextChannel()
                        if (channel != null) {
                            vm.fetchedUnitModel?.markChannelAsFire(channel)
                            showToast(String.format(getString(Strings.channel_fired_successfully), channel))
                            selectNextChannel()
                        }
                        vm.currentFiredChannel = null
                    }
                }
            }
        }
    }

    private fun showUnitDataFetchedUI() {
        binding.bgChannelConnected.isVisible = true
        binding.ivLauncher.setImageResource(Drawables.ic_red_freesbi)
        binding.tvFire.isVisible = true
        binding.tvChannel.isVisible = true
        binding.tvChannelNo.isVisible = true
        binding.tvBar.isVisible = vm.selectedUnitModel.value?.isServoVersion != true
        binding.tvBarNo.isVisible = vm.selectedUnitModel.value?.isServoVersion != true
        binding.tvBarNo.text = vm.fetchedUnitModel?.getBar()?.toString()
        startBlinkAnimation()
    }

    private fun selectNextChannel() {
        val nextAvailableChannel = vm.fetchedUnitModel?.getNextChannel()
        val noOfChannelAdded = vm.selectedUnitModel.value?.noOfChannel ?: 0
        if (nextAvailableChannel != null && nextAvailableChannel <= noOfChannelAdded) {
            binding.tvChannelNo.text = nextAvailableChannel.toString()
        } else {
            showDialog(
                title = getString(Strings.reload_unit),
                message = String.format(
                    getString(Strings.no_channel_available_msg),
                    vm.fetchedUnitModel?.getUnit()
                ),
                onPositiveClick = {
                    reloadUnitData()
                }
            )
        }
    }

    private fun fetchUnitData(unitModel: UnitModel?) {
        vm.resetFetchedUnit()
        hideUnitDataFetchedUI()
        val haxCode = unitModel?.fetchDataHexCode() ?: return
        sendCommand(haxCode)
    }

    private fun reloadUnitData() {
        if (vm.selectedUnitModel.value == null) return
        hideUnitDataFetchedUI()
        val haxCode = vm.selectedUnitModel.value?.reloadHexCode() ?: return
        sendCommand(haxCode)
    }

    private fun fireSelectedChannel() {
        val unit = vm.fetchedUnitModel?.getUnit()
        val channel = vm.fetchedUnitModel?.getNextChannel()
        val haxCode = vm.selectedUnitModel.value?.unitChannelHexCode(
            unit = unit,
            channel = channel
        ) ?: return
        sendCommand(haxCode)
        vm.currentFiredChannel = channel
    }

    private fun fireSoundCommand() {
        val haxCode = vm.selectedUnitModel.value?.soundHexCode() ?: return
        sendCommand(haxCode)
    }

    private fun fireSoundAndChannel() {
        lifecycleScope.launch {
            if (vm.fetchedUnitModel != null) {
                withContext(Dispatchers.Main) {
                    showSecondaryProgress.set(true)
                    fireSoundCommand()
                }
                repeat(prefs.soundCount - 1) {
                    delay((prefs.soundDelay * 1000L))
                    withContext(Dispatchers.Main) {
                        fireSoundCommand()
                    }
                }
                delay((prefs.fireDelay * 1000L))
                withContext(Dispatchers.Main) {
                    fireSelectedChannel()
                    showSecondaryProgress.set(false)
                }
            }
        }
    }

    private fun setVoltage() {
        binding.tvVoltage.text = getString(R.string.device_voltage, "${vm.fetchedUnitVoltage}V")
        vm.fetchedUnitVoltage?.toDoubleOrNull()?.let {
            binding.ivVoltage.setImageResource(getVoltageImageResId(it) )
        }
    }

    private fun setVoltageVisibility(iconVisible: Boolean = true) {
        val isVoltageVisible = !vm.fetchedUnitVoltage.isNullOrBlank()
        binding.tvVoltage.isVisible = !iconVisible && isVoltageVisible
        vm.fetchedUnitVoltage?.toDoubleOrNull()?.let {
            binding.ivVoltage.isVisible = iconVisible && isVoltageVisible
        }
    }

    private fun hideUnitDataFetchedUI() {
        binding.bgChannelConnected.isVisible = false
        binding.ivLauncher.setImageResource(Drawables.ic_green_freesbi)
        binding.tvFire.isVisible = false
        binding.tvChannel.isVisible = false
        binding.tvChannelNo.isVisible = false
        binding.tvBar.isVisible = false
        binding.tvBarNo.isVisible = false
        binding.tvVoltage.isVisible = false
        binding.ivVoltage.isVisible = false
        stopBlinkAnimation()
    }

    override fun onPause() {
        super.onPause()
        stopBlinkAnimation()
    }

    override fun onResume() {
        super.onResume()
        startBlinkAnimation()
    }
}