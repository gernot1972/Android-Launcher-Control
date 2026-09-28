package com.launcher_control_android.helper.util

import android.content.Context
import android.content.DialogInterface
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.launcher_control_android.R

class DialogUtil

fun Context.showAlertDialog(
    title: String? = null,
    message: String? = null,
    isCancelable: Boolean = true,
    positiveBtnText: String? = null,
    positiveClickListener: (() -> Unit)? = null,
    negativeBtnText: String? = null,
    negativeClickListener: (() -> Unit)? = null,
): AlertDialog? {
    return MaterialAlertDialogBuilder(this/*, R.style.MaterialDialogStyle*/)
        .setTitle(title)
        .setMessage(message)
        .setCancelable(isCancelable)
        .setPositiveButton(positiveBtnText?.uppercase()) { dialog, which ->
            positiveClickListener?.invoke()
            dialog.dismiss()
        }
        .setNegativeButton(negativeBtnText?.uppercase()) { dialog, which ->
            negativeClickListener?.invoke()
            dialog.dismiss()
        }
        .show()
}