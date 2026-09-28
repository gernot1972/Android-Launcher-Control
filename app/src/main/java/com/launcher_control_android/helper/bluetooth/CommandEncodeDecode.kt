package com.launcher_control_android.helper.bluetooth

class CommandEncodeDecode {
}

fun String.hexDecodedData(): ByteArray {
    if (!this.isHexadecimal()) {
        throw ConversionError.IncorrectInputFormat()
    }
    val data = ByteArray((this.length + 1) / 2) // 🎯 Korrektur: Erlaubt auch 1-stellige Hex-Befehle ("1".."6", "a".."f")
    val regex = Regex("[0-9a-fA-F]{1,2}")
    var dataIndex = 0

    regex.findAll(this).forEach { matchResult ->
        val byteString = matchResult.value
        val num = byteString.toInt(16).toByte()
        data[dataIndex] = num
        dataIndex++
    }

    return data
}

fun ByteArray.toHexEncodedString(): String {
    val hexString = StringBuilder()

    this.forEach { byte ->
        val hex = (byte.toInt() and 0xFF).toString(16)
        if (hex.length == 1) {
            hexString.append('0')
        }
        hexString.append(hex)
    }

    return hexString.toString()
}

fun String.isHexadecimal(): Boolean {
    val hexRegex = Regex("^[0-9a-fA-F]+$")
    return hexRegex.matches(this)
}

sealed class ConversionError : Exception() {
    class IncorrectInputFormat : ConversionError()
}