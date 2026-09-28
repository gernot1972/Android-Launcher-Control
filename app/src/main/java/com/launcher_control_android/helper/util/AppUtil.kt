package com.launcher_control_android.helper.util

import com.launcher_control_android.Drawables

fun getVoltageImageResId(voltage: Double): Int {
    // GEÄNDERT: Exakte iOS-Spannungsstufen für das Gateway (4.05V, 3.85V, 3.70V, 3.55V)
    val VOLTAGE_100 = 4.05
    val VOLTAGE_75 = 3.85
    val VOLTAGE_50 = 3.70
    val VOLTAGE_25 = 3.55

    val resId = when {
        voltage >= VOLTAGE_100 -> Drawables.battery100
        voltage >= VOLTAGE_75 -> Drawables.battery75
        voltage >= VOLTAGE_50 -> Drawables.battery50
        voltage >= VOLTAGE_25 -> Drawables.battery25
        else -> Drawables.battery0
    }
    return resId
}

fun getVoltagePercentage(voltage: Double): Int {
    // GEÄNDERT: Exakte iOS-Spannungsstufen für das Gateway (4.05V, 3.85V, 3.70V, 3.55V)
    val VOLTAGE_100 = 4.05
    val VOLTAGE_75 = 3.85
    val VOLTAGE_50 = 3.70
    val VOLTAGE_25 = 3.55

    val percentage = when {
        voltage >= VOLTAGE_100 -> 100
        voltage >= VOLTAGE_75 -> 75
        voltage >= VOLTAGE_50 -> 50
        voltage >= VOLTAGE_25 -> 25
        else -> 0
    }
    return percentage
}