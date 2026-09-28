package com.launcher_control_android.data.model.response

import java.io.Serializable

data class UnitModel(
    var unitNumber: Int,
    var noOfChannel: Int,
    var isSoundOptionInstalled: Boolean,
    var isServoVersion: Boolean,
    var selectedSound: Int,
    var selectedPressure: Int,
    var volumeStep: Int = 4 // 1 = 15% (Leise), 2 = 30% (Mittel), 3 = 50% (Laut), 4 = 100% (Voll)
): Serializable {
    fun testHexCode(): String? {
        return when (unitNumber) {
            1 -> "1F"
            2 -> "2F"
            3 -> "3F"
            4 -> "4F"
            else -> null
        }
    }

    fun reloadHexCode(): String? {
        return when (unitNumber) {
            1 -> "10"
            2 -> "20"
            3 -> "30"
            4 -> "40"
            else -> null
        }
    }

    fun fetchDataHexCode(): String? {
        return when(unitNumber) {
            1 -> "1E"
            2 -> "2E"
            3 -> "3E"
            4 -> "4E"
            else -> null
        }
    }

    fun unitChannelHexCode(unit: String?, channel: Int?): String? {
        val channelHex = when(channel) {
            1 -> "1"
            2 -> "2"
            3 -> "3"
            4 -> "4"
            5 -> "5"
            6 -> "6"
            7 -> "7"
            8 -> "8"
            9 -> "9"
            10 -> "a"
            11 -> "b"
            12 -> "c"
            else -> null
        } ?: return null
        return "$unit$channelHex"
    }

    fun soundHexCode(soundIndex: Int? = null, volumeStep: Int = 4): String? {
        val sound = (soundIndex ?: selectedSound) + 1
        val validSound = sound.coerceIn(1, 6)

        return when (volumeStep) {
            1 -> when (unitNumber) { // 🎯 15% (Leise)
                1 -> validSound.toString(16)
                2 -> (9 + validSound).toString(16)
                3 -> "7" + validSound.toString(16)
                4 -> "7" + (9 + validSound).toString(16)
                else -> null
            }
            2 -> when (unitNumber) { // 🎯 30% (Mittel)
                1 -> "5" + validSound.toString(16)
                2 -> "5" + (9 + validSound).toString(16)
                3 -> "8" + validSound.toString(16)
                4 -> "8" + (9 + validSound).toString(16)
                else -> null
            }
            3 -> when (unitNumber) { // 🎯 50% (Laut)
                1 -> "6" + validSound.toString(16)
                2 -> "6" + (9 + validSound).toString(16)
                3 -> "9" + validSound.toString(16)
                4 -> "9" + (9 + validSound).toString(16)
                else -> null
            }
            else -> when (unitNumber) { // 🎯 100% (Original)
                1 -> "A$validSound"
                2 -> "B$validSound"
                3 -> "C$validSound"
                4 -> "D$validSound"
                else -> null
            }
        }
    }

    fun pressureHaxCode(pressureIndex: Int?): String? {
        val lastCode = when(pressureIndex) {
            0 -> "A"
            1 -> "B"
            2 -> "C"
            3 -> "D"
            4 -> "E"
            else -> "F"
        }
        return when(unitNumber) {
            1 -> "A$lastCode"
            2 -> "B$lastCode"
            3 -> "C$lastCode"
            4 -> "D$lastCode"
            else -> null
        }
    }

    fun isReloadCommand(command: String?) : Boolean {
        return command == reloadHexCode()
    }

    fun isPressureCommand(command: String?): Boolean {
        return command?.getOrNull(0) in 'A'..'D' && command?.getOrNull(1) in 'A'..'F'
    }



    fun isVisible(): Boolean {
        return noOfChannel > 0 || isSoundOptionInstalled
    }

    fun isOnlySoundInstalled(): Boolean {
        return noOfChannel == 0 && isSoundOptionInstalled
    }

    fun isChannelAdded(): Boolean {
        return noOfChannel > 0
    }

    fun isSoundCommand(command: String?): Boolean {
        return com.launcher_control_android.data.model.response.isSoundCommand(command)
    }
}

fun isSoundCommand(command: String?): Boolean {
    if (command.isNullOrEmpty()) return false
    val cmd = command.lowercase()
    return when {
        // Unit 1 Sounds (Standard & Lautstärkestufen 15%, 30%, 50%, 100%)
        cmd in listOf("1", "2", "3", "4", "5", "6") -> true
        cmd in listOf("51", "52", "53", "54", "55", "56") -> true
        cmd in listOf("61", "62", "63", "64", "65", "66") -> true
        cmd.length == 2 && cmd[0] == 'a' && cmd[1] in '1'..'6' -> true

        // Unit 2 Sounds
        cmd in listOf("a", "b", "c", "d", "e", "f") -> true
        cmd in listOf("5a", "5b", "5c", "5d", "5e", "5f") -> true
        cmd in listOf("6a", "6b", "6c", "6d", "6e", "6f") -> true
        cmd.length == 2 && cmd[0] == 'b' && cmd[1] in '1'..'6' -> true

        // Unit 3 Sounds
        cmd in listOf("71", "72", "73", "74", "75", "76") -> true
        cmd in listOf("81", "82", "83", "84", "85", "86") -> true
        cmd in listOf("91", "92", "93", "94", "95", "96") -> true
        cmd.length == 2 && cmd[0] == 'c' && cmd[1] in '1'..'6' -> true

        // Unit 4 Sounds
        cmd in listOf("7a", "7b", "7c", "7d", "7e", "7f") -> true
        cmd in listOf("8a", "8b", "8c", "8d", "8e", "8f") -> true
        cmd in listOf("9a", "9b", "9c", "9d", "9e", "9f") -> true
        cmd.length == 2 && cmd[0] == 'd' && cmd[1] in '1'..'6' -> true

        else -> false
    }
}
