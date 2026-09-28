package com.launcher_control_android.main.ui.home.view

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.graphics.Color
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.os.bundleOf
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
import com.launcher_control_android.databinding.ActLauncherControlBinding
import com.launcher_control_android.helper.bluetooth.communication.BluetoothCommunicationAct
import com.launcher_control_android.helper.util.animateHorizontalFlip
import com.launcher_control_android.helper.util.animateRotate
import com.launcher_control_android.helper.util.animateWave
import com.launcher_control_android.helper.util.getVoltageImageResId
import com.launcher_control_android.helper.util.logE
import com.launcher_control_android.helper.util.startActivity
import com.launcher_control_android.helper.util.startActivityForResult
import com.launcher_control_android.helper.util.triggerHaptic
import com.launcher_control_android.main.common.ApiRenderState
import com.launcher_control_android.main.common.FetchedChannelModel
import com.launcher_control_android.main.ui.channel_list.view.ChannelListAct
import com.launcher_control_android.main.ui.connection_config.view.ConnectionConfigAct
import com.launcher_control_android.main.ui.home.LauncherControlUIStateModel
import com.launcher_control_android.main.ui.home.model.LauncherControlVM
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
class LauncherControlAct :
    BluetoothCommunicationAct<ActLauncherControlBinding, LauncherControlVM>(Layouts.act_launcher_control) {

    private val showSecondaryProgress = ObservableField(false)
    private var tvConnectingBlinkAnimator: ObjectAnimator? = null
    private var reloadBtnBlinkAnimator: ObjectAnimator? = null
    private var updateBtnBlinkAnimator: ObjectAnimator? = null
    private var animatorSet: AnimatorSet? = null

    private var showGatewayInfoJob: Job? = null
    private var stayArmedTimerJob: Job? = null
    private var autoupdateJob: Job? = null

    override val vm: LauncherControlVM by viewModels()

    override val hasProgress: Boolean = false

    override fun init() {
        binding.showSecondaryProgress = showSecondaryProgress
        deviceAddress = prefs.savedBluetoothDevice?.address ?: ""
        setListener()
        setObserver()
        setupBlinkAnimation()
        connectGateway()
        setupLauncherTouchArea() // GEÄNDERT: Klickbereich-Einschränkung anheften
        setupVolumeTouchListener() // 🎯 Touch-Geste für die vertikale Lautstärkestele

        // GEÄNDERT: Zurück-Taste/Geste abfangen: Bei gewählter Unit erst zur Hauptseite zurückkehren
        onBackPressedDispatcher.addCallback(this, object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (vm.uiState.value?.hasUnitSelected() == true) {
                    resetSelectUnit()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })

        /**
         * Set minimum autoupdatePressureDelay to 5 if in previous app version it set as less then 5 because in previous version minimum value is 3
         */
        if (prefs.autoupdatePressureDelay < 5) {
            prefs.autoupdatePressureDelay = 5
        }
    }

    private fun setupLauncherTouchArea() {
        // GEÄNDERT: Klicks im obersten 35dp-Streifen des Frisbee-Buttons ignorieren,
        // um Überschneidungen mit dem Akku-Icon/Spannungstext vollständig auszuschließen.
        val topIgnoreThresholdPx = 35 * resources.displayMetrics.density
        binding.ivLauncher.setOnTouchListener { _, event ->
            if (event.action == android.view.MotionEvent.ACTION_DOWN && event.y < topIgnoreThresholdPx) {
                true // Touch im oberen Streifen abfangen & Frisbee-Klick verhindern
            } else {
                false // Reguläres Klick-Verhalten im restlichen Bereich zulassen
            }
        }
    }

    private fun setListener() {
        binding.btnSound.setOnLongClickListener {
            it.triggerHaptic()
            showDialogFrag(
                SoundOptionsBsd.newInstance(vm.uiState.value?.selectedUnit?.selectedSound ?: 0) { soundIdx ->
                    vm.setSoundForSelectedUnit(soundIdx)
                    binding.invalidateAll() // 🎯 Zeichnet die Layout-Texte sofort neu
                },
            )
            true
        }
        binding.btnUpdate.setOnLongClickListener {
            it.triggerHaptic()
            if (vm.uiState.value?.selectedUnit?.isServoVersion != true) {
                showDialogFrag(
                    PressureOptionsBsd.newInstance(vm.uiState.value?.selectedUnit?.selectedPressure ?: 0) {
                        vm.setPressureForSelectedUnit(it)
                        vm.uiState.value?.selectedUnit?.pressureHaxCode(it)?.let { hexCode ->
                            sendCommand(hexCode)
                        }
                    },
                )
            }
            true
        }

        binding.ivLauncher.setOnLongClickListener {
            it.triggerHaptic()
            // NEU: Wenn keine Unit gewählt ist, zeigt Longpress die Gateway-Informationen an
            if (vm.uiState.value?.isGatewayConnected() == true && vm.uiState.value?.selectedUnit == null) {
                showGatewayInfo()
            } else if (vm.uiState.value?.isGatewayConnected() == true && vm.uiState.value?.hasUnitDataFetched() != true && vm.hasStayArmedActive()) {
                bluetoothService?.disconnectBluetoothDevice()
                setGatewayDisconnected()
            } else {
                if (vm.hasStayArmedActive()) {
                    fireSoundAndChannel(vm.uiState.value?.selectedUnit?.isChannelAdded() == true)
                } else {
                    vm.setUnitDisarmed(false)
                }
            }
            true
        }

        binding.tv1.setOnLongClickListener {
            it.triggerHaptic()
            handleLongUnitClick(1, prefs.unit1Model)
            true
        }

        binding.tv2.setOnLongClickListener {
            it.triggerHaptic()
            handleLongUnitClick(2, prefs.unit2Model)
            true
        }

        binding.tv3.setOnLongClickListener {
            it.triggerHaptic()
            handleLongUnitClick(3, prefs.unit3Model)
            true
        }

        binding.tv4.setOnLongClickListener {
            it.triggerHaptic()
            handleLongUnitClick(4, prefs.unit4Model)
            true
        }
    }

    private fun updateUnitButtonDimensions(state: LauncherControlUIStateModel) {
        val isMainScreen = !state.hasUnitSelected()
        // Zählt, wie viele Units aktuell sichtbar geschaltet sind
        val visibleCount = (1..4).count { unitNum -> vm.getUnitModel(unitNum)?.isVisible() == true }

        // Bedingung: Hauptseite (keine Unit gewählt) UND genau 4 Units sind sichtbar
        val isAll4Visible = !state.hasUnitSelected() && visibleCount == 4

        val sizeDp = if (isMainScreen) 70 else 60
        val marginDp = if (isMainScreen && isAll4Visible) 6 else 10
        val sizePx = (sizeDp * resources.displayMetrics.density).toInt()
        val marginPx = (marginDp * resources.displayMetrics.density).toInt()

        listOf(binding.tv1, binding.tv2, binding.tv3, binding.tv4).forEach { tv ->
            val params = tv.layoutParams as? androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
            if (params != null) {
                params.width = sizePx
                params.height = sizePx
                params.marginStart = marginPx
                params.marginEnd = marginPx
                tv.layoutParams = params
                tv.requestLayout()
            }
        }
    }

    private var fanRotateAnimator: ObjectAnimator? = null
    private var rainbowAnimator: ValueAnimator? = null
    private var cachedRainbowBitmap: android.graphics.Bitmap? = null
    private var cachedRainbowCanvas: android.graphics.Canvas? = null

    private fun updateRainbowGlowAnimation(shouldAnimate: Boolean) {
        if (shouldAnimate) {
            if (rainbowAnimator == null || !rainbowAnimator!!.isStarted) {
                val drawable = binding.ivLauncher.drawable ?: return
                binding.ivRainbowGlow.visibility = View.VISIBLE

                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                    binding.ivRainbowGlow.setRenderEffect(
                        android.graphics.RenderEffect.createBlurEffect(25f, 25f, android.graphics.Shader.TileMode.CLAMP)
                    )
                }

                val colors = intArrayOf(
                    Color.RED, Color.YELLOW, Color.GREEN,
                    Color.CYAN, Color.BLUE, Color.MAGENTA, Color.RED
                )

                rainbowAnimator = ValueAnimator.ofFloat(0f, 360f).apply {
                    duration = 3000
                    repeatCount = ValueAnimator.INFINITE
                    interpolator = android.view.animation.LinearInterpolator()
                    addUpdateListener { anim ->
                        val angle = anim.animatedValue as Float
                        val width = binding.ivRainbowGlow.width
                        val height = binding.ivRainbowGlow.height
                        if (width > 0 && height > 0) {
                            if (cachedRainbowBitmap == null || cachedRainbowBitmap?.width != width || cachedRainbowBitmap?.height != height) {
                                cachedRainbowBitmap = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
                                cachedRainbowCanvas = android.graphics.Canvas(cachedRainbowBitmap!!)
                            }
                            val bitmap = cachedRainbowBitmap!!
                            val canvas = cachedRainbowCanvas!!
                            bitmap.eraseColor(Color.TRANSPARENT)

                            drawable.setBounds(0, 0, width, height)
                            drawable.draw(canvas)

                            val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                                xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.SRC_IN)
                                val gradient = android.graphics.SweepGradient(width / 2f, height / 2f, colors, null)
                                val matrix = android.graphics.Matrix()
                                matrix.postRotate(angle, width / 2f, height / 2f)
                                gradient.setLocalMatrix(matrix)
                                shader = gradient
                            }
                            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
                            binding.ivRainbowGlow.setImageBitmap(bitmap)
                        }
                    }
                    start()
                }
            }
        } else {
            rainbowAnimator?.cancel()
            rainbowAnimator = null
            cachedRainbowBitmap = null
            cachedRainbowCanvas = null
            binding.ivRainbowGlow.visibility = View.GONE
        }
    }

    private fun updateFanAnimation(isCompressorActive: Boolean) {
        val isSoundOrServo = vm.uiState.value?.isSoundOnlyMode() == true || vm.uiState.value?.isUnitServoON() == true
        if (isCompressorActive && !isSoundOrServo) {
            if (fanRotateAnimator == null || !fanRotateAnimator!!.isStarted) {
                fanRotateAnimator = ObjectAnimator.ofFloat(binding.ivLeftStatusIcon, "rotation", 0f, 360f).apply {
                    duration = 1500
                    repeatCount = ValueAnimator.INFINITE
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

    private var failBlinkAnimator: ObjectAnimator? = null

    private fun startFailBlinkAnimation() {
        if (failBlinkAnimator == null || !failBlinkAnimator!!.isStarted) {
            cancelUpdateBtnBlinkAnimation()
            failBlinkAnimator = ObjectAnimator.ofFloat(binding.btnUpdateContainer, "alpha", 1f, 0.2f, 1f).apply {
                duration = 300
                repeatCount = ValueAnimator.INFINITE
                repeatMode = ValueAnimator.RESTART
                start()
            }
        }
    }

    private fun stopFailBlinkAnimation() {
        failBlinkAnimator?.cancel()
        failBlinkAnimator = null
        binding.btnUpdateContainer.alpha = 1.0f
    }

    private var leftIconFailBlinkAnimator: ObjectAnimator? = null

    private fun startLeftIconFailBlinkAnimation() {
        if (leftIconFailBlinkAnimator == null || !leftIconFailBlinkAnimator!!.isStarted) {
            leftIconFailBlinkAnimator = ObjectAnimator.ofFloat(binding.ivLeftStatusIcon, "alpha", 1f, 0.2f, 1f).apply {
                duration = 300
                repeatCount = ValueAnimator.INFINITE
                repeatMode = ValueAnimator.RESTART
                start()
            }
        }
    }

    private fun stopLeftIconFailBlinkAnimation() {
        leftIconFailBlinkAnimator?.cancel()
        leftIconFailBlinkAnimator = null
        binding.ivLeftStatusIcon.alpha = 1.0f
    }

    private fun updateLeftTelemetryPanel(state: LauncherControlUIStateModel) {
        val fetchedModel = state.fetchedUnitModel ?: vm.disarmedUnitFetchedData.value
        if (fetchedModel != null) {
            binding.ivLeftStatusIcon.setImageResource(state.leftStatusIconResId(fetchedModel))
            binding.ivLeftStatusIcon.imageTintList = android.content.res.ColorStateList.valueOf(getColor(state.leftStatusIconColor(fetchedModel)))
            val statusText = state.leftStatusText(fetchedModel)
            binding.tvLeftStatusText.text = statusText
            binding.tvLeftStatusText.setTextColor(getColor(state.leftStatusIconColor(fetchedModel)))

            // 🎯 Ruckelfreie Verschiebung über translationY (ohne DataBinding-Konflikt)
            val density = resources.displayMetrics.density
            val translationYDp = when (statusText) {
                "FAIL", "LOCK" -> -22f
                "SERVO OK", "SOUND OK" -> 2f
                "OFF" -> -28f
                "ON", -> -30f
                else -> 0f
            }
            binding.tvLeftStatusText.translationY = translationYDp * density

            updateFanAnimation(state.isCompressorActive(fetchedModel))

            if (fetchedModel.isBarFail()) {
                startLeftIconFailBlinkAnimation()
            } else {
                stopLeftIconFailBlinkAnimation()
            }

            // 🎯 RELOAD Button: Rot bei LOCK, sonst SkyBlue
            val isLocked = state.isCompressorLocked(fetchedModel)
            val reloadColor = if (isLocked) getColor(R.color.colorRed) else getColor(R.color.colorSkyBlue)
            binding.btnReloadContainer.backgroundTintList = android.content.res.ColorStateList.valueOf(reloadColor)

            // 🎯 UPDATE Button: Rot + Schnelles Blinken bei FAIL (>= 14), sonst Orange
            val isFail = fetchedModel.isBarFail()
            val updateColor = if (isFail) getColor(R.color.colorRed) else getColor(R.color.colorOrange)
            binding.btnUpdateContainer.backgroundTintList = android.content.res.ColorStateList.valueOf(updateColor)

            if (isFail) {
                startFailBlinkAnimation()
            } else {
                stopFailBlinkAnimation()
            }
        }
    }

    private fun updateVolumeUI() {
        val unit = vm.uiState.value?.selectedUnit ?: return
        val step = unit.volumeStep

        binding.tvVolPercent.text = when (step) {
            4 -> "100%"
            3 -> "50%"
            2 -> "30%"
            1 -> "15%"
            else -> "100%"
        }

        val activeColor = android.content.res.ColorStateList.valueOf(getColor(R.color.colorGreen))
        val inactiveColor = android.content.res.ColorStateList.valueOf(Color.parseColor("#4000FF00")) // Transparentes Grün (25% Alpha)

        binding.volSeg4.backgroundTintList = if (step >= 4) activeColor else inactiveColor
        binding.volSeg3.backgroundTintList = if (step >= 3) activeColor else inactiveColor
        binding.volSeg2.backgroundTintList = if (step >= 2) activeColor else inactiveColor
        binding.volSeg1.backgroundTintList = if (step >= 1) activeColor else inactiveColor
    }

    private fun setupVolumeTouchListener() {
        val listener = android.view.View.OnTouchListener { view, event ->
            if (event.action == android.view.MotionEvent.ACTION_DOWN || event.action == android.view.MotionEvent.ACTION_MOVE) {
                val unit = vm.uiState.value?.selectedUnit ?: return@OnTouchListener false
                val height = binding.llVolSegments.height.toFloat()
                if (height > 0) {
                    val y = event.y.coerceIn(0f, height)
                    val normalizedY = 1.0f - (y / height)
                    val newStep = (Math.ceil(normalizedY * 4.0).toInt()).coerceIn(1, 4)
                    if (unit.volumeStep != newStep) {
                        unit.volumeStep = newStep
                        vm.updateUnitPref(unit, unit.unitNumber)
                        updateVolumeUI()
                        view.triggerHaptic()
                        if (vm.hasStayArmedActive()) {
                            setStayArmed()
                        }
                    }
                }
            }
            true
        }
        binding.llVolSegments.setOnTouchListener(listener)
    }

    private fun setObserver() {
        vm.uiState.observe(this@LauncherControlAct) { state ->
            // GEÄNDERT: Dynamische Größen- und Abstandsanpassung für tv_1 bis tv_4
            updateUnitButtonDimensions(state)
            updateFanAnimation(state.isCompressorActive())
            updateLeftTelemetryPanel(state)
            updateVolumeUI() // 🎯 Aktualisiert die 4-Stufen Lautstärkestele und den %-Text

            // 1. Rainbow-Glow-Animation (iOS-Style)
            val shouldAnimateGlow = if (state.isSoundOnlyMode()) {
                state.hasUnitDataFetched() && !state.isDisarmed && state.hasUnitSelected()
            } else {
                state.getNextAvailableChannel() != null && !state.isDisarmed && state.hasUnitSelected()
            }
            updateRainbowGlowAnimation(shouldAnimateGlow)

            // 2. Disarm-Timer (starr an !isDisarmed gekoppelt)
            if (state.hasUnitSelected() && !state.isDisarmed && state.getNextAvailableChannel() != null) {
                if (stayArmedTimerJob == null || stayArmedTimerJob?.isActive != true) {
                    setStayArmed()
                }
            } else if (state.isDisarmed) {
                stayArmedTimerJob?.cancel()
                resetTitleUI()
            }

            if (vm.uiState.value?.fetchedUnitModel != null) {
                val isLocked = state.isCompressorLocked()
                val hasAvailableChannels = state.getNextAvailableChannel() != null

                // 🎯 RELOAD-Blinken starten, wenn Gerät im LOCK ist ODER alle Kanäle leer sind
                if (isLocked || !hasAvailableChannels) {
                    startReloadBtnAnimation()
                } else {
                    cancelReloadBtnBlinkAnimator()
                }

                if (hasAvailableChannels) {
                    cancelUpdateBtnBlinkAnimation()
                    startBlinkAnimation()
                } else {
                    stayArmedTimerJob?.cancel()
                    cancelUpdateBtnBlinkAnimation()
                    stopBlinkAnimation()
                }

                if (state.selectedUnit?.isChannelAdded() == true) {
                    autoupdateJob?.cancel()
                    if (prefs.autoupdatePressureEnable) {
                        autoupdateJob = lifecycleScope.launch {
                            delay((prefs.autoupdatePressureDelay * 1000L))
                            if (!vm.isAutoUpdateOnPause &&
                                vm.uiState.value?.hasUnitSelected() == true &&
                                vm.uiState.value?.selectedUnit?.isServoVersion != true &&
                                vm.uiState.value?.selectedUnit?.isChannelAdded() == true) {
                                fetchUnitData(vm.uiState.value?.selectedUnit, true)
                            }
                        }
                    }
                } else {
                    autoupdateJob?.cancel()
                }

                if (state.isGatewayConnecting()) {
                    startConnectingBlinkAnimation()
                } else {
                    cancelConnectingBlinkAnimation()
                }
            }
            binding.invalidateAll()
            binding.executePendingBindings()

            // ⏱️ Schützt den Disarm-Timer vor dem asynchronen DataBinding-Rebind bei Statusupdates
            if (stayArmedTimerJob?.isActive == true && remainingDisarmSeconds > 0) {
                binding.toolbar.textView.text = "DISARM IN ${remainingDisarmSeconds}s"
                binding.toolbar.textView.setTextColor(
                    if (remainingDisarmSeconds <= 5) Color.parseColor("#FFA500") else Color.WHITE
                )
            }
        }
        vm.disarmedUnitFetchedData.observe(this@LauncherControlAct) {
            vm.uiState.value?.let { state -> updateLeftTelemetryPanel(state) }
        }
    }

    private fun connectGateway() {
        if (vm.uiState.value?.hasGatewayAndUnitSet() == true && vm.uiState.value?.isGatewayNotConnected() == true) {
            checkBluetooth {
                vm.setGatewayConnecting()
                startConnectingBlinkAnimation()
                startBluetoothService {
                    setGatewayConnected()
                }
            }
        }
    }

    override fun onDeviceConnectionChanged(gatt: BluetoothGatt?, isConnected: Boolean) {
        super.onDeviceConnectionChanged(gatt, isConnected)
        runOnUiThread {
            if (isConnected) {
                setGatewayConnected()
            } else {
                setGatewayDisconnected()
            }
            binding.invalidateAll()
            binding.executePendingBindings()
        }
    }

    override fun renderState(apiRenderState: ApiRenderState) {

    }

    override fun onReadRemoteRssi(gatt: BluetoothGatt?, rssi: Int, status: Int) {
        runOnUiThread {
            vm.gatewaySignalStrength.value = rssi
        }
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
            "DEBUG 1: response: $response, command: $command, unit: $unit".logE()
            if (response?.startsWith("U", ignoreCase = true) == true) {
                val fetchedChannelModel = FetchedChannelModel(response)
                vm.setPressureInBar(fetchedChannelModel.getPressure())
                vm.setFetchedUnitData(fetchedChannelModel)
                updateFieldBatteryUI() // GEÄNDERT: Akkubalken bei neuer Telemetrie direkt aktualisieren
            } else if (response?.startsWith("V", ignoreCase = true) == true) {
                vm.setVoltageResponse(response)
                setVoltage()
                setVoltageVisibility()
            } else if (vm.uiState.value?.selectedUnit?.isSoundCommand(bluetoothService?.waitingForRes) == true) {
//                showToast(String.format(getString(Strings.signal_sent_to_unit), vm.uiState.value?.selectedUnit?.unitNumber))
            } else if (vm.uiState.value?.selectedUnit?.isPressureCommand(bluetoothService?.waitingForRes) == true) {
                showToast(String.format(getString(Strings.signal_sent_to_unit), vm.uiState.value?.selectedUnit?.unitNumber))
            } else {
                if (response == AppConstants.CommandResponse.NO_RESPONSE || response == AppConstants.CommandResponse.NO_REPLY) {
                    /*showDialog(
                        title = getString(Strings.no_response),
                        message = String.format(getString(Strings.no_response_msg), unit),
                        onPositiveClick = {
                            reloadUnitData()
                        }
                    )*/
                    if (vm.uiState.value?.fetchedUnitModel == null) {
                        val unitNumber = vm.uiState.value?.selectedUnit?.unitNumber ?: return@runOnUiThread
                        val fetchedChannelModel = FetchedChannelModel("U${unitNumber}")
                        vm.setFetchedUnitData(fetchedChannelModel)
                    }
                    stayArmedTimerJob?.cancel()
                    cancelReloadBtnBlinkAnimator()
                    stopBlinkAnimation()
                    startUpdateBtnBlinkAnimation()
                } else if (AppConstants.CommandResponse.isSuccess(response)) {
                    if (vm.uiState.value?.selectedUnit?.isReloadCommand(command) == true) {
                        cancelReloadBtnBlinkAnimator()
                        if (vm.uiState.value?.fetchedUnitModel == null) {
                            val unitNumber = vm.uiState.value?.selectedUnit?.unitNumber ?: return@runOnUiThread
                            val fetchedChannelModel = FetchedChannelModel("U${unitNumber}-000000000000")
                            vm.setFetchedUnitData(fetchedChannelModel)
                        } else {
                            vm.uiState.value?.fetchedUnitModel?.doReload()
                            vm.setFetchedUnitData(vm.uiState.value?.fetchedUnitModel)
                        }
                        setStayArmed()
                        showToast(Strings.unit_successfully_initialized)
                    } else {
                        val channel = vm.currentFiredChannel ?: return@runOnUiThread
                        "DEBUG 1: channel: ${vm.currentFiredChannel} ?: ${channel}".logE()
                        if (channel != null) {
                            vm.uiState.value?.fetchedUnitModel?.markChannelAsFire(channel)
                            showToast(String.format(getString(Strings.channel_fired_successfully), channel))
                            vm.setFetchedUnitData(vm.uiState.value?.fetchedUnitModel)
                        }
                        vm.currentFiredChannel = null
                    }
                }
            }
        }
    }

    override fun onCharacteristicChangedTimeout() {
        super.onCharacteristicChangedTimeout()
        when(bluetoothService?.waitingForRes) {
            in listOfTestCommand -> {

            }
            in listOfFetchDataCommand -> {
                val unitNumber = vm.uiState.value?.selectedUnit?.unitNumber
                if (unitNumber != null && vm.uiState.value?.fetchedUnitModel == null) {
                    vm.setFetchedUnitData(FetchedChannelModel("U $unitNumber"))
                }
                startUpdateBtnBlinkAnimation()
                cancelReloadBtnBlinkAnimator()
            }
        }
    }

    private fun fireSelectedChannel() {
        if (vm.uiState.value?.getNextAvailableChannel() != null) {
            val unit = vm.uiState.value?.fetchedUnitModel?.getUnit()
            val channel = vm.uiState.value?.getNextAvailableChannel()
            "DEBUG 1: Fire channel: $channel, unit: $unit".logE()
            val haxCode = vm.uiState.value?.selectedUnit?.unitChannelHexCode(
                unit = unit,
                channel = channel
            ) ?: return
            if (vm.hasStayArmedActive()) {
                vm.currentFiredChannel = channel
                sendCommand(haxCode)
                setStayArmed()
            } else {
                vm.setUnitDisarmed(false)
            }
        } else {
            // GEÄNDERT: Wenn alle Kanäle verschossen sind ("empty / please reload"), stößt ein Klick auf den großen Button direkt den Reload an
            binding.btnReload.animateRotate()
            reloadUnitData()
        }
    }

    private var remainingDisarmSeconds = 0

    private fun setStayArmed() {
        stayArmedTimerJob?.cancel()
        remainingDisarmSeconds = prefs.armedInterval
        stayArmedTimerJob = lifecycleScope.launch(Dispatchers.Main) {
            while (remainingDisarmSeconds > 0 && vm.hasStayArmedActive()) {
                binding.toolbar.textView.text = "DISARM IN ${remainingDisarmSeconds}s"
                binding.toolbar.textView.setTextColor(
                    if (remainingDisarmSeconds <= 5) Color.parseColor("#FFA500") else Color.WHITE
                )
                updateFieldBatteryUI()
                delay(1000L)
                remainingDisarmSeconds--
            }
            // GEÄNDERT: Disarm wird ausgeführt, wenn der Timer regulär abgelaufen ist
            if (remainingDisarmSeconds <= 0 && vm.hasStayArmedActive()) {
                resetTitleUI()
                vm.setUnitDisarmed(true)
            }
        }
    }

    private fun resetTitleUI() {
        stayArmedTimerJob?.cancel()
        binding.toolbar.textView.setOnClickListener(null) // GEÄNDERT: Klick-Listener auf der normalen Startseite entfernen
        vm.toolbarTitle.value = getString(R.string.launcher_control)
        binding.toolbar.textView.setTextColor(Color.WHITE)
    }

    // GEÄNDERT: Steuert die 14 Segment-Kästchen dynamisch nach empfangener Akku-Hex-Stufe an
    private fun updateFieldBatteryUI() {
        val isUnitSelected = vm.uiState.value?.hasUnitSelected() == true

        if (isUnitSelected) {
            binding.llFieldBattery.isVisible = true
            val fetchedModel = vm.uiState.value?.fetchedUnitModel ?: vm.disarmedUnitFetchedData.value
            val batteryHex = fetchedModel?.getBatteryHex()

            // GEÄNDERT: Wenn noch keine Telemetrie empfangen wurde, "--%" in Grau anzeigen
            val (percentText, color, activeBars) = if (batteryHex != null) {
                val (pct, col) = when (batteryHex) {
                    15 -> Pair("100%", Color.GREEN)
                    14 -> Pair("FAIL", Color.RED)
                    13 -> Pair("100%", Color.GREEN)
                    12 -> Pair("95%", Color.GREEN)
                    11 -> Pair("85%", Color.GREEN)
                    10 -> Pair("80%", Color.GREEN)
                    9  -> Pair("75%", Color.GREEN)
                    8  -> Pair("65%", Color.GREEN)
                    7  -> Pair("60%", Color.GREEN)
                    6  -> Pair("50%", Color.GREEN)
                    5  -> Pair("40%", Color.GREEN)
                    4  -> Pair("30%", Color.GREEN)
                    3  -> Pair("20%", Color.YELLOW)
                    2  -> Pair("10%", Color.YELLOW)
                    1  -> Pair("3%", Color.RED)
                    0  -> Pair("1%", Color.RED)
                    else -> Pair("FAIL", Color.RED)
                }
                val bars = if (batteryHex in 0..13) batteryHex + 1 else if (batteryHex == 15) 14 else 0
                Triple(pct, col, bars)
            } else {
                Triple("--%", Color.GRAY, 0)
            }

            val segments = listOf(
                binding.seg0, binding.seg1, binding.seg2, binding.seg3,
                binding.seg4, binding.seg5, binding.seg6, binding.seg7,
                binding.seg8, binding.seg9, binding.seg10, binding.seg11,
                binding.seg12, binding.seg13
            )
            segments.forEachIndexed { index, view ->
                val isActive = index < activeBars
                view.backgroundTintList = android.content.res.ColorStateList.valueOf(
                    if (isActive) color else Color.parseColor("#808080")
                )
            }

            binding.tvFieldBatLabel.setTextColor(color)
            binding.tvFieldBatPercent.setTextColor(color)
            binding.tvFieldBatPercent.text = " $percentText"
        } else {
            binding.llFieldBattery.isVisible = false
        }
    }

    private fun fireSoundCommand(soundIndex: Int? = null) {
        val unit = vm.uiState.value?.selectedUnit ?: return
        val haxCode = unit.soundHexCode(soundIndex, unit.volumeStep) ?: return
        sendCommand(haxCode)
    }

    private fun fireSoundAndChannel(isChannelAdded: Boolean = false) {
        vm.isAutoUpdateOnPause = true
        vm.isDisarmPause = true
        lifecycleScope.launch {
            if (prefs.randomizedSoundAndFireEnable) {
                fireSoundAndChannelRandomly(isChannelAdded)
            } else {
                fireSoundAndChannelStandard(isChannelAdded)
            }
            vm.isAutoUpdateOnPause = false
            vm.isDisarmPause = false
        }
    }

    private suspend fun fireSoundAndChannelStandard(isChannelAdded: Boolean = false) {
        if (vm.uiState.value?.fetchedUnitModel != null) {
            val soundIndex = if (vm.uiState.value?.selectedUnit?.isOnlySoundInstalled() == true) vm.selectedSoundIndex else null
            withContext(Dispatchers.Main) {
                showSecondaryProgress.set(true)
            }

            if (prefs.soundCount > 0) {
                withContext(Dispatchers.Main) { fireSoundCommand(soundIndex) }
            }

            for (i in 2..prefs.soundCount) {
                delay((prefs.soundDelay * 1000L))
                withContext(Dispatchers.Main) { fireSoundCommand(soundIndex) }
            }

            if (isChannelAdded && vm.uiState.value?.getNextAvailableChannel() != null) {
                delay((prefs.fireDelay * 1000L))
                withContext(Dispatchers.Main) { fireSelectedChannel() }
            }

            if (prefs.gunshotAfterLaunchEnable) {
                delay(1000L)
                withContext(Dispatchers.Main) { fireSoundCommand(4) }// Fire gunshot sound
            }

            withContext(Dispatchers.Main) {
                showSecondaryProgress.set(false)
            }
        }
    }

    private suspend fun fireSoundAndChannelRandomly(isChannelAdded: Boolean = false) {
        if (vm.uiState.value?.fetchedUnitModel != null) {
            val soundIndex = if (prefs.randomizedSoundEnable) {
                prefs.selectedSoundForRandomize.randomOrNull()?.toIntOrNull()
            } else {
                if (vm.uiState.value?.selectedUnit?.isOnlySoundInstalled() == true) vm.selectedSoundIndex else null
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
            if (isChannelAdded && vm.uiState.value?.getNextAvailableChannel() != null) {
                delay((fireDelay * 1000L))
                withContext(Dispatchers.Main) { fireSelectedChannel() }
            }

            if (prefs.gunshotAfterLaunchEnable) {
                delay(1000L)
                withContext(Dispatchers.Main) { fireSoundCommand(4) }// Fire gunshot sound
            }

            withContext(Dispatchers.Main) {
                showSecondaryProgress.set(false)
            }
        }
    }

    private var batteryBlinkAnimator: ObjectAnimator? = null

    private fun setVoltage() {
        val voltageStr = vm.fetchedUnitVoltage
        binding.tvVoltage.text = "${voltageStr}V"
        val voltage = voltageStr?.toDoubleOrNull()

        if (voltage != null) {
            binding.ivVoltage.setImageResource(getVoltageImageResId(voltage))

            // GEÄNDERT: Letzte Stufe (< 3.55 V) -> Orange, bei kritischem Tiefstand (< 3.40 V) -> Rot + Blinken
            when {
                voltage < 3.40 -> {
                    binding.ivVoltage.imageTintList = android.content.res.ColorStateList.valueOf(Color.RED)
                    binding.tvVoltage.setTextColor(Color.RED)
                    startBatteryBlinkAnimation()
                }
                voltage < 3.55 -> {
                    val orangeColor = getColor(R.color.colorOrange)
                    binding.ivVoltage.imageTintList = android.content.res.ColorStateList.valueOf(orangeColor)
                    binding.tvVoltage.setTextColor(orangeColor)
                    stopBatteryBlinkAnimation()
                }
                else -> {
                    binding.ivVoltage.imageTintList = android.content.res.ColorStateList.valueOf(Color.WHITE)
                    binding.tvVoltage.setTextColor(Color.WHITE)
                    stopBatteryBlinkAnimation()
                }
            }
        }
    }

    private fun startBatteryBlinkAnimation() {
        if (batteryBlinkAnimator == null || !batteryBlinkAnimator!!.isStarted) {
            batteryBlinkAnimator = ObjectAnimator.ofFloat(binding.ivVoltage, "alpha", 1f, 0.2f, 1f).apply {
                duration = 500
                repeatCount = ObjectAnimator.INFINITE
                repeatMode = ValueAnimator.RESTART
                start()
            }
        }
    }

    private fun stopBatteryBlinkAnimation() {
        batteryBlinkAnimator?.cancel()
        batteryBlinkAnimator = null
        binding.ivVoltage.alpha = 0.7f
    }

    private fun setVoltageVisibility(iconVisible: Boolean = true) {
        // GEÄNDERT: Sichtbarkeit an aktiven Gateway-Verbindungsstatus koppeln
        val isConnected = vm.uiState.value?.isGatewayConnected() == true
        val isVoltageVisible = isConnected && !vm.fetchedUnitVoltage.isNullOrBlank()
        binding.tvVoltage.isVisible = !iconVisible && isVoltageVisible
        vm.fetchedUnitVoltage?.toDoubleOrNull()?.let {
            binding.ivVoltage.isVisible = iconVisible && isVoltageVisible
        }
    }

    private fun showGatewayInfo() {
        showGatewayInfoJob?.cancel()
        showGatewayInfoJob = lifecycleScope.launch(Dispatchers.Main) {
            binding.cvGatewayInfo.isVisible = true
            delay(2000)
            binding.cvGatewayInfo.isVisible = false
        }
    }

    private fun fetchUnitData(unitModel: UnitModel?, autoupdate: Boolean = false) {

        vm.fetchDataFromAutoupdate = autoupdate
        val haxCode = unitModel?.fetchDataHexCode() ?: return
        sendCommand(haxCode)
    }

    private fun reloadUnitData() {
        if (vm.uiState.value?.selectedUnit == null) return
        vm.currentFiredChannel = null
        vm.fetchDataFromAutoupdate = false
        vm.setUnitDisarmed(false)
        val haxCode = vm.uiState.value?.selectedUnit?.reloadHexCode() ?: return
        sendCommand(haxCode)
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
        if (animatorSet != null && !animatorSet!!.isStarted) {
            animatorSet!!.start()
        }
    }

    private fun stopBlinkAnimation() {
        if (animatorSet != null && animatorSet!!.isStarted) {
            animatorSet!!.cancel()
            binding.tvFire.setTextColor(Color.BLACK)
            binding.tvChannel.setTextColor(Color.BLACK)
            binding.tvChannelNo.setTextColor(Color.BLACK)
        }
    }

    private fun startConnectingBlinkAnimation() {
        tvConnectingBlinkAnimator =
            ObjectAnimator.ofFloat(binding.tvConnecting, "alpha", 1f, 0f)
        tvConnectingBlinkAnimator?.duration = 500
        tvConnectingBlinkAnimator?.repeatMode = ValueAnimator.REVERSE
        tvConnectingBlinkAnimator?.repeatCount = ValueAnimator.INFINITE
        tvConnectingBlinkAnimator?.start()
    }

    private fun cancelConnectingBlinkAnimation() {
        tvConnectingBlinkAnimator?.cancel()
        binding.tvConnecting.alpha = 1.0f
    }

    private fun startReloadBtnAnimation() {
        if (reloadBtnBlinkAnimator == null || !reloadBtnBlinkAnimator!!.isStarted) {
            reloadBtnBlinkAnimator?.cancel()
            reloadBtnBlinkAnimator = ObjectAnimator.ofFloat(binding.btnReloadContainer, "alpha", 1f, 0f, 1f).apply {
                duration = 1000
                repeatCount = ValueAnimator.INFINITE
                repeatMode = ValueAnimator.RESTART
                interpolator = AccelerateDecelerateInterpolator()
                start()
            }
        }
    }

    private fun cancelReloadBtnBlinkAnimator() {
        reloadBtnBlinkAnimator?.cancel()
        reloadBtnBlinkAnimator = null
        binding.btnReloadContainer.alpha = 1.0f
    }

    private fun startUpdateBtnBlinkAnimation() {
        updateBtnBlinkAnimator?.cancel()
        updateBtnBlinkAnimator = ObjectAnimator.ofFloat(binding.btnUpdateContainer, "alpha", 1f, 0f, 1f)
        updateBtnBlinkAnimator?.duration = 1000
        updateBtnBlinkAnimator?.repeatCount = ObjectAnimator.INFINITE
        updateBtnBlinkAnimator?.repeatMode = ObjectAnimator.RESTART
        updateBtnBlinkAnimator?.interpolator = AccelerateDecelerateInterpolator()
        updateBtnBlinkAnimator?.start()
    }

    private fun cancelUpdateBtnBlinkAnimation() {
        updateBtnBlinkAnimator?.cancel()
        binding.btnUpdateContainer.alpha = 1.0f
    }

    /*private fun showUnitDataFetchedUI() {
        //        binding.bgChannelConnected.isVisible = true
        binding.tvDisarmedUnit.isVisible = false
        setLauncherButtonImage(true)
        if (vm.uiState.value?.selectedUnit?.isOnlySoundInstalled() == true) {
            binding.tvSound.isVisible = true
            binding.tvFire.isVisible = false
            binding.tvChannel.isVisible = false
            binding.tvChannelNo.isVisible = false
        } else {
            binding.tvFire.isVisible = true
            binding.tvChannel.isVisible = true
            binding.tvChannelNo.isVisible = true
            binding.tvSound.isVisible = false
            binding.tvBar.isVisible = vm.selectedUnitModel.value?.isServoVersion != true
            binding.tvBarNo.isVisible = vm.selectedUnitModel.value?.isServoVersion != true
            binding.tvBarNo.text = vm.fetchedUnitModel.value?.getBar()

            listOfUnitTvIds.forEach { textViewId ->
                val view = binding.root.findViewById<TextView>(textViewId) ?: return@forEach
                if (view.id == vm.selectedUnitTvId) {
                    view.setTextColor(getColor(Colors.black))
                    view.setBackgroundResource(Drawables.shape_rect_rounded_10_red)
//                view.backgroundTintList = ColorStateList.valueOf(getColor(Colors.colorRed))
                } else {
                    view.setTextColor(getColor(Colors.white))
                    view.setBackgroundResource(Drawables.shape_rect_rounded_border_10)
//                view.backgroundTintList = null
                }
            }
        }
        startBlinkAnimation()
    }

    private fun hideUnitDataFetchedUI() {
//        binding.bgChannelConnected.isVisible = false
        setLauncherButtonImage(false)
        binding.ivLauncher.alpha = 0.1f
        binding.ivLauncher.isClickable = false

        binding.tvFire.isVisible = false
        binding.tvChannel.isVisible = false
        binding.tvChannelNo.isVisible = false
        binding.tvSound.isVisible = false
        binding.tvBar.isVisible = false
        binding.tvBarNo.isVisible = false
        binding.tvVoltage.isVisible = false
        binding.ivVoltage.isVisible = false
        stopBlinkAnimation()

        listOfUnitTvIds.forEach { textViewId ->
            val view = binding.root.findViewById<TextView>(textViewId) ?: return
            if (view.id == vm.selectedUnitTvId) {
                view.setTextColor(getColor(Colors.black))
                view.setBackgroundResource(Drawables.shape_rect_rounded_10)
            } else {
                view.setTextColor(getColor(Colors.white))
                view.setBackgroundResource(Drawables.shape_rect_rounded_border_10)
            }
        }
    }*/

    private fun selectNextChannel() {
        val nextAvailableChannel = vm.uiState.value?.fetchedUnitModel?.getNextChannel()
        val noOfChannelAdded = vm.uiState.value?.selectedUnit?.noOfChannel ?: 0
        if (vm.uiState.value?.getNextAvailableChannel() != null) {
            startBlinkAnimation()
        }
        if (nextAvailableChannel == null || nextAvailableChannel > noOfChannelAdded) {

            stopBlinkAnimation()
        }
    }

    private fun handleUnitClick(unitNo: Int, unitModel: UnitModel?) {
        if (vm.uiState.value?.hasUnitSelected() == true) {
            selectUnit(unitNo, unitModel)
        } else if (prefs.advancedControlIsStandardEnable) {
            resetSelectUnit()
            val bundle =
                bundleOf(AppConstants.Communication.BundleData.INTENT_UNIT_MODEL to unitModel)
            startActivityForResult(ChannelListAct::class.java, channelListActResultLauncher, bundle = bundle)
        } else {
            selectUnit(unitNo, unitModel)
        }
    }

    private fun handleLongUnitClick(unitNo: Int, unitModel: UnitModel?) {
        if (vm.uiState.value?.hasUnitSelected() == true) {
            resetSelectUnit()
            val bundle =
                bundleOf(AppConstants.Communication.BundleData.INTENT_UNIT_MODEL to unitModel)
            startActivityForResult(ChannelListAct::class.java, channelListActResultLauncher, bundle = bundle)
        } else if (prefs.advancedControlIsStandardEnable) {
            selectUnit(unitNo, unitModel)
        } else {
            resetSelectUnit()
            val bundle =
                bundleOf(AppConstants.Communication.BundleData.INTENT_UNIT_MODEL to unitModel)
            startActivityForResult(ChannelListAct::class.java, channelListActResultLauncher, bundle = bundle)
        }
    }

    private fun selectUnit(unitNo: Int, unitModel: UnitModel?) {
        if (vm.uiState.value?.isThisUnitSelected(unitNo) == true) {
            val state = vm.uiState.value
            if (state?.isNoResponse() == true) {
                // Unit ist offline: Erneuter Abfrageversuch
                fetchUnitData(unitModel)
            } else if (vm.hasStayArmedActive()) {
                // Unit ist Scharf -> Entschärfen (Text "DISARMED UNIT X..." erscheint & Button wird weiß)
                resetTitleUI()
                vm.setUnitDisarmed(true)
            } else {
                // Unit ist Entschärft -> wieder Scharfschalten (Button wird rot)
                vm.setUnitDisarmed(false)
                setStayArmed()
            }
        } else {
            vm.setSelectedUnit(unitModel)
            fetchUnitData(unitModel)
        }
    }

    private fun resetSelectUnit() {
        vm.setSelectedUnit(null)
        vm.setFetchedUnitData(null)
        resetTitleUI() // GEÄNDERT: Titel & Akkubalken beim Abwählen zurücksetzen
        binding.llFieldBattery.isVisible = false // GEÄNDERT: Wird nur hier beim kompletten Abwählen ausgeblendet
        binding.btnReloadContainer.backgroundTintList = android.content.res.ColorStateList.valueOf(getColor(R.color.colorSkyBlue))
        binding.btnUpdateContainer.backgroundTintList = android.content.res.ColorStateList.valueOf(getColor(R.color.colorOrange))
        stopFailBlinkAnimation()
    }

    private val settingActResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
        vm.reloadState()
        connectGateway()
        vm.setSelectedUnit(null)
    }

    // GEÄNDERT: Empfängt die in der Advanced View gewählte Unit und wählt diese in der Simple View aus
    private val channelListActResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
        if (result.resultCode == RESULT_OK) {
            val intent = result.data
            val unitModel = if (intent != null) {
                androidx.core.content.IntentCompat.getSerializableExtra(
                    intent,
                    AppConstants.Communication.BundleData.INTENT_UNIT_MODEL,
                    UnitModel::class.java
                )
            } else null

            if (unitModel != null) {
                selectUnit(unitModel.unitNumber, unitModel)
            }
        }
    }

    override fun onClick(v: View) {
        super.onClick(v)
        v.triggerHaptic()
        when(v.id) {
            R.id.iv_launcher -> {
                when {
                    vm.uiState.value?.isGatewayNotConnected() == true -> connectGateway()
                    vm.uiState.value?.isGatewayConnected() == true -> {
                        if (vm.uiState.value?.selectedUnit != null) {
                            if (vm.uiState.value?.selectedUnit?.isOnlySoundInstalled() == true) {
                                binding.ivSpeaker.animateWave()
                                fireSoundCommand(vm.selectedSoundIndex)
                            } else {
                                fireSelectedChannel()
                            }
                        } else {
                            // NEU: Wenn keine Unit gewählt ist, trennt ein einfacher Klick die Verbindung (Disconnect)
                            bluetoothService?.disconnectBluetoothDevice()
                            setGatewayDisconnected()
                        }
                    }
                    else -> {
                    }
                }
            }

            R.id.iv_voltage, R.id.tv_voltage -> {
                lifecycleScope.launch {
                    setVoltageVisibility(false)
                    delay(2000)
                    setVoltageVisibility(true)
                }
            }

            R.id.btn_back -> {
                resetSelectUnit() // GEÄNDERT: Klick auf den Zurück-Pfeil hebt die Unit-Auswahl auf und geht zur Startseite
            }

            // GEÄNDERT: Tap auf den Titel "DISARM IN XXs" setzt den Countdown sauber auf die volle Zeit zurück
            R.id.textView -> {
                if (vm.uiState.value?.hasUnitSelected() == true && vm.hasStayArmedActive()) {
                    setStayArmed()
                }
            }

            R.id.btn_available_devices, R.id.btn_network, R.id.tv_save_gateway_info -> {
                startActivityForResult(ConnectionConfigAct::class.java, settingActResultLauncher)
            }

            R.id.btn_setting, R.id.tv_set_up_unit_info -> {
                startActivityForResult(MainConfigurationAct::class.java, settingActResultLauncher)
            }

            R.id.tv_1 -> {
                handleUnitClick(1, prefs.unit1Model)
            }

            R.id.tv_2 -> {
                handleUnitClick(2, prefs.unit2Model)
            }

            R.id.tv_3 -> {
                handleUnitClick(3, prefs.unit3Model)
            }

            R.id.tv_4 -> {
                handleUnitClick(4, prefs.unit4Model)
            }

            R.id.tv_disarmed_unit -> {
                vm.setUnitDisarmed(false)
                setStayArmed()
            }

            R.id.btn_reload -> {
                v.animateRotate()
                reloadUnitData()
            }

            R.id.btn_update -> {
                v.animateHorizontalFlip()
                fetchUnitData(vm.uiState.value?.selectedUnit)
            }

            R.id.btn_sound -> {
                v.animateWave()
                fireSoundCommand()
            }

            R.id.btn_sound_duck -> {
                selectSound(0)
            }

            R.id.btn_sound_pheasant -> {
                selectSound(1)
            }

            R.id.btn_sound_goose -> {
                selectSound(2)
            }

            R.id.btn_sound_brrr -> {
                selectSound(3)
            }

            R.id.btn_sound_gunshot -> {
                selectSound(4)
            }

            R.id.btn_sound_magpie -> {
                selectSound(5)
            }

            R.id.btn_program_settings -> {
                showToast("Program Settings")
            }

            R.id.btn_start_program -> {
                showToast("Start Program")
            }
        }
    }

    private fun selectSound(index: Int) {
        vm.selectedSoundIndex = index
        binding.invalidateAll()
    }

    private fun setGatewayConnected() {
        startGatewaySignalStrengthUpdate()
        vm.setGatewayConnected()
    }

    private fun setGatewayDisconnected() {
        stopGatewaySignalStrengthUpdate()
        vm.setGatewayDisconnected()
        // GEÄNDERT: Batterie-Icon & Spannungstext bei getrenntem Gateway ausblenden
        binding.ivVoltage.isVisible = false
        binding.tvVoltage.isVisible = false
    }

    override fun onPause() {
        super.onPause()
        resetTitleUI() // GEÄNDERT: Stoppt laufenden Disarm-Timer beim Wechsel in andere Ansichten
        stopBlinkAnimation()
    }

    override fun onResume() {
        super.onResume()
        startBlinkAnimation()

        /**
         * Reload unit
         */
        val unitNumber = vm.uiState.value?.selectedUnit?.unitNumber ?: -1
        vm.setSelectedUnit(vm.getUnitModel(unitNumber))

        if (vm.uiState.value?.hasUnitSelected() == true) {
            vm.setUnitDisarmed(false)
            setStayArmed()
        }
    }
}