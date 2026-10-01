package com.launcher_control_android.helper.util

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.Gravity
import android.view.animation.LinearInterpolator
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.graphics.drawable.GradientDrawable
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.launcher_control_android.R
import kotlinx.coroutines.*

class ProgramPrimingHelper(
    private val context: Context,
    private val scope: CoroutineScope,
    private val onSendCommand: (String) -> Unit,
    private val onStartSequence: () -> Unit
) {
    private var pollingJob: Job? = null
    private val fanViewsMap = mutableMapOf<Int, ImageView>()
    private val fanAnimatorsMap = mutableMapOf<Int, ObjectAnimator?>() // 🎯 Nullable wie in LauncherControlAct
    private val readyUnits = mutableSetOf<Int>()
    private val respondingUnits = mutableSetOf<Int>()
    private val failedOrNotReachedUnits = mutableSetOf<Int>()
    private var alertDialog: AlertDialog? = null
    private var btnStartSequence: Button? = null

    private var btnPrimeRef: Button? = null // 🎯 Referenz auf den Prime-Button für den Countdown
    private var btnPrimeAnimator: ObjectAnimator? = null // 🎯 Animation für den pulsierenden Prime-Button
    private var btnStartAnimator: ObjectAnimator? = null // 🎯 Animation für den pulsierenden Start-Button
    private var isPollingPhaseActive = false // 🎯 Schutz: Abwurf ERST ab Phase 4 erlauben!

    private fun createRoundedDrawable(colorInt: Int, cornerRadiusDp: Float = 8f): GradientDrawable {
        val density = context.resources.displayMetrics.density
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = cornerRadiusDp * density
            setColor(colorInt) // 🎯 Nimmt direkt den aufgelösten Color-Int Wert entgegen
        }
    }

    private fun createRoundedDrawable(colorHex: String, cornerRadiusDp: Float = 8f): GradientDrawable {
        return createRoundedDrawable(Color.parseColor(colorHex), cornerRadiusDp)
    }

    fun showPreFlightDialog(requiredUnits: List<Int>) {
        cancel()
        readyUnits.clear()
        respondingUnits.clear()
        fanViewsMap.clear()
        fanAnimatorsMap.clear()

        val density = context.resources.displayMetrics.density
        val paddingPx = (16 * density).toInt()

        val builder = AlertDialog.Builder(context)

        // 🎯 Zentrierter Titel als Custom Title View
        val tvTitle = TextView(context).apply {
            text = "CHECKING, RELEASING AND PRIMING ALL UNITS"
            textSize = 20f
            setTextColor(Color.BLACK)
            gravity = Gravity.CENTER
            setPadding(paddingPx, paddingPx, paddingPx, 0)
            setTypeface(null, android.graphics.Typeface.BOLD)
        }
        builder.setCustomTitle(tvTitle)

        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(paddingPx, paddingPx, paddingPx, paddingPx)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        requiredUnits.forEach { unitNo ->
            val rowLayout = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                // 🎯 Seitliches Padding von 24dp rückt die Elemente weiter zur Mitte
                setPadding((30 * density).toInt(), (8 * density).toInt(), (24 * density).toInt(), (8 * density).toInt())
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }

            val tvUnit = TextView(context).apply {
                text = "Unit $unitNo"
                textSize = 24f // 🎯 Textgröße auf 20f erhöht
                setTextColor(Color.BLACK)
                alpha = 0.4f
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            val ivFan = ImageView(context).apply {
                setImageResource(R.drawable.ic_fan)
                imageTintList = ColorStateList.valueOf(Color.DKGRAY) // 🎯 Dunkelgrau für abgedimmten Zustand
                alpha = 0.4f
                val iconSize = (32 * context.resources.displayMetrics.density).toInt()
                layoutParams = LinearLayout.LayoutParams(iconSize, iconSize)
            }

            rowLayout.addView(tvUnit)
            rowLayout.addView(ivFan)
            container.addView(rowLayout)
            fanViewsMap[unitNo] = ivFan
            unitTextViewsMap[unitNo] = tvUnit
        }

        // 🎯 Punkt 2: Eigene Button-Leiste für garantierte Reihenfolge
        val buttonBar = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL // 🎯 Vertikal untereinander
            val topMargin = (16 * density).toInt()
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, topMargin, 0, 0)
            }
        }

        val btnMargin = (6 * density).toInt()
        val btnLayoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            setMargins(0, btnMargin, 0, btnMargin)
        }

        val btnPrime = Button(context).apply {
            text = "RELEASE & PRIME"
            textSize = 20f
            setTextColor(Color.WHITE)
            background = createRoundedDrawable("#FFA500")
            layoutParams = btnLayoutParams
        }

        val btnStart = Button(context).apply {
            text = "OPEN SEQUENCE"
            textSize = 20f
            setTextColor(Color.WHITE)
            isEnabled = false
            alpha = 0.3f
            background = createRoundedDrawable("#808080")
            isEnabled = false
            layoutParams = btnLayoutParams
        }

        val btnCancel = Button(context).apply {
            text = "Cancel"
            textSize = 20f
            setTextColor(Color.WHITE)
            background = createRoundedDrawable("#D32F2F")
            layoutParams = btnLayoutParams
        }

        btnStartSequence = btnStart
        btnPrimeRef = btnPrime

        // 🎯 Orangener Button pulsiert sofort beim Öffnen der Seite als Handlungsaufforderung
        btnPrimeAnimator?.cancel()
        btnPrimeAnimator = ObjectAnimator.ofFloat(btnPrime, "alpha", 1.0f, 0.3f, 1.0f).apply {
            duration = 800
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            start()
        }

        // 🎯 Klick stoppt den Vorgang wenn er läuft, oder startet ihn neu
        btnPrime.setOnClickListener {
            if (pollingJob?.isActive == true) {
                stopPrimingProcess()
            } else {
                startPrimingProcess(requiredUnits)
            }
        }
        btnStart.setOnClickListener { cancel(); alertDialog?.dismiss(); onStartSequence() }
        btnCancel.setOnClickListener { cancel(); alertDialog?.dismiss() }

        buttonBar.addView(btnPrime)
        buttonBar.addView(btnStart)
        buttonBar.addView(btnCancel)
        container.addView(buttonBar)

        builder.setView(container)

        alertDialog = builder.create().apply {
            setCanceledOnTouchOutside(false) // 🎯 Verhindert das Schließen beim Klick in den Hintergrund
            // 🎯 Fenster-Hintergrund weiß mit abgerundeten Ecken (hier z. B. 16dp testen)
            window?.setBackgroundDrawable(createRoundedDrawable("#FFFFFF", 16f))
            show()
        }
    }

    private fun stopPrimingProcess() {
        pollingJob?.cancel()
        pollingJob = null
        btnPrimeAnimator?.cancel() // 🎯 Pulsieren sofort stoppen
        btnPrimeAnimator = null
        btnPrimeRef?.alpha = 1.0f   // 🎯 Volle Deckkraft wiederherstellen
        fanAnimatorsMap.values.forEach { it?.cancel() }
        fanAnimatorsMap.clear()
        btnPrimeRef?.text = "RELEASE & PRIME"
        btnPrimeRef?.isEnabled = true
    }

    private val unitTextViewsMap = mutableMapOf<Int, TextView>()

    // In showPreFlightDialog beim Erstellen der Zeilen: unitTextViewsMap[unitNo] = tvUnit hinzufügen!

    private fun startPrimingProcess(requiredUnits: List<Int>) {
        pollingJob?.cancel()
        pollingJob = scope.launch {
            // 🎯 0. Alle alten Zustände & Animationen vollständig zurücksetzen
            readyUnits.clear()
            respondingUnits.clear()
            withContext(Dispatchers.Main) {
                fanAnimatorsMap.values.forEach { it?.cancel() }
                fanAnimatorsMap.clear()

                requiredUnits.forEach { unitNo ->
                    unitTextViewsMap[unitNo]?.apply {
                        text = "Unit $unitNo"
                        setTextColor(Color.BLACK)
                        alpha = 0.4f
                    }
                    fanViewsMap[unitNo]?.apply {
                        imageTintList = ColorStateList.valueOf(Color.DKGRAY)
                        alpha = 0.4f
                        rotation = 0f
                    }
                }

                btnStartSequence?.isEnabled = false
                btnStartSequence?.alpha = 0.3f
                btnStartSequence?.background = createRoundedDrawable("#808080")
            }

            val startTime = System.currentTimeMillis()

            // 🎯 Sanftes Pulsen des orangenen Buttons (1.0f <-> 0.8f)
            withContext(Dispatchers.Main) {
                btnPrimeAnimator?.cancel()
                btnPrimeAnimator = ObjectAnimator.ofFloat(btnPrimeRef, "alpha", 1.0f, 0.8f).apply {
                    duration = 700
                    repeatCount = ValueAnimator.INFINITE
                    repeatMode = ValueAnimator.REVERSE
                    start()
                }
            }

            // 🎯 Countdown-Job startet sofort ab Sekunde 0
            val countdownJob = scope.launch(Dispatchers.Main) {
                while (isActive) {
                    val elapsed = ((System.currentTimeMillis() - startTime) / 1000L).toInt()
                    val remaining = (40 - elapsed).coerceAtLeast(0)
                    btnPrimeRef?.isEnabled = true
                    btnPrimeRef?.text = "Waiting for units... $remaining"
                    if (remaining <= 0) break
                    delay(500L)
                }
            }

            try {
                isPollingPhaseActive = false
                // 🎯 1. Release-Befehle STRENG SEQUENTIELL senden & direkt prüfen
                requiredUnits.forEach { unitNo ->
                    onSendCommand("${unitNo}0")
                    delay(1000L) // 1 Sekunde Fenster für die Rückmeldung

                    // 🎯 Sofort-Check: Keine Rückmeldung in 1s? -> Rot + "not reached" setzen
                    if (!respondingUnits.contains(unitNo)) {
                        failedOrNotReachedUnits.add(unitNo) // 🎯 Als final nicht erreicht markieren
                        withContext(Dispatchers.Main) {
                            unitTextViewsMap[unitNo]?.apply {
                                alpha = 1.0f
                                setTextColor(Color.RED)
                                text = "Unit $unitNo - not reached"
                            }
                            fanViewsMap[unitNo]?.apply {
                                alpha = 1.0f
                                imageTintList = ColorStateList.valueOf(Color.RED)
                            }
                        }
                    }
                }

                // 🎯 2. 5 Sekunden Pause für den Druckaufbau
                delay(5000L)

                // 🎯 3. Status für noch nicht geantwortete Units aktualisieren (KEIN return@launch, Countdown läuft weiter!)
                val missingUnits = requiredUnits.filter { unitNo -> !respondingUnits.contains(unitNo) }
                if (missingUnits.isNotEmpty()) {
                    withContext(Dispatchers.Main) {
                        missingUnits.forEach { missingUnit ->
                            unitTextViewsMap[missingUnit]?.apply {
                                alpha = 1.0f
                                setTextColor(Color.RED)
                                text = "Unit $missingUnit - not reached"
                            }
                            fanViewsMap[missingUnit]?.apply {
                                alpha = 1.0f
                                imageTintList = ColorStateList.valueOf(Color.RED)
                            }
                        }
                    }
                }

                // 🎯 4. Reihum-Polling bis 50s abgelaufen sind
                isPollingPhaseActive = true // 🎯 Jetzt läuft Phase 4 -> Abwurf bei Fertigstellung erlaubt!
                var unitIndex = 0
                while (isActive) {
                    val elapsed = ((System.currentTimeMillis() - startTime) / 1000L).toInt()
                    if (elapsed >= 40) {
                        withContext(Dispatchers.Main) {
                            requiredUnits.filter { !readyUnits.contains(it) }.forEach { unreadyUnit ->
                                // 🎯 Pumpen stoppen & Animation abbrechen
                                fanAnimatorsMap[unreadyUnit]?.cancel()
                                fanAnimatorsMap[unreadyUnit] = null

                                val tvUnit = unitTextViewsMap[unreadyUnit]
                                val currentText = tvUnit?.text?.toString() ?: ""

                                // 🎯 AUSNAHME: Falls bereits "Fail" gesetzt ist, bleibt der Text "Unit X - Fail" unverändert!
                                if (!currentText.contains("Fail")) {
                                    tvUnit?.apply {
                                        alpha = 1.0f
                                        setTextColor(Color.RED)
                                        text = if (respondingUnits.contains(unreadyUnit)) "Unit $unreadyUnit - check Unit" else "Unit $unreadyUnit - not reached"
                                    }
                                }

                                fanViewsMap[unreadyUnit]?.apply {
                                    alpha = 1.0f
                                    imageTintList = ColorStateList.valueOf(Color.RED)
                                }
                            }
                        }
                        break
                    }

                    val unitNo = requiredUnits[unitIndex]
                    val fetchHex = when (unitNo) {
                        1 -> "1E"
                        2 -> "2E"
                        3 -> "3E"
                        4 -> "4E"
                        else -> null
                    }
                    if (fetchHex != null) {
                        onSendCommand(fetchHex)
                    }
                    unitIndex = (unitIndex + 1) % requiredUnits.size
                    delay(1000L)
                }
            } finally {
                countdownJob.cancel()
                // 🎯 NonCancellable garantiert die Ausführung des Aufräumcodes auch bei Coroutine-Abbruch
                withContext(NonCancellable + Dispatchers.Main) {
                    btnPrimeAnimator?.cancel()
                    btnPrimeAnimator = null
                    btnPrimeRef?.alpha = 1.0f
                    btnPrimeRef?.text = "RELEASE & PRIME"
                    btnPrimeRef?.isEnabled = true
                }
            }
        }
    }


    fun onTelemetryReceived(
        unitNo: Int,
        hasStatusUpdate: Boolean,
        isReady: Boolean,
        isFail: Boolean,
        requiredUnits: List<Int>
    ) {
        // Schutzabfrage: Nur auswerten, wenn der Dialog tatsächlich geöffnet ist
        if (alertDialog == null || alertDialog?.isShowing != true) return

        val ivFan = fanViewsMap[unitNo] ?: return
        val tvUnit = unitTextViewsMap[unitNo] ?: return

        if (hasStatusUpdate) {
            respondingUnits.add(unitNo)
            tvUnit.alpha = 1.0f

            // 🎯 1. Priorität: Unit meldet explizit FAIL -> Zwingend "Unit X - Fail" setzen
            if (isFail) {
                fanAnimatorsMap[unitNo]?.cancel()
                fanAnimatorsMap[unitNo] = null
                tvUnit.setTextColor(Color.RED)
                tvUnit.text = "Unit $unitNo - Fail"
                ivFan.alpha = 1.0f
                ivFan.imageTintList = ColorStateList.valueOf(Color.RED)
                readyUnits.remove(unitNo)
                failedOrNotReachedUnits.add(unitNo)
            }
            // 🎯 2. Priorität: Solldruck erreicht -> "Unit X - Ready"
            else if (isReady) {
                fanAnimatorsMap[unitNo]?.cancel()
                fanAnimatorsMap[unitNo] = null
                ivFan.alpha = 1.0f
                val appGreen = androidx.core.content.ContextCompat.getColor(context, R.color.colorGreen)
                ivFan.imageTintList = ColorStateList.valueOf(appGreen)
                tvUnit.setTextColor(appGreen)
                tvUnit.text = "Unit $unitNo - Ready"
                readyUnits.add(unitNo)
                failedOrNotReachedUnits.remove(unitNo)
            }
            // 🎯 3. Priorität: Druckaufbau läuft -> "Unit X - checking"
            else {
                tvUnit.setTextColor(Color.BLACK)
                tvUnit.text = "Unit $unitNo - checking"
                if (fanAnimatorsMap[unitNo] == null) {
                    ivFan.alpha = 1.0f
                    ivFan.imageTintList = ColorStateList.valueOf(Color.parseColor("#FFA500"))
                    val animator = ObjectAnimator.ofFloat(ivFan, "rotation", 0f, 360f).apply {
                        duration = 1200
                        repeatCount = ValueAnimator.INFINITE
                        interpolator = LinearInterpolator()
                        start()
                    }
                    fanAnimatorsMap[unitNo] = animator
                }
                readyUnits.remove(unitNo)
                failedOrNotReachedUnits.remove(unitNo)
            }
        }

        // 🎯 Früher Abwurf NUR wenn die Polling-Phase (Phase 4) wirklich aktiv ist!
        if (isPollingPhaseActive) {
            val allFinished = requiredUnits.all { readyUnits.contains(it) || failedOrNotReachedUnits.contains(it) }

            if (allFinished) {
                pollingJob?.cancel()
                pollingJob = null
                isPollingPhaseActive = false
                btnPrimeAnimator?.cancel()
                btnPrimeAnimator = null
                btnPrimeRef?.alpha = 1.0f
                btnPrimeRef?.text = "RELEASE & PRIME"
                btnPrimeRef?.isEnabled = true
            }
        }

        // 🎯 START SEQUENCE wird NUR freigeschaltet, wenn wirklich ALLE Units READY sind
        if (readyUnits.containsAll(requiredUnits)) {
            // 🎯 Hier wird Countdown & Polling gestoppt:
            pollingJob?.cancel()
            pollingJob = null
            btnPrimeAnimator?.cancel()
            btnPrimeAnimator = null
            btnPrimeRef?.alpha = 1.0f
            btnPrimeRef?.text = "RELEASE & PRIME"
            btnPrimeRef?.isEnabled = true
            btnStartSequence?.isEnabled = true
            val greenColor = androidx.core.content.ContextCompat.getColor(context, R.color.colorGreen)
            btnStartSequence?.background = createRoundedDrawable(greenColor)

            // 🎯 Grüner Button fängt an zu blinken/pulsieren
            if (btnStartAnimator == null) {
                btnStartAnimator = ObjectAnimator.ofFloat(btnStartSequence, "alpha", 1.0f, 0.3f, 1.0f).apply {
                    duration = 800
                    repeatCount = ValueAnimator.INFINITE
                    repeatMode = ValueAnimator.REVERSE
                    start()
                }
            }
        } else {
            btnStartSequence?.isEnabled = false
            btnStartSequence?.alpha = 0.3f
            btnStartSequence?.background = createRoundedDrawable("#808080")
        }
    }

    fun cancel() {
        pollingJob?.cancel()
        pollingJob = null
        btnPrimeAnimator?.cancel()
        btnPrimeAnimator = null
        btnStartAnimator?.cancel()
        btnStartAnimator = null
        btnPrimeRef?.alpha = 1.0f
        fanAnimatorsMap.values.forEach { it?.cancel() }
        btnPrimeRef?.text = "RELEASE & PRIME"
        btnPrimeRef?.isEnabled = true
        btnPrimeRef = null
        btnStartSequence = null
    }
}