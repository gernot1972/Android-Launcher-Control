package com.launcher_control_android.main.ui.home

import com.launcher_control_android.Colors
import com.launcher_control_android.Drawables
import com.launcher_control_android.data.model.response.UnitModel
import com.launcher_control_android.main.common.FetchedChannelModel

data class LauncherControlUIStateModel(
    val isGatewaySetup: Boolean,
    val isUnitSetup: Boolean,
    val gatewayConnectionStatus: GatewayConnectionStatus,
    val selectedUnit: UnitModel? = null,
    val fetchedUnitModel: FetchedChannelModel? = null,
    val isDisarmed: Boolean = false
) {
    fun hasGatewayAndUnitSet(): Boolean {
        return isGatewaySetup && isUnitSetup
    }

    fun launcherImageResId(): Int {
        return if (getNextAvailableChannel() != null && !isDisarmed) Drawables.ic_red_freesbi else Drawables.ic_green_freesbi
    }

    fun isGatewayNotConnected(): Boolean = gatewayConnectionStatus == GatewayConnectionStatus.NOT_CONNECTED
    fun isGatewayConnecting(): Boolean = gatewayConnectionStatus == GatewayConnectionStatus.CONNECTING
    fun isGatewayConnected(): Boolean = gatewayConnectionStatus == GatewayConnectionStatus.CONNECTED

    fun hasUnitSelected(): Boolean = selectedUnit != null

    fun hasUnitDataFetched(): Boolean = fetchedUnitModel != null

    fun isThisUnitSelected(unitNumber: Int): Boolean {
        return selectedUnit?.unitNumber == unitNumber
    }

    @JvmOverloads
    fun isUnitServoON(fetchedModel: FetchedChannelModel? = fetchedUnitModel): Boolean {
        return selectedUnit?.isServoVersion == true || fetchedModel?.getBar() == 13
    }

    fun isSoundOnlyMode(): Boolean {
        return selectedUnit?.isOnlySoundInstalled() == true
    }

    fun isCompressorActive(fetchedModel: FetchedChannelModel? = fetchedUnitModel): Boolean {
        return fetchedModel?.isCompressorActive() == true
    }

    fun isCompressorLocked(fetchedModel: FetchedChannelModel? = fetchedUnitModel): Boolean {
        val bar = fetchedModel?.getBar() ?: return false
        val targetBar = fetchedModel.getPressure() ?: ((selectedUnit?.selectedPressure ?: 0) * 2)
        if (bar >= 13) return false

        return if (fetchedModel.hasCompressorTelemetry()) {
            // 🎯 Neue Geräte (4-stelliges Suffix PTYZ): Sperre wenn Solldruck > Istdruck UND Kompressor steht
            targetBar > bar && !isCompressorActive(fetchedModel)
        } else {
            // 🎯 Alte Geräte (2-stelliges Suffix PT): Sperre erlischt automatisch bei Istdruck > 1 bar
            targetBar > bar && bar <= 1
        }
    }

    fun isTargetPressureReached(fetchedModel: FetchedChannelModel? = fetchedUnitModel): Boolean {
        val bar = fetchedModel?.getBar() ?: return false
        val targetBar = fetchedModel.getPressure() ?: ((selectedUnit?.selectedPressure ?: 0) * 2)
        return bar < 13 && targetBar > 0 && bar >= targetBar
    }

    fun leftStatusIconResId(fetchedModel: FetchedChannelModel? = fetchedUnitModel): Int {
        return when {
            isUnitServoON(fetchedModel) -> Drawables.remember_me_24
            isSoundOnlyMode() -> Drawables.ic_volume
            else -> Drawables.ic_fan
        }
    }

    fun leftStatusIconColor(fetchedModel: FetchedChannelModel? = fetchedUnitModel): Int {
        return when {
            fetchedModel?.isBarFail() == true -> Colors.colorRed
            isUnitServoON(fetchedModel) || isSoundOnlyMode() -> Colors.colorGreen
            isCompressorLocked(fetchedModel) -> Colors.colorRed
            isTargetPressureReached(fetchedModel) -> Colors.colorGreen
            isCompressorActive(fetchedModel) -> Colors.colorOrange
            else -> Colors.white
        }
    }

    fun leftStatusText(fetchedModel: FetchedChannelModel? = fetchedUnitModel): String {
        return when {
            fetchedModel?.isBarFail() == true -> "FAIL"
            isUnitServoON(fetchedModel) -> "SERVO OK"
            isSoundOnlyMode() -> "SOUND OK"
            isCompressorLocked(fetchedModel) -> "LOCK"
            isCompressorActive(fetchedModel) -> "ON"
            else -> "OFF"
        }
    }

    fun canShowFireChannel(): Boolean {
        return selectedUnit?.isChannelAdded() == true && fetchedUnitModel != null && !isDisarmed
    }

    fun isNoResponse(): Boolean {
        return fetchedUnitModel != null && fetchedUnitModel.isNoResponse()
    }

    fun isAllChannelsFired(): Boolean {
        return fetchedUnitModel != null && !fetchedUnitModel.isNoResponse() && getNextAvailableChannel() == null
    }

    fun getNextAvailableChannel(): Int? {
        val nextAvailableChannel = fetchedUnitModel?.getNextChannel()
        val noOfChannelAdded = selectedUnit?.noOfChannel ?: 0
        return if (nextAvailableChannel != null && nextAvailableChannel <= noOfChannelAdded) {
            nextAvailableChannel
        } else {
            null
        }
    }

    fun getUnitBackground(unitNumber: Int): Int {
        return if (fetchedUnitModel?.getUnit()?.toIntOrNull() == unitNumber && getNextAvailableChannel() != null && !isDisarmed) {
            Drawables.shape_rect_rounded_10_red
        } else if (selectedUnit?.unitNumber == unitNumber) {
            Drawables.shape_rect_rounded_10
        } else if (selectedUnit != null) {
            Drawables.shape_rect_rounded_border_10
        } else {
            Drawables.selector_unit_round_bg
        }
    }

    fun getUnitTextColor(unitNumber: Int): Int {
        return if (selectedUnit?.unitNumber == unitNumber) {
            Colors.black
        } else {
            Colors.white
        }
    }
}
