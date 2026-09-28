package com.launcher_control_android.main.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.LayoutRes
import androidx.databinding.DataBindingUtil
import androidx.databinding.ObservableField
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.launcher_control_android.AppConstants
import com.launcher_control_android.BR
import com.launcher_control_android.helper.util.PrefUtil
import com.launcher_control_android.helper.util.ToastUtil
import com.launcher_control_android.main.common.ApiRenderState
import kotlinx.coroutines.launch
import javax.inject.Inject

abstract class BaseBsd<binding : ViewDataBinding, VM : BaseVM>(@LayoutRes private val layoutId: Int) : BottomSheetDialogFragment(), View.OnClickListener {



    protected lateinit var binding: binding

    private var progress: ObservableField<Boolean>? = null

    protected abstract fun renderState(apiRenderState: ApiRenderState)

    protected abstract val hasProgress: Boolean
    protected abstract val vm: VM?
    protected abstract fun init()

    @Inject
    lateinit var prefs: PrefUtil

    //    override fun getTheme(): Int = Styles.BsdTheme

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate<binding>(inflater, layoutId, container, false).apply {
            lifecycleOwner = this@BaseBsd
            vm?.let {
                setVariable(BR.vm, it)

                viewLifecycleOwner.lifecycleScope.launch {
                    it.state().collect {
                        renderState(it)
                    }
                }

                lifecycleScope.launch {
                    it.apiError.collect {
                        if (it.resCode == AppConstants.Api.ResponseCode.UNAUTHORIZED_CODE)
                            (requireActivity() as BaseAct<ViewDataBinding, BaseVM>).logout(true)
                        else
                            if (it.resultType != BaseRepo.ApiResultType.CANCELLED) {
                                hideProgress()
                                it.error?.let {
                                    errorToast(it)
                                }
                            }
                    }
                }
            }
            setVariable(BR.click, this@BaseBsd)
        }

        if (hasProgress) {
            progress = ObservableField()
            binding.setVariable(BR.showProgress, progress)
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        init()
    }

    fun showProgress() {
        progress?.set(true)
    }

    fun hideProgress() {
        progress?.set(false)
    }

    fun errorToast(message: String, duration: Int = Toast.LENGTH_SHORT) {
        ToastUtil.showToastMessage(requireContext(), message, duration)
    }

    fun successToast(message: String, duration: Int = Toast.LENGTH_SHORT) {
        ToastUtil.showToastMessage(requireContext(), message, duration)
    }

    override fun onClick(v: View) {

    }

}
