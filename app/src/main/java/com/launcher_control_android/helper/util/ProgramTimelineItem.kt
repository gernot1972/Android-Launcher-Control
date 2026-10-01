package com.launcher_control_android.helper.util

enum class TimelineRowType {
    START,
    ACTION,
    FINISH
}

data class ProgramTimelineItem(
    val id: String,
    val sequenceIndex: Int,          // 0 = START, 1..N = Aktionen, N+1 = FINISH
    val rowType: TimelineRowType,
    val isLaunch: Boolean = false,   // true = Launch (Rot), false = Sound (Grün)
    val displayText: String,
    var isActive: Boolean = false,
    var isCompleted: Boolean = false,
    var isStrikethrough: Boolean = false, // 🎯 Erst nach 2 Sek. Verzögerung true!
    var lineProgress: Float = 0.0f   // 0.0f bis 1.0f für den Füllstand der Verbindungslinie
)