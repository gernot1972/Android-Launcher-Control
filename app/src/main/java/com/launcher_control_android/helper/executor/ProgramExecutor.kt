package com.launcher_control_android.helper.executor

import com.launcher_control_android.data.model.response.ProgramItemModel
import com.launcher_control_android.data.model.response.UnitModel
import com.launcher_control_android.helper.util.ProgramTimelineItem
import com.launcher_control_android.helper.util.TimelineRowType
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ProgramExecutor(
    private val context: android.content.Context,
    private val scope: CoroutineScope,
    private val onSendCommand: (String) -> Unit,
    private val onGetNextChannel: ((unit: Int) -> Int?)? = null,
    private val onChannelFired: ((unit: Int, channel: Int) -> Unit)? = null
) {
    private val _uiState = MutableStateFlow(ProgramExecutionState())
    val uiState: StateFlow<ProgramExecutionState> = _uiState.asStateFlow()

    private var executionJob: Job? = null
    private var currentSequence: List<ProgramItemModel> = emptyList()

    /**
     * Lädt die Sequenz für die Timeline-Anzeige, ohne sie sofort zu starten
     */
    fun prepareExecution(sequence: List<ProgramItemModel>) {
        stopProgram()
        currentSequence = sequence
        val firstDelay = sequence.firstOrNull()?.delay ?: 0
        _uiState.value = ProgramExecutionState(
            isRunning = true,
            isPaused = true,
            currentItem = -1,
            remainingDelaySeconds = firstDelay, // 🎯 Ersten Delay-Wert beim Laden der Sequenz setzen
            isSequenceEnd = false
        )
    }

    /**
     * Startet die Sequenz von vorne (bei Schritt 0)
     */
    fun startExecution(sequence: List<ProgramItemModel>) {
        stopProgram()
        currentSequence = sequence
        _uiState.value = ProgramExecutionState(
            isRunning = true,
            isPaused = false,
            currentItem = 0,
            isSequenceEnd = false
        )
        runNextStep(fromResume = false)
    }

    /**
     * Generiert die Liste aller Timeline-Items inkl. START (#0) und FINISH (#N+1)
     */
    fun getTimelineItems(): List<ProgramTimelineItem> {
        val items = mutableListOf<ProgramTimelineItem>()
        val isSequenceEnd = _uiState.value.isSequenceEnd
        val currentItem = _uiState.value.currentItem

        val validCount = currentSequence.count { it.isValidSequence }

        // 🎯 1. START-Item ganz oben (Index -1)
        items.add(
            ProgramTimelineItem(
                id = "start",
                sequenceIndex = 0,
                rowType = TimelineRowType.START,
                displayText = "START",
                isActive = (currentItem == -1),
                isCompleted = true
            )
        )

        // 🎯 2. Aktions-Schritte (Index 0..N-1)
        currentSequence.forEachIndexed { idx, program ->
            if (!program.isValidSequence) return@forEachIndexed

            val isActive = (idx == currentItem)
            val isCompleted = (idx < currentItem || currentItem == validCount)
            val isStrikethrough = (idx < currentItem || currentItem == validCount)
            val displayText = if (program.isEligibleForFire) {
                val channelSuffix = if (program.targetChannel > 0) " - Ch${program.targetChannel}" else ""
                "U${program.fireUnit} - Launch$channelSuffix"
            } else if (program.isUseSound) {
                val volPercent = when (program.volumeStep) {
                    1 -> "15%"
                    2 -> "30%"
                    3 -> "50%"
                    else -> "100%"
                }
                "U${program.fireUnit} - ${program.shortActionName} - $volPercent"
            } else {
                "U${program.fireUnit} - Action"
            }

            items.add(
                ProgramTimelineItem(
                    id = "step_$idx",
                    sequenceIndex = idx + 1,
                    rowType = TimelineRowType.ACTION,
                    isLaunch = program.isEligibleForFire,
                    displayText = displayText,
                    isActive = isActive,
                    isCompleted = isCompleted,
                    isStrikethrough = isStrikethrough,
                    lineProgress = if (isActive) _uiState.value.lineProgress else if (isCompleted) 1.0f else 0.0f
                )
            )
        }

        // 🎯 3. FINISH-Item ganz unten (künstliches Ende)
        items.add(
            ProgramTimelineItem(
                id = "finish",
                sequenceIndex = validCount + 1,
                rowType = TimelineRowType.FINISH,
                displayText = "FINISH",
                isActive = isSequenceEnd,
                isCompleted = isSequenceEnd,
                isStrikethrough = false
            )
        )

        return items
    }


    /**
     * Führt den aktuellen Schritt aus und zählt das Delay herunter
     */
    private fun runNextStep(fromResume: Boolean, isManualJump: Boolean = false) {
        val currentIndex = _uiState.value.currentItem
        if (currentIndex !in currentSequence.indices) {
            stopProgram()
            return
        }

        val programItem = currentSequence[currentIndex]
        if (!programItem.isValidSequence) {
            stopProgram()
            return
        }

        executionJob = scope.launch(Dispatchers.Default) {
            // 🎯 Exakte Unterscheidung: Manueller Sprung -> 0s Delay, Automatischer Ablauf -> Reguläres Liste-Delay
            val totalDelaySeconds = if (fromResume) {
                _uiState.value.remainingDelaySeconds // 0s bei manuellem Sprung im Pause-Zustand, >0s beim Fortsetzen
            } else if (isManualJump) {
                0 // 0s bei manuellem Sprung im laufenden Betrieb
            } else {
                maxOf(0, programItem.delay - 1) // Reguläres Schritt-Delay aus der Liste für automatischen Ablauf
            }
            val delayMillis = (totalDelaySeconds * 1000).toLong()
            val startTime = System.currentTimeMillis()
            var lastKeepAliveTime = System.currentTimeMillis()

            // Countdown-Schleife (alle 100ms Aktualisierung für flüssigen Fortschritt)
            while (isActive && _uiState.value.isRunning && !_uiState.value.isPaused) {
                val elapsed = System.currentTimeMillis() - startTime
                val progress = (elapsed.toFloat() / delayMillis).coerceIn(0f, 1f)
                val remainingSec = maxOf(0, kotlin.math.ceil((delayMillis - elapsed) / 1000.0).toInt())

                _uiState.value = _uiState.value.copy(
                    lineProgress = progress,
                    remainingDelaySeconds = remainingSec
                )

                // 🎯 Gezielte Prüfung: Nur wenn das aktuelle Schritt-Delay länger als 3 Minuten (180s) ist,
                // senden wir alle 3 Minuten ein Keep-Alive-Signal an die Remote
                if (totalDelaySeconds > 180) {
                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastKeepAliveTime >= 180_000L) {
                        lastKeepAliveTime = currentTime
                        sendKeepAlivePing()
                    }
                }

                if (progress >= 1f) break
                delay(100)
            }

            if (!isActive || _uiState.value.isPaused) return@launch

            _uiState.value = _uiState.value.copy(isExecutingAction = true)
            performAction(programItem)

            if (_uiState.value.isRunning && !_uiState.value.isPaused) {
                delay(1000) // 1 Sekunde Wartezeit nach dem Befehl
                _uiState.value = _uiState.value.copy(isExecutingAction = false)
                onSequenceComplete()
            }
        }
    }

    /**
     * STOP - Pausiert die Ausführung oder setzt sie an der aktuellen Stelle fort
     */
    fun stopProgram() {
        executionJob?.cancel()
        executionJob = null
        _uiState.value = ProgramExecutionState(isRunning = false, isPaused = false)
        com.launcher_control_android.helper.service.ProgramExecutionService.stopService(context)
    }

    /**
     * PAUSE - Pausiert die Ausführung oder setzt sie an der aktuellen Stelle fort
     */
    fun togglePause() {
        val validCount = currentSequence.count { it.isValidSequence }
        // 🎯 Bei START (-1) oder FINISH (N): Bei Schritt #1 (Index 0) starten
        if (_uiState.value.currentItem == -1 || _uiState.value.currentItem >= validCount || !_uiState.value.isRunning) {
            if (validCount > 0) {
                _uiState.value = _uiState.value.copy(
                    isRunning = true,
                    isPaused = false,
                    isSequenceEnd = false,
                    currentItem = 0,
                    lineProgress = 0f,
                    remainingDelaySeconds = currentSequence[0].delay
                )
                runNextStep(fromResume = false)
            }
            return
        }
        val newPausedState = !_uiState.value.isPaused
        _uiState.value = _uiState.value.copy(isPaused = newPausedState)

        if (!newPausedState) {
            runNextStep(fromResume = true)
        } else {
            executionJob?.cancel()
            executionJob = null
        }
    }

    /**
     * Springt manuell zum nächsten Schritt
     */
    fun stepForward() {
        val validCount = currentSequence.count { it.isValidSequence }
        if (validCount == 0 || _uiState.value.currentItem >= validCount) return

        executionJob?.cancel()
        executionJob = null
        val nextIndex = _uiState.value.currentItem + 1
        val wasExecuting = !_uiState.value.isPaused && _uiState.value.currentItem >= 0

        if (nextIndex == validCount) {
            // 🎯 Ziel ist FINISH
            _uiState.value = _uiState.value.copy(
                currentItem = validCount,
                isSequenceEnd = true,
                isPaused = true
            )
        } else {
            // 🎯 Ziel ist ein Aktionsschritt (0..N-1)
            _uiState.value = _uiState.value.copy(
                currentItem = nextIndex,
                isSequenceEnd = false,
                lineProgress = 1.0f,
                remainingDelaySeconds = 0
            )
            if (wasExecuting) {
                runNextStep(fromResume = false, isManualJump = true)
            }
        }
    }

    /**
     * Springt durch Antippen einer Zeile direkt zum gewählten Schritt (nur erlaubt im Pausenmodus)
     */
    fun jumpToStep(targetIndex: Int) {
        // Navigation per Tapping nur im Pausenmodus zulassen
        if (!_uiState.value.isPaused) return

        if (targetIndex !in currentSequence.indices || !currentSequence[targetIndex].isValidSequence) return
        executionJob?.cancel()
        executionJob = null

        _uiState.value = _uiState.value.copy(
            isRunning = true,
            isPaused = true,
            isSequenceEnd = false,
            currentItem = targetIndex,
            lineProgress = 1.0f, // 🎯 Linie zum Zielschritt sofort 100% gezeichnet
            remainingDelaySeconds = 0 // 🎯 Delay auf 0, damit bei Play sofort gefeuert wird
        )
    }

    fun stepBackward() {
        val validCount = currentSequence.count { it.isValidSequence }
        if (validCount == 0 || _uiState.value.currentItem <= -1) return

        executionJob?.cancel()
        executionJob = null
        val prevIndex = _uiState.value.currentItem - 1
        val wasExecuting = !_uiState.value.isPaused && _uiState.value.currentItem in 0 until validCount

        if (prevIndex == -1) {
            // 🎯 Ziel ist START
            _uiState.value = _uiState.value.copy(
                currentItem = -1,
                isSequenceEnd = false,
                isPaused = true,
                lineProgress = 0f,
                remainingDelaySeconds = currentSequence.firstOrNull()?.delay ?: 0
            )
        } else {
            // 🎯 Ziel ist ein Aktionsschritt (0..N-1)
            _uiState.value = _uiState.value.copy(
                currentItem = prevIndex,
                isSequenceEnd = false,
                lineProgress = 1.0f,
                remainingDelaySeconds = 0
            )
            if (wasExecuting) {
                runNextStep(fromResume = false, isManualJump = true)
            }
        }
    }


    /**
     * Startet die aktuelle Sequenz wieder bei Schritt 0
     */
    fun restartProgram() {
        if (currentSequence.isEmpty()) return
        executionJob?.cancel()
        executionJob = null
        _uiState.value = _uiState.value.copy(
            isRunning = true,
            isPaused = true,
            currentItem = -1,
            lineProgress = 0f,
            remainingDelaySeconds = currentSequence.firstOrNull()?.delay ?: 0,
            isSequenceEnd = false
        )
    }


    /**
     * Sendet den passenden Bluetooth-Befehl für den aktuellen Schritt
     */
    private fun performAction(item: ProgramItemModel) {
        if (item.isEligibleForFire) {
            // Wenn targetChannel == 0 (angezeigt als "-"), wird der nächste freie Kanal genommen.
            // Wenn targetChannel in 1..12 ist, wird direkt dieser spezifische Kanal gefeuert.
            val channelToFire = if (item.targetChannel == 0) {
                onGetNextChannel?.invoke(item.fireUnit)
            } else {
                item.targetChannel
            }

            if (channelToFire != null) {
                val channelHex = when (channelToFire) {
                    1 -> "1"; 2 -> "2"; 3 -> "3"; 4 -> "4"
                    5 -> "5"; 6 -> "6"; 7 -> "7"; 8 -> "8"
                    9 -> "9"; 10 -> "a"; 11 -> "b"; 12 -> "c"
                    else -> "1"
                }
                onSendCommand("${item.fireUnit}$channelHex")
                onChannelFired?.invoke(item.fireUnit, channelToFire)
            } else {
                _uiState.value = _uiState.value.copy(
                    needToShowReload = true,
                    isPaused = true
                )
            }
        } else if (item.isUseSound) {
            val soundIndex = item.action - 2 // action 2 = Duck (0), 3 = Pheasant (1), etc.
            val tempUnit = UnitModel(
                unitNumber = item.fireUnit,
                noOfChannel = 0,
                isSoundOptionInstalled = true,
                isServoVersion = false,
                selectedSound = soundIndex,
                selectedPressure = 0,
                volumeStep = item.volumeStep
            )
            val hexCode = tempUnit.soundHexCode(soundIndex = soundIndex, volumeStep = item.volumeStep)
            if (hexCode != null) {
                onSendCommand(hexCode)
            }
        }
    }

    /**
     * Schaltet zum nächsten Schritt weiter oder beendet die Sequenz
     */
    private fun onSequenceComplete() {
        val validCount = currentSequence.count { it.isValidSequence }
        val nextIndex = _uiState.value.currentItem + 1

        if (nextIndex >= validCount) {
            scope.launch(Dispatchers.Default) {
                _uiState.value = _uiState.value.copy(
                    currentItem = validCount,
                    isSequenceEnd = true,
                    isRunning = false,
                    isPaused = true
                )
            }
            return
        }

        _uiState.value = _uiState.value.copy(currentItem = nextIndex)
        runNextStep(fromResume = false)
    }

    /**
     * Sendet alle 3 Minuten einen Keep-Alive-Befehl für die erste in der Sequenz genutzte Unit (z. B. "1E", "2E"),
     * um ein automatisches Abschalten der Fernbedienung (5-Minuten Kill-Timer) zu verhindern.
     */
    private fun sendKeepAlivePing() {
        val targetUnit = currentSequence.firstOrNull { it.isValidSequence }?.fireUnit ?: 1
        val keepAliveCmd = when (targetUnit) {
            1 -> "1E"
            2 -> "2E"
            3 -> "3E"
            4 -> "4E"
            else -> "1E"
        }
        onSendCommand(keepAliveCmd)
    }
}