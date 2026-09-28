package com.launcher_control_android.main.common

class FetchedChannelModel(private val input: String) {

    private var channels: CharArray?
    private var unit: String?
    private var bar: Int?
    private var selectedPressure: Int? = null
    private var batteryHex: Int? = null
    private var compressorActive: Boolean = false

    init {
        val strArr = input.replace("U", "", ignoreCase = true).split("-")
        unit = strArr.firstOrNull()
        channels = strArr.getOrNull(1)?.toCharArray()
        val barStr = strArr.getOrNull(2)
        bar = barStr?.getOrNull(0)?.toString()?.toInt(16)
        selectedPressure = barStr?.getOrNull(1)?.toString()?.toInt(16)
        batteryHex = barStr?.getOrNull(2)?.toString()?.toInt(16)
        compressorActive = barStr?.getOrNull(3) == '1'
    }

    fun getUnit() = unit

    fun getBar() = bar

    fun getPressure() = selectedPressure

    fun getBatteryHex(): Int? = batteryHex

    fun hasBatterySensor(): Boolean = batteryHex != null

    fun hasCompressorTelemetry(): Boolean = (input.split("-").getOrNull(2)?.length ?: 0) >= 4

    fun isCompressorActive() = compressorActive

    fun getNextChannel(): Int? {
        val index = channels?.indexOfFirst { it == '0' } ?: -1
        return if (index != -1) index + 1 else null
    }

    fun getCurChannel(): Int? {
        val index = channels?.indexOfLast { it == '1' } ?: -1
        return if (index != -1) index + 1 else null
    }

    fun doReload() {
        channels = "000000000000".toCharArray()
    }

    fun markChannelAsFire(channelNo: Int) {
        if (channelNo >= 1) {
            channels?.set(channelNo - 1, '1')
        }
    }

    fun markChannelAsAvailable(channelNo: Int) {
        if (channelNo >= 1) {
            channels?.set(channelNo - 1, '0')
        }
    }

    fun isChannelAvailable(channelNo: Int): Boolean {
        return if (channelNo >= 1) {
            return channels?.getOrNull(channelNo - 1) == '0'
        } else false
    }

    fun isBarFail(): Boolean {
        return (bar ?: 0) >= 14
    }

    fun isNoResponse(): Boolean {
        return channels == null
    }
}