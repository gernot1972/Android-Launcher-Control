package com.launcher_control_android.main.ui.unit_detail.model

import androidx.lifecycle.MutableLiveData
import com.launcher_control_android.data.model.response.UnitModel
import com.launcher_control_android.helper.util.PrefUtil
import com.launcher_control_android.main.base.BaseVM
import com.launcher_control_android.main.common.FetchedChannelModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class UnitDetailActVM @Inject constructor(private val prefs: PrefUtil) : BaseVM() {

    var selectedUnitModel = MutableLiveData<UnitModel>()
    var fetchedUnitModel: FetchedChannelModel? = null
    var fetchedUnitVoltage: String? = null
    val savedBluetoothDevice = MutableLiveData(prefs.savedBluetoothDevice)
    var currentFiredChannel: Int? = null

    fun getPrefUtil() = prefs

    fun resetFetchedUnit() {
        fetchedUnitModel = null
        fetchedUnitVoltage = null
    }
}