package com.launcher_control_android.helper.util

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator

fun View.animateRotate() {
    val rotate = ObjectAnimator.ofFloat(this, "rotation", 360f, 0f)
    rotate.duration = 500
    rotate.start()
}

fun View.animateHorizontalFlip() {
    this.cameraDistance = 8000 * this.resources.displayMetrics.density

    val rotateOut = ObjectAnimator.ofFloat(this, View.ROTATION_Y, 0f, 90f)
    val rotateIn = ObjectAnimator.ofFloat(this, View.ROTATION_Y, -90f, 0f)

    val scaleDownX = ObjectAnimator.ofFloat(this, View.SCALE_X, 1f, 0.9f)
    val scaleUpX = ObjectAnimator.ofFloat(this, View.SCALE_X, 0.9f, 1f)

    AnimatorSet().apply {
        play(rotateOut).with(scaleDownX)
        play(rotateIn).with(scaleUpX).after(rotateOut)
        duration = 200
        interpolator = AccelerateDecelerateInterpolator()
        start()
    }
}

fun View.animateWave() {
    this.visibility = View.VISIBLE
    this.scaleX = 0f
    this.scaleY = 0f
    this.alpha = 1f

    val scaleX = ObjectAnimator.ofFloat(this, View.SCALE_X, 0f, 1.2f)
    val scaleY = ObjectAnimator.ofFloat(this, View.SCALE_Y, 0f, 1.2f)
    val alpha = ObjectAnimator.ofFloat(this, View.ALPHA, 1f, 0f)

    AnimatorSet().apply {
        playTogether(scaleX, scaleY, alpha)
        duration = 300
        interpolator = AccelerateDecelerateInterpolator()

        addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                // RESET so icon stays visible
                this@animateWave.alpha = 1f
                this@animateWave.scaleX = 1f
                this@animateWave.scaleY = 1f
            }
        })

        start()
    }

    scaleX.repeatCount = 1
    scaleY.repeatCount = 1
    alpha.repeatCount = 1
}

fun View.triggerHaptic() {
    // 1. Try system haptic first
    val performed = this.performHapticFeedback(
        HapticFeedbackConstants.KEYBOARD_TAP,
        HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
    )

    // 2. Fallback to vibrator if OEM ignored it
    if (!performed) {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager =
                this.context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            manager.defaultVibrator
        } else {
            this.context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        if (vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(
                    VibrationEffect.createOneShot(
                        35, // duration ms
                        255
                    )
                )
            } else {
                vibrator.vibrate(35)
            }
        }
    }
}