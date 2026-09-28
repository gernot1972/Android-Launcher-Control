package com.launcher_control_android.main.ui.channel_list.view

import android.animation.ObjectAnimator
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.os.Build
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.databinding.ObservableField
import androidx.lifecycle.lifecycleScope
import com.launcher_control_android.AppConstants
import com.launcher_control_android.AppConstants.Command.listOfFetchDataCommand
import com.launcher_control_android.AppConstants.Command.listOfTestCommand
import com.launcher_control_android.Layouts
import com.launcher_control_android.R
import com.launcher_control_android.Strings
import com.launcher_control_android.data.model.response.UnitModel
import com.launcher_control_android.databinding.ActChannelListBinding
import com.launcher_control_android.helper.bluetooth.communication.BluetoothCommunicationAct
import com.launcher_control_android.helper.util.animateHorizontalFlip
import com.launcher_control_android.helper.util.animateRotate
import com.launcher_control_android.helper.util.animateWave
import com.launcher_control_android.helper.util.showAlertDialog
import com.launcher_control_android.helper.util.startActivityForResult
import com.launcher_control_android.helper.util.triggerHaptic
import com.launcher_control_android.main.common.ApiRenderState
import com.launcher_control_android.main.common.FetchedChannelModel
import com.launcher_control_android.main.ui.channel_list.model.ChannelListActVM
import com.launcher_control_android.main.ui.home.view.PressureOptionsBsd
import com.launcher_control_android.main.ui.main_configuration.view.MainConfigurationAct
import com.launcher_control_android.main.ui.unit_detail.view.SoundOptionsBsd
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random

@AndroidEntryPoint
class ChannelListAct :
    BluetoothCommunicationAct<ActChannelListBinding, ChannelListActVM>(Layouts.act_channel_list) {

    private val showSecondaryProgress = ObservableField(false)

    override val hasProgress: Boolean = true

    override val vm: ChannelListActVM by viewModels()

    private var reloadBtnBlinkAnimator: ObjectAnimator? = null
    private var updateBtnBlinkAnimator: ObjectAnimator? = null
    private var autoupdateJob: Job? = null
    private var stayArmedTimerJob: Job? = null
    private var remainingDisarmSeconds = 0

    private fun resetStayArmed() {
        stayArmedTimerJob?.cancel()
        remainingDisarmSeconds = prefs.armedInterval
        stayArmedTimerJob = lifecycleScope.launch(Dispatchers.Main) {
            while (remainingDisarmSeconds > 0) {
                binding.tvDisarmTimer.text = "DISARM IN ${remainingDisarmSeconds}s"
                binding.tvDisarmTimer.setTextColor(
                    if (remainingDisarmSeconds <= 5) getColor(R.color.colorOrange) else getColor(R.color.white)
                )
                delay(1000L)
                remainingDisarmSeconds--
            }
            finish()
        }
    }

    private fun updateTileSizesAndMargins(noOfChannels: Int) {
        val density = resources.displayMetrics.density

        // 1.1 Kachelgröße: 68dp bei 1–8 Kanälen, sonst 60dp
        val sizeDp = if (noOfChannels in 1..8) 68 else 60
        val sizePx = (sizeDp * density).toInt()

        // 1.2 Reihenabstand bei 2–6 Kanälen auf 20dp erhöhen (sonst 12dp)
        val rowMarginDp = if (noOfChannels in 2..6) 20 else 12
        val rowMarginPx = (rowMarginDp * density).toInt()

        val oddTiles = listOf(
            binding.tv1, binding.tv3, binding.tv5,
            binding.tv7, binding.tv9, binding.tv11
        )
        val evenTiles = listOf(
            binding.tv2, binding.tv4, binding.tv6,
            binding.tv8, binding.tv10, binding.tv12
        )

        // Kachelgrößen für alle 12 Kacheln setzen (68dp bei 1-8 Kanälen, sonst 60dp)
        (oddTiles + evenTiles).forEach { tv ->
            tv.layoutParams = tv.layoutParams.apply {
                width = sizePx
                height = sizePx
            }
        }

        // Vertikalen Reihenabstand (20dp bei 2-6 Kanälen) NUR auf ungerade Kacheln anwenden
        oddTiles.forEach { tv ->
            (tv.layoutParams as? androidx.constraintlayout.widget.ConstraintLayout.LayoutParams)?.let { lp ->
                lp.bottomMargin = rowMarginPx
                lp.goneBottomMargin = rowMarginPx
                tv.layoutParams = lp
            }
        }

        // Gerade Kacheln richten sich rein an den ungeraden aus (kein bottomMargin)
        evenTiles.forEach { tv ->
            (tv.layoutParams as? androidx.constraintlayout.widget.ConstraintLayout.LayoutParams)?.let { lp ->
                lp.bottomMargin = 0
                lp.goneBottomMargin = 0
                tv.layoutParams = lp
            }
        }

        // 2. Spezifische Abstände ausschließlich bei exakt 12 Kanälen verkleinern
        val is12Channels = (noOfChannels == 12)

        if (is12Channels) {
            (binding.tv11.layoutParams as? androidx.constraintlayout.widget.ConstraintLayout.LayoutParams)?.let { lp ->
                lp.bottomMargin = (2 * density).toInt()
                lp.goneBottomMargin = (2 * density).toInt()
                binding.tv11.layoutParams = lp
            }
        }

        // Abstand 2: Oberes Padding bei 2dp halten, unteres Padding auf 12dp erhöhen
        val paddingTopPx = ((if (is12Channels) 2 else 10) * density).toInt()
        val paddingBottomPx = ((if (is12Channels) 12 else 18) * density).toInt()

        binding.btnContainer.setPadding(
            binding.btnContainer.paddingLeft,
            paddingTopPx,
            binding.btnContainer.paddingRight,
            paddingBottomPx
        )
    }

    override fun init() {
        binding.showSecondaryProgress = showSecondaryProgress
        binding.toolbar.textView.visibility = View.GONE // 🎯 Blendet das Textfeld dauerhaft aus (geschützt vor DataBinding invalidateAll)
        deviceAddress = vm.savedBluetoothDevice.value?.address ?: ""
        checkIntent()
        setListener()
        resetStayArmed()

        // Dynamische Anpassung von Kachelgrößen (68dp bei 1-8 Kanälen) und Abständen (bei 12 Kanälen)
        vm.selectedUnitModel.observe(this) { unitModel ->
            updateTileSizesAndMargins(unitModel?.noOfChannel ?: 0)
            updateBatteryAndVolumeUI()
        }
        setupVolumeTouchListener()
    }

    private var fanRotateAnimator: ObjectAnimator? = null
    private var leftIconFailBlinkAnimator: ObjectAnimator? = null

    private fun startLeftIconFailBlinkAnimation() {
        if (leftIconFailBlinkAnimator == null || !leftIconFailBlinkAnimator!!.isStarted) {
            leftIconFailBlinkAnimator = ObjectAnimator.ofFloat(binding.ivLeftStatusIcon, "alpha", 1f, 0.2f, 1f).apply {
                duration = 300
                repeatCount = ObjectAnimator.INFINITE
                repeatMode = ObjectAnimator.RESTART
                start()
            }
        }
    }

    private fun stopLeftIconFailBlinkAnimation() {
        leftIconFailBlinkAnimator?.cancel()
        leftIconFailBlinkAnimator = null
        binding.ivLeftStatusIcon.alpha = 1.0f
    }

    private fun updateLeftTelemetryPanel() {
        val iconResId = vm.getLeftStatusIconResId()
        val iconColor = getColor(vm.getLeftStatusIconColor())
        val statusText = vm.getLeftStatusText()

        // 🎯 1:1 Simple View Logik: Dynamisches Icon, Farbtint und Statustext
        binding.ivLeftStatusIcon.setImageResource(iconResId)
        binding.ivLeftStatusIcon.imageTintList = android.content.res.ColorStateList.valueOf(iconColor)

        binding.tvLeftStatusText.text = statusText
        binding.tvLeftStatusText.setTextColor(iconColor)

        // 🎯 1:1 Simple View Logik: Blinken bei System-FAIL
        if (vm.fetchedUnitModel?.isBarFail() == true) {
            startLeftIconFailBlinkAnimation()
        } else {
            stopLeftIconFailBlinkAnimation()
        }

        // 🎯 1:1 Simple View Logik: Lüfter-Rotation bei aktiver Kompressor-Fahrt ("ON")
        if (vm.isCompressorActive() && !vm.isServoON() && !vm.isSoundOnlyMode()) {
            if (fanRotateAnimator == null || !fanRotateAnimator!!.isStarted) {
                fanRotateAnimator = ObjectAnimator.ofFloat(binding.ivLeftStatusIcon, "rotation", 0f, 360f).apply {
                    duration = 1200
                    repeatCount = ObjectAnimator.INFINITE
                    interpolator = android.view.animation.LinearInterpolator()
                    start()
                }
            }
        } else {
            fanRotateAnimator?.cancel()
            fanRotateAnimator = null
            binding.ivLeftStatusIcon.rotation = 0f
        }
    }

    // 🎯 Steuerung der 14-Stufen Akku-Stele & 4-Stufen Lautstärkestele in Code
    // 🎯 Steuerung der 14-Stufen Akku-Stele & 4-Stufen Lautstärkestele in Code
    private fun updateBatteryAndVolumeUI() {
        updateLeftTelemetryPanel()
        updateBottomButtonsUI() // 🎯 1:1 Simple View: Farben, Sperren & Blinken der unteren Buttons steuern
        val batteryHex = vm.fetchedUnitModel?.getBatteryHex()
        binding.tvBatPercent.text = vm.getBatteryPercentText()

        val (activeBars, color) = when (batteryHex) {
            15 -> Pair(14, getColor(R.color.colorGreen))
            14 -> Pair(0, getColor(R.color.colorRed)) // SENSOR FAIL
            13 -> Pair(14, getColor(R.color.colorGreen))
            12 -> Pair(13, getColor(R.color.colorGreen))
            11 -> Pair(12, getColor(R.color.colorGreen))
            10 -> Pair(11, getColor(R.color.colorGreen))
            9  -> Pair(10, getColor(R.color.colorGreen))
            8  -> Pair(9, getColor(R.color.colorGreen))
            7  -> Pair(8, getColor(R.color.colorGreen))
            6  -> Pair(7, getColor(R.color.colorGreen))
            5  -> Pair(6, getColor(R.color.colorGreen))
            4  -> Pair(5, getColor(R.color.colorGreen))
            3  -> Pair(4, getColor(R.color.colorOrange))
            2  -> Pair(3, getColor(R.color.colorOrange))
            1  -> Pair(2, getColor(R.color.colorRed))
            0  -> Pair(1, getColor(R.color.colorRed))
            else -> Pair(0, getColor(R.color.white)) // Neutral bei alten Geräten (--%)
        }

        val batSegments = listOf(
            binding.batSeg0, binding.batSeg1, binding.batSeg2, binding.batSeg3,
            binding.batSeg4, binding.batSeg5, binding.batSeg6, binding.batSeg7,
            binding.batSeg8, binding.batSeg9, binding.batSeg10, binding.batSeg11,
            binding.batSeg12, binding.batSeg13
        )
        batSegments.forEachIndexed { index, view ->
            view.backgroundTintList = android.content.res.ColorStateList.valueOf(
                if (index < activeBars) color else android.graphics.Color.parseColor("#99999999")
            )
        }

        // 🎯 Lautstärke-Stele (Stufen 1..4 farblich hinterlegen)
        val volBgs = listOf(binding.volBg1, binding.volBg2, binding.volBg3, binding.volBg4)
        volBgs.forEachIndexed { index, view ->
            val isSelected = (index + 1) <= vm.selectedVolumeStep
            view.backgroundTintList = android.content.res.ColorStateList.valueOf(
                if (isSelected) getColor(R.color.colorGreen) else android.graphics.Color.parseColor("#99999999")
            )
        }
    }

    // 🎯 Touch- & Drag-Geste für die Lautstärkestele (1:1 wie iOS StatusView)
    @android.annotation.SuppressLint("ClickableViewAccessibility")
    private fun setupVolumeTouchListener() {
        binding.llVolSegments.setOnTouchListener { v, event ->
            if (event.action == android.view.MotionEvent.ACTION_DOWN || event.action == android.view.MotionEvent.ACTION_MOVE) {
                val height = v.height.toFloat()
                if (height > 0) {
                    val normalizedY = 1.0f - Math.max(0.0f, Math.min(1.0f, event.y / height))
                    val calculatedStep = Math.max(1, Math.min(4, Math.ceil((normalizedY * 4.0f).toDouble()).toInt()))
                    if (vm.selectedVolumeStep != calculatedStep) {
                        vm.selectedVolumeStep = calculatedStep
                        v.triggerHaptic()
                        updateBatteryAndVolumeUI()
                    }
                }
                resetStayArmed()
                true
            } else false
        }
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
        vm.selectedUnitModel.value = unitModel
        fetchUnitData(unitModel)
    }

    private val btnSoundLongClick = View.OnLongClickListener {
        it.triggerHaptic()
        resetStayArmed()
        showDialogFrag(
            SoundOptionsBsd.newInstance(vm.selectedUnitModel.value?.selectedSound ?: 0) {
                vm.setSoundForSelectedUnit(it)
                binding.invalidateAll()
            },
        )
        true
    }

    private val btnUpdateLongClick = View.OnLongClickListener {
        it.triggerHaptic()
        resetStayArmed()
        if (vm.selectedUnitModel.value?.isServoVersion != true) {
            showDialogFrag(
                PressureOptionsBsd.newInstance(vm.selectedUnitModel.value?.selectedPressure ?: 0) {
                    vm.setPressureForSelectedUnit(it)
                    vm.selectedUnitModel.value?.pressureHaxCode(it)?.let { hexCode ->
                        sendCommand(hexCode)
                    }
                },
            )
        }
        true
    }

    private fun setListener() {
        binding.btnSound.setOnLongClickListener(btnSoundLongClick)
        binding.btnUpdate.setOnLongClickListener(btnUpdateLongClick)
        binding.btnSoundLarge.setOnLongClickListener(btnSoundLongClick)
        binding.btnUpdateLarge.setOnLongClickListener(btnUpdateLongClick)

        binding.tv1.setOnLongClickListener {
            it.triggerHaptic()
            fireSoundAndChannel(1)
            true
        }

        binding.tv2.setOnLongClickListener {
            it.triggerHaptic()
            fireSoundAndChannel(2)
            true
        }

        binding.tv3.setOnLongClickListener {
            it.triggerHaptic()
            fireSoundAndChannel(3)
            true
        }

        binding.tv4.setOnLongClickListener {
            it.triggerHaptic()
            fireSoundAndChannel(4)
            true
        }

        binding.tv5.setOnLongClickListener {
            it.triggerHaptic()
            fireSoundAndChannel(5)
            true
        }

        binding.tv6.setOnLongClickListener {
            it.triggerHaptic()
            fireSoundAndChannel(6)
            true
        }

        binding.tv7.setOnLongClickListener {
            it.triggerHaptic()
            fireSoundAndChannel(7)
            true
        }

        binding.tv8.setOnLongClickListener {
            it.triggerHaptic()
            fireSoundAndChannel(8)
            true
        }

        binding.tv9.setOnLongClickListener {
            it.triggerHaptic()
            fireSoundAndChannel(9)
            true
        }

        binding.tv10.setOnLongClickListener {
            it.triggerHaptic()
            fireSoundAndChannel(10)
            true
        }

        binding.tv11.setOnLongClickListener {
            it.triggerHaptic()
            fireSoundAndChannel(11)
            true
        }

        binding.tv12.setOnLongClickListener {
            it.triggerHaptic()
            fireSoundAndChannel(12)
            true
        }

        // GEÄNDERT: Schickt die spezifisch langgedrückte Unit an die Simple View zurück
        binding.tvUnit1.setOnLongClickListener {
            it.triggerHaptic()
            handleUnitLongClick(prefs.unit1Model)
            true
        }
        binding.tvUnit2.setOnLongClickListener {
            it.triggerHaptic()
            handleUnitLongClick(prefs.unit2Model)
            true
        }
        binding.tvUnit3.setOnLongClickListener {
            it.triggerHaptic()
            handleUnitLongClick(prefs.unit3Model)
            true
        }
        binding.tvUnit4.setOnLongClickListener {
            it.triggerHaptic()
            handleUnitLongClick(prefs.unit4Model)
            true
        }
    }

    // GEÄNDERT: Hilfsmethode zum Zurückgeben des Unit-Modells an LauncherControlAct
    private fun handleUnitLongClick(unitModel: UnitModel?) {
        val intent = android.content.Intent().apply {
            putExtra(AppConstants.Communication.BundleData.INTENT_UNIT_MODEL, unitModel)
        }
        setResult(RESULT_OK, intent)
        finish()
    }

    override fun renderState(apiRenderState: ApiRenderState) {
    }

    private val settingActResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { _ ->
        vm.refreshSelectedUnit()
        binding.invalidateAll()
        runAutoUpdate()
    }

    private fun runAutoUpdate() {
        if (vm.selectedUnitModel.value?.isChannelAdded() == true) {
            autoupdateJob?.cancel()
            if (prefs.autoupdatePressureEnable) {
                autoupdateJob = lifecycleScope.launch {
                    delay((prefs.autoupdatePressureDelay * 1000L))
                    if (!vm.isAutoUpdateOnPause &&
                        vm.selectedUnitModel.value != null &&
                        vm.selectedUnitModel.value?.isServoVersion != true &&
                        vm.selectedUnitModel.value?.isOnlySoundInstalled() != true && // 🎯 Deaktiviert Autoupdate für Soundunits
                        vm.selectedUnitModel.value?.isChannelAdded() == true &&
                        vm.getNextAvailableChannel() != null) {
                        fetchUnitData(vm.selectedUnitModel.value, true)
                    }
                }
            } else autoupdateJob?.cancel()
        } else {
            autoupdateJob?.cancel()
        }
    }

    override fun onClick(v: View) {
        super.onClick(v)
        v.triggerHaptic()
        resetStayArmed()
        when (v.id) {
            R.id.btn_back -> {
                finish()
            }

            R.id.btn_setting, R.id.tv_set_up_unit_info -> {
                startActivityForResult(MainConfigurationAct::class.java, settingActResultLauncher)
            }

            R.id.tv_autoupdate_pressure_status -> {
                vm.toggleAutoupdatePressureEnable()
                runAutoUpdate()
            }

            R.id.tv_1 -> {
                fireChannel(1)
            }

            R.id.tv_2 -> {
                fireChannel(2)
            }

            R.id.tv_3 -> {
                fireChannel(3)
            }

            R.id.tv_4 -> {
                fireChannel(4)
            }

            R.id.tv_5 -> {
                fireChannel(5)
            }

            R.id.tv_6 -> {
                fireChannel(6)
            }

            R.id.tv_7 -> {
                fireChannel(7)
            }

            R.id.tv_8 -> {
                fireChannel(8)
            }

            R.id.tv_9 -> {
                fireChannel(9)
            }

            R.id.tv_10 -> {
                fireChannel(10)
            }

            R.id.tv_11 -> {
                fireChannel(11)
            }

            R.id.tv_12 -> {
                fireChannel(12)
            }

            R.id.btn_reload_large -> {
                binding.ivReloadLarge.animateRotate()
                reloadUnitData()
            }

            R.id.btn_update_large -> {
                binding.ivUpdateLarge.animateHorizontalFlip()
                fetchUnitData(vm.selectedUnitModel.value)
            }

            R.id.btn_sound_large -> {
                binding.ivSoundLarge.animateWave()
                fireSoundCommand()
            }

            R.id.btn_reload -> {
                v.animateRotate()
                reloadUnitData()
            }

            R.id.btn_update -> {
                v.animateHorizontalFlip()
                fetchUnitData(vm.selectedUnitModel.value)
            }

            R.id.btn_sound -> {
                v.animateWave()
                fireSoundCommand()
            }

            R.id.btn_sound_1 -> {
                binding.ivLeftStatusIcon.animateWave()
                fireSoundCommand(0)
                resetStayArmed()
            }

            R.id.btn_sound_2 -> {
                binding.ivLeftStatusIcon.animateWave()
                fireSoundCommand(1)
                resetStayArmed()
            }

            R.id.btn_sound_3 -> {
                binding.ivLeftStatusIcon.animateWave()
                fireSoundCommand(2)
                resetStayArmed()
            }

            R.id.btn_sound_4 -> {
                binding.ivLeftStatusIcon.animateWave()
                fireSoundCommand(3)
                resetStayArmed()
            }

            R.id.btn_sound_5 -> {
                binding.ivLeftStatusIcon.animateWave()
                fireSoundCommand(4)
                resetStayArmed()
            }

            R.id.btn_sound_6 -> {
                binding.ivLeftStatusIcon.animateWave()
                fireSoundCommand(5)
                resetStayArmed()
            }

            R.id.tv_unit_1 -> {
                val unitModel = prefs.unit1Model
                if (vm.selectedUnitModel.value?.unitNumber != unitModel.unitNumber) {
                    vm.selectedUnitModel.value = unitModel
                    fetchUnitData(unitModel)
                }
                resetStayArmed()
            }

            R.id.tv_unit_2 -> {
                val unitModel = prefs.unit2Model
                if (vm.selectedUnitModel.value?.unitNumber != unitModel.unitNumber) {
                    vm.selectedUnitModel.value = unitModel
                    fetchUnitData(unitModel)
                }
                resetStayArmed()
            }

            R.id.tv_unit_3 -> {
                val unitModel = prefs.unit3Model
                if (vm.selectedUnitModel.value?.unitNumber != unitModel.unitNumber) {
                    vm.selectedUnitModel.value = unitModel
                    fetchUnitData(unitModel)
                }
                resetStayArmed()
            }

            R.id.tv_unit_4 -> {
                val unitModel = prefs.unit4Model
                if (vm.selectedUnitModel.value?.unitNumber != unitModel.unitNumber) {
                    vm.selectedUnitModel.value = unitModel
                    fetchUnitData(unitModel)
                }
                resetStayArmed()
            }
        }
    }

    override fun onCharacteristicWrite(
        gatt: BluetoothGatt?,
        characteristic: BluetoothGattCharacteristic?,
        isSuccess: Boolean
    ) {
        super.onCharacteristicWrite(gatt, characteristic, isSuccess)
    }

    override fun onCharacteristicRead(
        gatt: BluetoothGatt?,
        characteristic: BluetoothGattCharacteristic?,
        value: ByteArray,
        isSuccess: Boolean
    ) {
        super.onCharacteristicRead(gatt, characteristic, value, isSuccess)
//        onCharacteristicChanged(gatt, characteristic, value)
    }

    override fun onCharacteristicChanged(
        gatt: BluetoothGatt?,
        characteristic: BluetoothGattCharacteristic?,
        value: ByteArray
    ) {
        super.onCharacteristicChanged(gatt, characteristic, value)
        runOnUiThread {
            val response = characteristic?.value?.decodeToString()?.lowercase()
            val command = bluetoothService?.waitingForRes
            val unit = getWaitingForResUnit() ?: return@runOnUiThread
            if (response?.startsWith("U", ignoreCase = true) == true) {
                val fetchedChannelModel = FetchedChannelModel(response)
                vm.setPressureInBar(fetchedChannelModel.getPressure())
                vm.fetchedUnitModel = fetchedChannelModel
                showUnitDataFetchedUI()
                if (!vm.fetchDataFromAutoupdate) {
                    cancelUpdateBtnBlinkAnimation()
                    if (vm.getNextAvailableChannel() == null) {
                        startReloadBtnAnimation()
                    } else {
                        cancelReloadBtnBlinkAnimator()
                    }
                }
                runAutoUpdate()
            } else if (response?.startsWith("V", ignoreCase = true) == true) {
                vm.setVoltageResponse(response)
            } else if (vm.selectedUnitModel.value?.isSoundCommand(bluetoothService?.waitingForRes) == true) {
//                showToast(String.format(getString(Strings.signal_sent_to_unit), vm.selectedUnitModel.value?.unitNumber))
            } else if (vm.selectedUnitModel.value?.isPressureCommand(bluetoothService?.waitingForRes) == true) {
                showToast(String.format(getString(Strings.signal_sent_to_unit), vm.selectedUnitModel.value?.unitNumber))
            } else {
                if (response == AppConstants.CommandResponse.NO_RESPONSE || response == AppConstants.CommandResponse.NO_REPLY) {
                    /*showDialog(
                        title = getString(Strings.no_response),
                        message = String.format(getString(Strings.no_response_msg), unit),
                        onPositiveClick = {
                            reloadUnitData()
                        }
                    )*/
                    if (vm.fetchedUnitModel == null) {
                        val unitNumber = vm.selectedUnitModel.value?.unitNumber ?: return@runOnUiThread
                        val fetchedChannelModel = FetchedChannelModel("U${unitNumber}")
                        vm.fetchedUnitModel = fetchedChannelModel
                    }
                    cancelReloadBtnBlinkAnimator()
                    startUpdateBtnBlinkAnimation()
                } else if (response == AppConstants.CommandResponse.GOT_IT) {
                    if (vm.selectedUnitModel.value?.isReloadCommand(command) == true) {
                        vm.fireChannelNo = -1
                        cancelReloadBtnBlinkAnimator()
                        if (vm.fetchedUnitModel == null) {
                            val unitNumber = vm.selectedUnitModel.value?.unitNumber ?: return@runOnUiThread
                            val fetchedChannelModel = FetchedChannelModel("U${unitNumber}-000000000000")
                            vm.fetchedUnitModel = fetchedChannelModel
                        } else {
                            vm.fetchedUnitModel?.doReload()
                        }
                        showUnitDataFetchedUI()
                        runAutoUpdate()
                        showToast(Strings.unit_successfully_initialized)
                    } else {
                        val channel = vm.fireChannelNo
                        if (channel != -1) {
                            vm.fetchedUnitModel?.markChannelAsFire(channel)
                            showUnitDataFetchedUI()
                            showToast(
                                String.format(
                                    getString(Strings.channel_fired_successfully),
                                    channel
                                )
                            )
                            if (vm.getNextAvailableChannel() == null) {
                                cancelUpdateBtnBlinkAnimation()
                                startReloadBtnAnimation()
                            }
                        }
                    }
                }
            }
            binding.invalidateAll()
        }
    }

    override fun onCharacteristicChangedTimeout() {
        super.onCharacteristicChangedTimeout()
        updateBatteryAndVolumeUI() // 🎯 Aktualisiert Status-Panel (Icon, Text, Farben) bei Timeout
        when(bluetoothService?.waitingForRes) {
            in listOfTestCommand -> {

            }
            in listOfFetchDataCommand -> {
                startUpdateBtnBlinkAnimation()
                cancelReloadBtnBlinkAnimator()
            }
        }
    }


    private fun showUnitDataFetchedUI() {
        updateBatteryAndVolumeUI() // 🎯 Aktualisiert Akku- und Lautstärkeanzeige bei Telemetrie-Eingang
        binding.tv1.isSelected = vm.fetchedUnitModel?.isChannelAvailable(1) == true
        binding.tv2.isSelected = vm.fetchedUnitModel?.isChannelAvailable(2) == true
        binding.tv3.isSelected = vm.fetchedUnitModel?.isChannelAvailable(3) == true
        binding.tv4.isSelected = vm.fetchedUnitModel?.isChannelAvailable(4) == true
        binding.tv5.isSelected = vm.fetchedUnitModel?.isChannelAvailable(5) == true
        binding.tv6.isSelected = vm.fetchedUnitModel?.isChannelAvailable(6) == true
        binding.tv7.isSelected = vm.fetchedUnitModel?.isChannelAvailable(7) == true
        binding.tv8.isSelected = vm.fetchedUnitModel?.isChannelAvailable(8) == true
        binding.tv9.isSelected = vm.fetchedUnitModel?.isChannelAvailable(9) == true
        binding.tv10.isSelected = vm.fetchedUnitModel?.isChannelAvailable(10) == true
        binding.tv11.isSelected = vm.fetchedUnitModel?.isChannelAvailable(11) == true
        binding.tv12.isSelected = vm.fetchedUnitModel?.isChannelAvailable(12) == true
        binding.invalidateAll()
    }

    private fun fetchUnitData(unitModel: UnitModel?, autoupdate: Boolean = false) {
        if (!autoupdate) {
            vm.resetFetchedUnit()
            hideUnitDataFetchedUI()
        }
        vm.fetchDataFromAutoupdate = autoupdate
        val haxCode = unitModel?.fetchDataHexCode() ?: return
        sendCommand(haxCode)
    }

    private fun reloadUnitData() {
        if (vm.selectedUnitModel.value == null) return
        /*vm.fireChannelNo = -1
        hideUnitDataFetchedUI()*/
        val haxCode = vm.selectedUnitModel.value?.reloadHexCode() ?: return
        sendCommand(haxCode)
    }

    private fun reloadChannel(channelNo: Int) {
        vm.isAutoUpdateOnPause = true
        lifecycleScope.launch {
            delay(15000)
            vm.isAutoUpdateOnPause = false
            runAutoUpdate()
        }
        vm.fetchedUnitModel?.markChannelAsAvailable(channelNo)
        showUnitDataFetchedUI()
        showToast(
            String.format(
                getString(Strings.channel_n_successfully_initialized),
                channelNo
            )
        )
    }

    private fun fireChannel(channelNo: Int) {
        if (vm.fetchedUnitModel?.isChannelAvailable(channelNo) != true) {
            showToast(Strings.channel_not_available)
            return
        }
        val haxCode = vm.selectedUnitModel.value?.unitChannelHexCode(
            unit = vm.fetchedUnitModel?.getUnit(),
            channel = channelNo
        ) ?: return
        vm.fireChannelNo = channelNo
        sendCommand(haxCode)
    }

    private fun fireSoundCommand(soundIndex: Int? = null) {
        val haxCode = vm.selectedUnitModel.value?.soundHexCode(soundIndex, vm.selectedVolumeStep) ?: return
        sendCommand(haxCode)
    }

    private fun fireSoundAndChannel(channelNo: Int) {
        if (vm.fetchedUnitModel?.isChannelAvailable(channelNo) != true) {
            showAlertDialog(
                title = getString(Strings.fire_channel),
                message = String.format(
                    getString(Strings.do_you_want_to_fire_channel_x),
                    channelNo
                ),
                isCancelable = false,
                positiveBtnText = getString(Strings.yes),
                negativeBtnText = getString(Strings.no),
                positiveClickListener = {
//                    reloadChannel(channelNo)
                    fireChannel(channelNo)
                },
                negativeClickListener = {

                }
            )
            return
        }
        vm.isAutoUpdateOnPause = true
        lifecycleScope.launch {
            if (prefs.randomizedSoundAndFireEnable) {
                fireSoundAndChannelRandomly(channelNo)
            } else {
                fireSoundAndChannelStandard(channelNo)
            }
            vm.isAutoUpdateOnPause = false
            runAutoUpdate()
        }
    }

    private suspend fun fireSoundAndChannelStandard(channelNo: Int) {
        if (vm.fetchedUnitModel != null) {
            withContext(Dispatchers.Main) {
                showSecondaryProgress.set(true)
            }

            if (prefs.soundCount > 0) {
                withContext(Dispatchers.Main) { fireSoundCommand() }
            }

            repeat(prefs.soundCount - 1) {
                delay((prefs.soundDelay * 1000L))
                withContext(Dispatchers.Main) { fireSoundCommand() }
            }

            delay((prefs.fireDelay * 1000L))
            withContext(Dispatchers.Main) { fireChannel(channelNo) }

            if (prefs.gunshotAfterLaunchEnable) {
                delay(1000L)
                withContext(Dispatchers.Main) { fireSoundCommand(4) } // Fire gunshot sound
            }

            withContext(Dispatchers.Main) {
                showSecondaryProgress.set(false)
            }
        }
    }

    private suspend fun fireSoundAndChannelRandomly(channelNo: Int) {
        if (vm.fetchedUnitModel != null) {
            val soundIndex = if (prefs.randomizedSoundEnable) {
                prefs.selectedSoundForRandomize.randomOrNull()?.toIntOrNull()
            } else {
                null
            }
            withContext(Dispatchers.Main) {
                showSecondaryProgress.set(true)
            }

            val soundCount = if (prefs.randomSoundCountEnable) Random.nextInt(prefs.randomSoundCountMin, prefs.randomSoundCountMax + 1) else prefs.soundCount
            if (soundCount > 0) {
                withContext(Dispatchers.Main) { fireSoundCommand(soundIndex) }
            }

            val soundDelay = if (prefs.randomSoundDelayEnable) Random.nextInt(prefs.randomSoundDelayMin, prefs.randomSoundDelayMax + 1) else prefs.soundDelay
            for (i in 2..soundCount) {
                val nextSoundIndex = if (prefs.randomizedSoundEnable && !prefs.canSameSoundForMultipleSounds) {
                    prefs.selectedSoundForRandomize.randomOrNull()?.toIntOrNull()
                } else {
                    soundIndex
                }
                delay((soundDelay * 1000L))
                withContext(Dispatchers.Main) { fireSoundCommand(nextSoundIndex) }
            }

            val fireDelay = if (prefs.randomFireDelayEnable) Random.nextInt(prefs.randomFireDelayMin, prefs.randomFireDelayMax + 1) else prefs.fireDelay
            delay((fireDelay * 1000L))
            withContext(Dispatchers.Main) { fireChannel(channelNo) }

            if (prefs.gunshotAfterLaunchEnable) {
                delay(1000L)
                withContext(Dispatchers.Main) { fireSoundCommand(4) } // Fire gunshot sound
            }

            withContext(Dispatchers.Main) {
                showSecondaryProgress.set(false)
            }
        }
    }

    private fun hideUnitDataFetchedUI() {
        updateBatteryAndVolumeUI() // 🎯 Aktualisiert das Status-Panel auch beim Zurücksetzen der Daten
        binding.tv1.isSelected = false
        binding.tv2.isSelected = false
        binding.tv3.isSelected = false
        binding.tv4.isSelected = false
        binding.tv5.isSelected = false
        binding.tv6.isSelected = false
        binding.tv7.isSelected = false
        binding.tv8.isSelected = false
        binding.tv9.isSelected = false
        binding.tv10.isSelected = false
        binding.tv11.isSelected = false
        binding.tv12.isSelected = false
    }

    private fun startReloadBtnAnimation() {
        val btnView = if (binding.llReload.isVisible) binding.btnReloadContainer else binding.btnReloadLarge
        reloadBtnBlinkAnimator?.cancel()
        reloadBtnBlinkAnimator = ObjectAnimator.ofFloat(btnView, "alpha", 1f, 0f, 1f)
        reloadBtnBlinkAnimator?.duration = 1000
        reloadBtnBlinkAnimator?.repeatCount = ObjectAnimator.INFINITE
        reloadBtnBlinkAnimator?.repeatMode = ObjectAnimator.RESTART
        reloadBtnBlinkAnimator?.interpolator = AccelerateDecelerateInterpolator()
        reloadBtnBlinkAnimator?.start()
    }

    private fun cancelReloadBtnBlinkAnimator() {
        reloadBtnBlinkAnimator?.cancel()
        binding.btnReloadContainer.alpha = 1.0f
        binding.btnReloadLarge.alpha = 1.0f
    }

    private var failBlinkAnimator: ObjectAnimator? = null

    private fun startFailBlinkAnimation() {
        val btnView = if (binding.llUpdate.isVisible) binding.btnUpdateContainer else binding.btnUpdateLarge
        if (failBlinkAnimator == null || !failBlinkAnimator!!.isStarted) {
            cancelUpdateBtnBlinkAnimation()
            failBlinkAnimator = ObjectAnimator.ofFloat(btnView, "alpha", 1f, 0.2f, 1f).apply {
                duration = 300
                repeatCount = ObjectAnimator.INFINITE
                repeatMode = ObjectAnimator.RESTART
                start()
            }
        }
    }

    private fun stopFailBlinkAnimation() {
        failBlinkAnimator?.cancel()
        failBlinkAnimator = null
        binding.btnUpdateContainer.alpha = 1.0f
        binding.btnUpdateLarge.alpha = 1.0f
    }

    private fun updateBottomButtonsUI() {
        val fetchedModel = vm.fetchedUnitModel

        // 🎯 1:1 Simple View RELOAD-Logik: Rot bei LOCK, sonst SkyBlue
        val isLocked = vm.isCompressorLocked()
        val reloadColor = if (isLocked) getColor(R.color.colorRed) else getColor(R.color.colorSkyBlue)
        binding.btnReloadContainer.backgroundTintList = android.content.res.ColorStateList.valueOf(reloadColor)
        binding.btnReloadLarge.backgroundTintList = android.content.res.ColorStateList.valueOf(reloadColor)

        // RELOAD-Blinken: bei LOCK ODER wenn alle Kanäle leer/verschossen sind
        val hasAvailableChannels = vm.getNextAvailableChannel() != null
        if (isLocked || (fetchedModel != null && !hasAvailableChannels)) {
            startReloadBtnAnimation()
        } else {
            cancelReloadBtnBlinkAnimator()
        }

        // 🎯 1:1 Simple View UPDATE-Logik: Rot + Schnelles Warnblinken (300ms) bei FAIL, sonst Orange
        val isFail = fetchedModel?.isBarFail() == true
        val updateColor = if (isFail) getColor(R.color.colorRed) else getColor(R.color.colorOrange)
        binding.btnUpdateContainer.backgroundTintList = android.content.res.ColorStateList.valueOf(updateColor)
        binding.btnUpdateLarge.backgroundTintList = android.content.res.ColorStateList.valueOf(updateColor)

        if (isFail) {
            startFailBlinkAnimation()
        } else {
            stopFailBlinkAnimation()
        }
        binding.invalidateAll() // 🎯 Aktualisiert die Text-Anbindung des Reload-Buttons sofort
    }

    private fun startUpdateBtnBlinkAnimation() {
        val btnView = if (binding.llUpdate.isVisible) binding.btnUpdateContainer else binding.btnUpdateLarge
        updateBtnBlinkAnimator?.cancel()
        updateBtnBlinkAnimator = ObjectAnimator.ofFloat(btnView, "alpha", 1f, 0f, 1f)
        updateBtnBlinkAnimator?.duration = 1000
        updateBtnBlinkAnimator?.repeatCount = ObjectAnimator.INFINITE
        updateBtnBlinkAnimator?.repeatMode = ObjectAnimator.RESTART
        updateBtnBlinkAnimator?.interpolator = AccelerateDecelerateInterpolator()
        updateBtnBlinkAnimator?.start()
    }

    private fun cancelUpdateBtnBlinkAnimation() {
        updateBtnBlinkAnimator?.cancel()
        binding.btnUpdateContainer.alpha = 1.0f
        binding.btnUpdateLarge.alpha = 1.0f
    }
}