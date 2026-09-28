package com.launcher_control_android.main.ui.unit_detail.view

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

class SoundOptionsBsd: BaseBsd<BsdSoundOptionsBinding, BaseVM>(Layouts.bsd_sound_options) {

    companion object {
        private var onSoundSelection: ((soundIndex: Int) -> Unit)? = null
        fun newInstance(selectedSoundIndex: Int, onSoundSelection: ((soundIndex: Int) -> Unit)? = null): SoundOptionsBsd {
            this.onSoundSelection = onSoundSelection
            val args = Bundle()
            args.putInt(AppConstants.Communication.BundleData.INTENT_POSITION, selectedSoundIndex)
            val fragment = SoundOptionsBsd()
            fragment.arguments = args
            return fragment
        }
    }

    private lateinit var rvUtil: RvUtil
    private var selectedSoundIndex: Int = -1

    override val hasProgress: Boolean = false

    override val vm: BaseVM? = null

    override fun init() {
        checkArguments()
        setRecyclerView()
    }

    private fun checkArguments() {
        selectedSoundIndex = arguments?.getInt(AppConstants.Communication.BundleData.INTENT_POSITION, -1) ?: -1
    }

    private fun setRecyclerView() {
        rvUtil = RvUtil(
            rv = binding.rvSoundOption,
            adapter = BaseRvBindingAdapter(
                layoutId = Layouts.item_sound_title,
                list = AppConstants.App.listOfSound,
                br = BR.item,
                otherBr = { item, pos ->
                    listOf(BR.isSelected to (selectedSoundIndex == pos))
                },
                clickListener = {view, item, pos ->
                    onSoundSelection?.invoke(pos)
                    dismiss()
                }
            )
        )
    }

    override fun renderState(apiRenderState: ApiRenderState) {

    }
}