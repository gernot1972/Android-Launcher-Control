package com.launcher_control_android.main.ui.home.view

import android.os.Bundle
import com.launcher_control_android.AppConstants
import com.launcher_control_android.BR
import com.launcher_control_android.Layouts
import com.launcher_control_android.databinding.BsdSoundOptionsBinding
import com.launcher_control_android.helper.rvutil.RvUtil
import com.launcher_control_android.main.base.BaseBsd
import com.launcher_control_android.main.base.BaseVM
import com.launcher_control_android.main.base.rv.BaseRvBindingAdapter
import com.launcher_control_android.main.common.ApiRenderState

class PressureOptionsBsd: BaseBsd<BsdSoundOptionsBinding, BaseVM>(Layouts.bsd_sound_options) {

    companion object {
        private var onPressureSelection: ((pressureIndex: Int) -> Unit)? = null
        fun newInstance(selectedPressureIndex: Int, onPressureSelection: ((pressureIndex: Int) -> Unit)? = null): PressureOptionsBsd {
            this.onPressureSelection = onPressureSelection
            val args = Bundle()
            args.putInt(AppConstants.Communication.BundleData.INTENT_POSITION, selectedPressureIndex)
            val fragment = PressureOptionsBsd()
            fragment.arguments = args
            return fragment
        }
    }

    private lateinit var rvUtil: RvUtil
    private var selectedPressureIndex: Int = -1

    override val hasProgress: Boolean = false

    override val vm: BaseVM? = null

    override fun init() {
        checkArguments()
        setRecyclerView()
    }

    private fun checkArguments() {
        selectedPressureIndex = arguments?.getInt(AppConstants.Communication.BundleData.INTENT_POSITION, -1) ?: -1
    }

    private fun setRecyclerView() {
        rvUtil = RvUtil(
            rv = binding.rvSoundOption,
            adapter = BaseRvBindingAdapter(
                layoutId = Layouts.item_sound_title,
                list = AppConstants.App.listOfPressure,
                br = BR.item,
                otherBr = { item, pos ->
                    listOf(BR.isSelected to (selectedPressureIndex == pos))
                },
                clickListener = {view, item, pos ->
                    onPressureSelection?.invoke(pos)
                    dismiss()
                }
            )
        )
    }

    override fun renderState(apiRenderState: ApiRenderState) {

    }
}