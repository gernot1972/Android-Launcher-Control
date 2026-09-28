package com.launcher_control_android.helper.util

import android.content.Context
import android.view.Gravity
import android.widget.Toast

object ToastUtil {

    /*top snackbar
    fun errorSnackbar(message: String, view: View?, callback: ((Boolean) -> Unit)?) {
        TopSnackbar.make(view!!, message, Snackbar.LENGTH_LONG).let {
            it.view.setBackgroundColor(Color.parseColor("#dd5a5a"))
            it.setCallback(object : TopSnackbar.Callback() {
                override fun onDismissed(snackbar: TopSnackbar, event: Int) {
                    callback?.invoke(true)
                }
            })
            it.show()
        }
    }

    fun successSnackbar(message: String, view: View?, callback: ((Boolean) -> Unit)?) {
        TopSnackbar.make(view!!, message, Snackbar.LENGTH_LONG).let {
            it.view.setBackgroundColor(Color.parseColor("#87cc6c"))
            it.setCallback(object : TopSnackbar.Callback() {
                override fun onDismissed(snackbar: TopSnackbar, event: Int) {
                    callback?.invoke(true)
                }
            })
            it.show()
        }
    }*/

    /*fun errorSnackbar(message: String, view: View?, callback: ((Boolean) -> Unit)?) {

        Snackbar.make(view!!, message, Snackbar.LENGTH_SHORT).let {
            it.view.setBackgroundColor(Color.parseColor("#dd5a5a"))
            it.addCallback(object : Snackbar.Callback() {
                override fun onShown(sb: Snackbar?) {

                }

                override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                    callback?.invoke(true)
                }
            })
//        (snackbar.view.findViewById(com.google.android.material.R.id.snackbar_text) as TextView).setTextColor(Color.WHITE)
            it.show()
        }
    }

    fun successSnackbar(message: String, view: View?, callback: ((Boolean) -> Unit)?) {

        Snackbar.make(view!!, message, Snackbar.LENGTH_SHORT).let {
            it.view.setBackgroundColor(Color.parseColor("#87cc6c"))
            it.addCallback(object : Snackbar.Callback() {
                override fun onShown(sb: Snackbar?) {

                }

                override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                    callback?.invoke(true)
                }
            })
//        (snackbar.view.findViewById(com.google.android.material.R.id.snackbar_text) as TextView).setTextColor(Color.WHITE)
            it.show()
        }
    }*/

    private var toast: Toast? = null
    fun showToastMessage(context: Context, message: String, duration: Int = Toast.LENGTH_SHORT) {
        toast?.cancel()
        toast = Toast.makeText(context, message, duration)
//        toast.setGravity(Gravity.CENTER, 0, 0)
        toast?.show()
    }
}


