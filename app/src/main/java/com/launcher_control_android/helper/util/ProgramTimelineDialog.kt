package com.launcher_control_android.helper.util

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.launcher_control_android.R
import com.launcher_control_android.helper.executor.ProgramExecutor
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest

class ProgramTimelineDialog(
    private val context: Context,
    private val scope: CoroutineScope,
    private val executor: ProgramExecutor
) {
    private var alertDialog: AlertDialog? = null
    private var updateJob: Job? = null

    private fun createRoundedDrawable(colorHex: String, cornerRadiusDp: Float = 16f): GradientDrawable {
        val density = context.resources.displayMetrics.density
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = cornerRadiusDp * density
            setColor(Color.parseColor(colorHex))
        }
    }

    fun show() {
        dismiss()

        val density = context.resources.displayMetrics.density
        val paddingPx = (16 * density).toInt()

        val builder = AlertDialog.Builder(context)

        // 🎯 Root-Layout als FrameLayout, um das Sperr-Overlay darüber legen zu können
        val rootLayout = android.widget.FrameLayout(context)

        val mainContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(paddingPx, paddingPx, paddingPx, paddingPx)
            layoutParams = android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                android.widget.FrameLayout.LayoutParams.WRAP_CONTENT
            )
        }

        // 🎯 Header-Leiste mit Titel und Lock-Button oben rechts
        val headerBar = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, (8 * density).toInt())
        }

        val tvHeaderTitle = TextView(context).apply {
            text = "Sequence Control"
            textSize = 18f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(Color.BLACK)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        // Taschensperre-Overlaydeklaration
        lateinit var lockOverlay: LinearLayout

        val btnLock = TextView(context).apply {
            text = "🔒 Lock"
            textSize = 14f
            setTextColor(Color.WHITE)
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding((10 * density).toInt(), (6 * density).toInt(), (10 * density).toInt(), (6 * density).toInt())
            background = createRoundedDrawable("#555555", 12f)
            setOnClickListener {
                lockOverlay.visibility = android.view.View.VISIBLE
            }
        }

        headerBar.addView(tvHeaderTitle)
        headerBar.addView(btnLock)
        mainContainer.addView(headerBar)

        // 🎯 Scrollbarer Bereich für die Timeline-Schritte
        val scrollView = ScrollView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (350 * density).toInt() // Feste Höhe für angenehmes Scrollen
            )
        }

        val timelineContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        scrollView.addView(timelineContainer)
        mainContainer.addView(scrollView)

        // 🎯 Untere Steuerleiste mit 5 Tasten (|<<, <, Play/Pause, Stop, >)
        val controlBar = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, (12 * density).toInt(), 0, 0)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val btnRestart = createIconButton(R.drawable.not_started) { executor.restartProgram() }.apply {
            scaleX = -1f // 🎯 Spiegelt not_started um die Y-Achse (aus |> wird <|)
        }
        val btnPrev = createIconButton(R.drawable.arrow_circle_left) { executor.stepBackward() }
        val btnPlayPause = createIconButton(R.drawable.pause_circle) { executor.togglePause() }
        val btnStop = createIconButton(R.drawable.stop_circle) {
            executor.stopProgram()
            dismiss()
        }
        val btnNext = createIconButton(R.drawable.arrow_circle_right) { executor.stepForward() }

        controlBar.addView(btnRestart)
        controlBar.addView(btnPrev)
        controlBar.addView(btnPlayPause)
        controlBar.addView(btnNext)
        controlBar.addView(btnStop)

        mainContainer.addView(controlBar)
        rootLayout.addView(mainContainer)

        // 🎯 Taschensperre Overlay (Sperrt alle Touch-Eingaben)
        lockOverlay = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#EE000000")) // Dunkles, fast undurchsichtiges Overlay
            visibility = android.view.View.GONE
            isClickable = true
            isFocusable = true
            layoutParams = android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT
            )
        }

        val tvLockIcon = TextView(context).apply {
            text = "🔒"
            textSize = 48f
            gravity = Gravity.CENTER
        }

        val tvLockInstruction = TextView(context).apply {
            text = "Screen Locked\nProgram is executed in the background\n\nPress and hold 2s to unlock"
            textSize = 18f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(0, (12 * density).toInt(), 0, 0)
        }

        val btnUnlockArea = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding((24 * density).toInt(), (16 * density).toInt(), (24 * density).toInt(), (16 * density).toInt())
            background = createRoundedDrawable("#333333", 16f)
            addView(tvLockIcon)
            addView(tvLockInstruction)
        }

        // 🎯 2-Sekunden Long-Press Entsperrlogik
        var unlockJob: Job? = null
        btnUnlockArea.setOnTouchListener { _, event ->
            when (event.action) {
                android.view.MotionEvent.ACTION_DOWN -> {
                    tvLockInstruction.text = "Hold down..."
                    unlockJob = scope.launch(Dispatchers.Main) {
                        delay(2000) // 2 Sekunden Gedrückthalten abwarten
                        lockOverlay.visibility = android.view.View.GONE
                        tvLockInstruction.text = "Screen Locked\nPress and hold 2s to unlock"
                        com.launcher_control_android.helper.util.ToastUtil.showToastMessage(context, "Unlocked")
                    }
                    true
                }
                android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL -> {
                    unlockJob?.cancel()
                    unlockJob = null
                    tvLockInstruction.text = "Screen Locked\nPress and hold 2s to unlock"
                    true
                }
                else -> false
            }
        }

        lockOverlay.addView(btnUnlockArea)
        rootLayout.addView(lockOverlay)

        builder.setView(rootLayout)

        alertDialog = builder.create().apply {
            setCanceledOnTouchOutside(false)
            window?.setBackgroundDrawable(createRoundedDrawable("#FFFFFF", 16f))
            show()
        }

        // 🎯 Beobachten des Executor-Status für Live-Updates
        updateJob = scope.launch(Dispatchers.Main) {
            executor.uiState.collectLatest { state ->
                val showPlaySymbol = state.isPaused || state.isSequenceEnd || !state.isRunning
                val playPauseIcon = if (showPlaySymbol) R.drawable.play_circle else R.drawable.pause_circle
                btnPlayPause.setImageResource(playPauseIcon)
                updateTimelineList(timelineContainer, state.remainingDelaySeconds)

                // 🎯 Automatisches Zentrieren/Mitscrollen zum aktiven Element
                val targetIndex = if (state.isSequenceEnd) {
                    timelineContainer.childCount - 1 // FINISH-Zeile ganz unten
                } else {
                    state.currentItem + 1 // +1 wegen der START-Zeile an Position 0
                }
                if (targetIndex in 0 until timelineContainer.childCount) {
                    val targetView = timelineContainer.getChildAt(targetIndex)
                    scrollView.post {
                        val y = targetView.top - (scrollView.height / 2) + (targetView.height / 2)
                        scrollView.smoothScrollTo(0, maxOf(0, y))
                    }
                }
            }
        }
    }

    private fun createIconButton(iconResId: Int, onClick: () -> Unit): ImageView {
        val density = context.resources.displayMetrics.density
        val btnMargin = (4 * density).toInt()
        val paddingPx = (4 * density).toInt() // 🎯 Innenabstand verringert für deutlich größere Icons
        val params = LinearLayout.LayoutParams(0, (52 * density).toInt(), 1f).apply {
            setMargins(btnMargin, 0, btnMargin, 0)
        }
        return ImageView(context).apply {
            setImageResource(iconResId)
            imageTintList = android.content.res.ColorStateList.valueOf(Color.WHITE)
            background = createRoundedDrawable("#333333", 8f)
            setPadding(paddingPx, paddingPx, paddingPx, paddingPx)
            layoutParams = params
            setOnClickListener { onClick() }
        }
    }

    private fun updateTimelineList(container: LinearLayout, remainingSec: Int) {
        container.removeAllViews()
        val items = executor.getTimelineItems()
        val density = context.resources.displayMetrics.density

        items.forEach { item ->
            when (item.rowType) {
                TimelineRowType.START -> {
                    // 🎯 START ganz oben über die volle Breite (ab X=0)
                    val tvStart = TextView(context).apply {
                        textSize = 20f
                        isSingleLine = true
                        text = item.displayText
                        setTypeface(null, android.graphics.Typeface.BOLD)
                        setTextColor(
                            if (item.isActive) androidx.core.content.ContextCompat.getColor(context, R.color.colorGreen)
                            else Color.BLACK
                        )
                        setPadding(0, (4 * density).toInt(), 0, (4 * density).toInt())
                    }
                    container.addView(tvStart)
                }

                TimelineRowType.ACTION -> {
                    val rowContainer = LinearLayout(context).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = Gravity.TOP
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        )
                    }

                    // 🎯 Linke Spalte (32dp): Linie OBERHALB des Icons + Icon darunter
                    val leftCol = LinearLayout(context).apply {
                        orientation = LinearLayout.VERTICAL
                        gravity = Gravity.CENTER_HORIZONTAL
                        layoutParams = LinearLayout.LayoutParams(
                            (32 * density).toInt(),
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply {
                            setMargins(0, 0, (4 * density).toInt(), 0)
                        }
                    }

                    // 1. Linie OBERHALB des Icons (führt vom vorherigen Schritt zu diesem Schritt)
                    val progress = if (item.isCompleted) 1.0f else if (item.isActive) executor.uiState.value.lineProgress else 0.0f
                    val maxLineHeightPx = (20 * density).toInt()
                    val currentLineHeightPx = (maxLineHeightPx * progress).toInt()

                    val lineContainer = LinearLayout(context).apply {
                        orientation = LinearLayout.VERTICAL
                        gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                        translationX = -(4 * density) // 🎯 Verschiebt die Linie exakt 4dp nach links ins Kreiszentrum
                        layoutParams = LinearLayout.LayoutParams((2 * density).toInt(), maxLineHeightPx).apply {
                            setMargins(0, 0, 0, (2 * density).toInt())
                        }
                    }

                    if (currentLineHeightPx > 0) {
                        val blackLine = android.view.View(context).apply {
                            setBackgroundColor(Color.BLACK)
                            layoutParams = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                currentLineHeightPx
                            )
                        }
                        lineContainer.addView(blackLine)
                    }
                    leftCol.addView(lineContainer)

                    // 2. Icon (Circle)
                    val ivIcon = ImageView(context).apply {
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        )
                        val actionIcon = when {
                            item.isStrikethrough -> R.drawable.ic_step_completed
                            item.isActive && item.isLaunch -> R.drawable.ic_step_active_red
                            item.isActive -> R.drawable.ic_step_active_green
                            else -> R.drawable.ic_step_pending
                        }
                        setImageResource(actionIcon)
                    }
                    leftCol.addView(ivIcon)

                    // 🎯 Rechte Spalte: Text für die Aktion
                    val tvStep = TextView(context).apply {
                        textSize = 20f
                        isSingleLine = true
                        ellipsize = android.text.TextUtils.TruncateAt.END
                        setPadding(0, (20 * density).toInt(), 0, 0)
                        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)

                        val baseText = "#${item.sequenceIndex}: ${item.displayText}"
                        text = if (item.isActive && remainingSec > 0) {
                            "$baseText (${remainingSec}s)"
                        } else {
                            baseText
                        }

                        if (item.isStrikethrough) {
                            setTextColor(Color.GRAY)
                            paintFlags = paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                        } else if (item.isActive) {
                            val activeColor = if (item.isLaunch) Color.RED else androidx.core.content.ContextCompat.getColor(context, R.color.colorGreen)
                            setTextColor(activeColor)
                            setTypeface(null, android.graphics.Typeface.BOLD)
                            paintFlags = paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                        } else {
                            setTextColor(Color.BLACK)
                            paintFlags = paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                        }
                    }

                    rowContainer.addView(leftCol)
                    rowContainer.addView(tvStep)
                    // 🎯 Tap-Funktion: Beim Klick auf die Zeile direkt zu diesem Schritt springen (nur bei Pause)
                    val actionSequenceIndex = item.sequenceIndex - 1 // 0-basierter Index für currentSequence
                    rowContainer.setOnClickListener {
                        if (executor.uiState.value.isPaused) {
                            executor.jumpToStep(actionSequenceIndex)
                        }
                    }

                    container.addView(rowContainer)
                }

                TimelineRowType.FINISH -> {
                    // 🎯 FINISH-Bereich: Verbindungslinie vom letzten Schritt runter zu FINISH
                    val isSequenceEnd = executor.uiState.value.isSequenceEnd
                    val progress = if (isSequenceEnd) 1.0f else 0.0f
                    val maxLineHeightPx = (20 * density).toInt()
                    val currentLineHeightPx = (maxLineHeightPx * progress).toInt()

                    val finishLineContainer = LinearLayout(context).apply {
                        orientation = LinearLayout.VERTICAL
                        gravity = Gravity.TOP or Gravity.START
                        setPadding((11 * density).toInt(), 0, 0, 0)
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            maxLineHeightPx
                        )
                    }

                    if (currentLineHeightPx > 0) {
                        val blackLine = android.view.View(context).apply {
                            setBackgroundColor(Color.BLACK)
                            layoutParams = LinearLayout.LayoutParams(
                                (2 * density).toInt(),
                                currentLineHeightPx
                            )
                        }
                        finishLineContainer.addView(blackLine)
                    }
                    container.addView(finishLineContainer)

                    // 🎯 FINISH ganz unten über die volle Breite (ab X=0)
                    val tvFinish = TextView(context).apply {
                        textSize = 20f
                        isSingleLine = true
                        text = item.displayText
                        setTypeface(null, android.graphics.Typeface.BOLD)
                        setTextColor(
                            if (item.isActive) androidx.core.content.ContextCompat.getColor(context, R.color.colorGreen)
                            else Color.BLACK
                        )
                        setPadding(0, (4 * density).toInt(), 0, (4 * density).toInt())
                    }
                    container.addView(tvFinish)
                }
            }
        }
    }

    fun dismiss() {
        updateJob?.cancel()
        updateJob = null
        alertDialog?.dismiss()
        alertDialog = null
    }
}