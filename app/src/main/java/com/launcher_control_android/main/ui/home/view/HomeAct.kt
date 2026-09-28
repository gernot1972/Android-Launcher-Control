package com.launcher_control_android.main.ui.home.view

import android.view.View
import androidx.activity.viewModels
import androidx.core.os.bundleOf
import com.launcher_control_android.AppConstants
import com.launcher_control_android.Layouts
import com.launcher_control_android.R
import com.launcher_control_android.databinding.ActHomeBinding
import com.launcher_control_android.helper.bluetooth.communication.BluetoothLeService
import com.launcher_control_android.helper.util.startActivity
import com.launcher_control_android.main.base.BaseAct
import com.launcher_control_android.main.common.ApiRenderState
import com.launcher_control_android.main.ui.channel_list.view.ChannelListAct
import com.launcher_control_android.main.ui.connection_config.view.ConnectionConfigAct
import com.launcher_control_android.main.ui.home.model.HomeActVM
import com.launcher_control_android.main.ui.main_configuration.view.MainConfigurationAct
import com.launcher_control_android.main.ui.unit_detail.view.UnitDetailAct
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HomeAct : BaseAct<ActHomeBinding, HomeActVM>(Layouts.act_home) {

    override val vm: HomeActVM by viewModels()

    override val hasProgress: Boolean = false

    override fun init() {
        setListener()
    }

    override fun renderState(apiRenderState: ApiRenderState) {

    }

    private fun setListener() {
        binding.btnUnit1.setOnClickListener {
            checkBleDeviceSaved {
                val bundle = bundleOf(AppConstants.Communication.BundleData.INTENT_UNIT_MODEL to prefs.unit1Model)
                startActivity(UnitDetailAct::class.java, bundle = bundle)
            }
        }

        binding.btnUnit2.setOnClickListener {
            checkBleDeviceSaved {
                val bundle =
                    bundleOf(AppConstants.Communication.BundleData.INTENT_UNIT_MODEL to prefs.unit2Model)
                startActivity(UnitDetailAct::class.java, bundle = bundle)
            }
        }

        binding.btnUnit3.setOnClickListener {
            checkBleDeviceSaved {
                val bundle =
                    bundleOf(AppConstants.Communication.BundleData.INTENT_UNIT_MODEL to prefs.unit3Model)
                startActivity(UnitDetailAct::class.java, bundle = bundle)
            }
        }

        binding.btnUnit4.setOnClickListener {
            checkBleDeviceSaved {
                val bundle =
                    bundleOf(AppConstants.Communication.BundleData.INTENT_UNIT_MODEL to prefs.unit4Model)
                startActivity(UnitDetailAct::class.java, bundle = bundle)
            }
        }

        binding.btnUnit1.setOnLongClickListener {
            checkBleDeviceSaved {
                val bundle =
                    bundleOf(AppConstants.Communication.BundleData.INTENT_UNIT_MODEL to prefs.unit1Model)
                startActivity(ChannelListAct::class.java, bundle = bundle)
            }
            false
        }

        binding.btnUnit2.setOnLongClickListener {
            checkBleDeviceSaved {
                val bundle =
                    bundleOf(AppConstants.Communication.BundleData.INTENT_UNIT_MODEL to prefs.unit2Model)
                startActivity(ChannelListAct::class.java, bundle = bundle)
            }
            false
        }

        binding.btnUnit3.setOnLongClickListener {
            checkBleDeviceSaved {
                val bundle =
                    bundleOf(AppConstants.Communication.BundleData.INTENT_UNIT_MODEL to prefs.unit3Model)
                startActivity(ChannelListAct::class.java, bundle = bundle)
            }
            false
        }

        binding.btnUnit4.setOnLongClickListener {
            checkBleDeviceSaved {
                val bundle =
                    bundleOf(AppConstants.Communication.BundleData.INTENT_UNIT_MODEL to prefs.unit4Model)
                startActivity(ChannelListAct::class.java, bundle = bundle)
            }
            false
        }
    }

    private fun checkBleDeviceSaved(callback: () -> Unit) {
        if (prefs.savedBluetoothDevice == null) {
            openConnectionConfigAct()
        } else callback.invoke()
    }

    private fun openConnectionConfigAct() {
        startActivity(ConnectionConfigAct::class.java)
    }

    override fun onClick(v: View) {
        super.onClick(v)
        when(v.id) {
            R.id.btn_available_devices -> {
                openConnectionConfigAct()
            }

            R.id.btn_setting -> {
                startActivity(MainConfigurationAct::class.java)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        binding.invalidateAll()
    }

    override fun onDestroy() {
        BluetoothLeService.getInstance(applicationContext).disconnectBluetoothDevice()
        super.onDestroy()
    }
}