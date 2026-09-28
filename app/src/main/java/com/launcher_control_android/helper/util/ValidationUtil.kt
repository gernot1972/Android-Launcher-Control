package com.launcher_control_android.helper.util

import androidx.annotation.StringDef
import androidx.core.util.PatternsCompat
import java.util.regex.Pattern

object ValidationUtil {

    const val maxPasswordLength = 6
    val EMAIL_ADDRESS_PATTERN: String = PatternsCompat.EMAIL_ADDRESS.pattern()
    val MOBILE_PATTERN: String = "[0-9]{10}"

    @StringDef("MM/dd/yyyy", "EEE dd/MM/yy")
    annotation class DatePatterns

}

fun String.isPasswordValid() = length >= ValidationUtil.maxPasswordLength

fun String.isEmailValid() =
    Pattern.compile(ValidationUtil.EMAIL_ADDRESS_PATTERN, Pattern.CASE_INSENSITIVE).matcher(this)
        .matches()

fun String.isMobileValid() =
    Pattern.compile(ValidationUtil.MOBILE_PATTERN, Pattern.CASE_INSENSITIVE).matcher(this).matches()

infix fun String.matchesPassword(string: String): Boolean {
    return this == string
}
