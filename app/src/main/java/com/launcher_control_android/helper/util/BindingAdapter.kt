package com.launcher_control_android.helper.util

import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.text.Html
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.databinding.BindingAdapter
import com.launcher_control_android.Drawables


@BindingAdapter("android:visibility")
fun setVisibility(view: View, isVisible: Boolean?) {
    view.isVisible = isVisible ?: false
}

@BindingAdapter("htmlText")
fun loadHtml(textView: TextView, htmlText: String?) {
    if (htmlText.isNullOrBlank()) {
        return
    }
    textView.text = Html.fromHtml(htmlText, Html.FROM_HTML_MODE_LEGACY)
}

@BindingAdapter("isSelected")
fun setSelected(view: View, isSelected: Boolean = false) {
    view.isSelected = isSelected
}

@BindingAdapter(value = ["dimensionRatio"])
fun setDimensionRatio(view: View, dimension: String?) {
    if (view.parent is ConstraintLayout) {
        (view.layoutParams as ConstraintLayout.LayoutParams).apply {
            dimensionRatio = dimension ?: "1"
        }
    }
}

@BindingAdapter(value = ["clickable", "disabledUi"], requireAll = false)
fun setClickable(view: View, isClickable: Boolean? = true, disabledUi: Boolean? = null) {
    view.isClickable = isClickable ?: true
    if (disabledUi != false) {
        view.alpha = if (isClickable != false) 1F else 0.5F
    }
}

@BindingAdapter("signalImage")
fun setSignalImage(imageView: ImageView, signalStrength: Int?) {
    val resId = when(signalStrength) {
        in Int.MIN_VALUE..-100 -> Drawables.ic_signal_strength_0
        in -100..-85 -> Drawables.ic_signal_strength_1
        in -85..-70 -> Drawables.ic_signal_strength_2
        in -70..-55 -> Drawables.ic_signal_strength_3
        in -55..0 -> Drawables.ic_signal_strength_4
        else -> Drawables.ic_signal_strength_0
    }
    imageView.setImageDrawable(ContextCompat.getDrawable(imageView.context, resId))
}

@BindingAdapter("hasGrayScale")
fun setGrayScale(iv: ImageView, hasGrayScale: Boolean) {
    if (hasGrayScale) {
        val matrix = ColorMatrix()
        matrix.setSaturation(0f)
        iv.colorFilter = ColorMatrixColorFilter(matrix)
    } else {
        iv.clearColorFilter()
    }
}