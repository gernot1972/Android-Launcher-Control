package com.launcher_control_android.main.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.databinding.ObservableField
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.launcher_control_android.AppConstants.Api.ResponseCode.UNAUTHORIZED_CODE
import com.launcher_control_android.BR
import com.launcher_control_android.Styles
import com.launcher_control_android.helper.util.PrefUtil
import com.launcher_control_android.helper.util.ToastUtil
import com.launcher_control_android.main.base.BaseRepo.ApiResultType.CANCELLED
import com.launcher_control_android.main.common.ApiRenderState
import kotlinx.coroutines.launch
import javax.inject.Inject

abstract class BaseDialFrag<binding : ViewDataBinding, VM : BaseVM> : DialogFragment(),
    View.OnClickListener {

    @Inject
    lateinit var prefs: PrefUtil

    protected lateinit var binding: binding

    private var progress: ObservableField<Boolean>? = null

    protected abstract val layoutId: Int
    protected abstract val vm: VM?
    protected abstract fun init()
    protected abstract fun renderState(apiRenderState: ApiRenderState)
    protected abstract val hasProgress: Boolean

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, Styles.AppTheme)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate<binding>(inflater, layoutId, container, false).apply {
            lifecycleOwner = this@BaseDialFrag
            vm?.let {
                setVariable(BR.vm, it)

                lifecycleScope.launch {
                    it.state().collect {
                        renderState(it)
                    }
                }

                lifecycleScope.launch {
                    it.apiError.collect {
                        if (it.resCode == UNAUTHORIZED_CODE)
                            (requireActivity() as BaseAct<ViewDataBinding, BaseVM>).logout(true)
                        else
                            if (it.resultType != CANCELLED) {
                                hideProgress()
                                it.error?.let {
                                    errorToast(it)
                                }
                            }
                    }
                }
            }
            setVariable(BR.click, this@BaseDialFrag)
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

    fun errorToast(message: String, duration: Int = Toast.LENGTH_SHORT) {
        ToastUtil.showToastMessage(requireContext(), message, duration)
    }

    fun successToast(message: String, duration: Int = Toast.LENGTH_SHORT) {
        ToastUtil.showToastMessage(requireContext(), message, duration)
    }

    fun showProgress() {
        progress?.set(true)
    }

    fun hideProgress() {
        progress?.set(false)
    }

    override fun onClick(v: View) {

    }
}
