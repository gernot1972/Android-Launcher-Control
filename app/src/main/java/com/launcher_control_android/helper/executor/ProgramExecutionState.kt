package com.launcher_control_android.helper.executor

data class ProgramExecutionState(
    val isRunning: Boolean = false,
    val isPaused: Boolean = false,
    val currentItem: Int = 0,
    val remainingDelaySeconds: Int = 0,
    val lineProgress: Float = 0f,
    val isExecutingAction: Boolean = false,
    val isSequenceEnd: Boolean = false,
    val needToShowReload: Boolean = false
)