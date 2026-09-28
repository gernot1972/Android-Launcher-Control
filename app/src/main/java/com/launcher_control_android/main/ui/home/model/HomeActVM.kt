package com.launcher_control_android.main.ui.home.model

import com.launcher_control_android.helper.util.PrefUtil
import com.launcher_control_android.main.base.BaseVM
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class HomeActVM @Inject constructor(val prefs: PrefUtil) : BaseVM() {

    fun hasSetAnyChannel(): Boolean {
        return prefs.unit1Model.noOfChannel > 0 ||
                prefs.unit2Model.noOfChannel > 0 ||
                prefs.unit3Model.noOfChannel > 0 ||
                prefs.unit4Model.noOfChannel > 0
    }
}