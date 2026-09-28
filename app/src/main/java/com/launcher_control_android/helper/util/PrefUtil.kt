package com.launcher_control_android.helper.util

import android.content.Context
import android.content.SharedPreferences
import com.launcher_control_android.AppConstants.Prefs.ADVANCED_CONTROL_IS_STANDARD
import com.launcher_control_android.AppConstants.Prefs.ARMED_INTERVAL
import com.launcher_control_android.AppConstants.Prefs.AUTH_TOKEN
import com.launcher_control_android.AppConstants.Prefs.AUTOUPDATE_PRESSURE_DELAY
import com.launcher_control_android.AppConstants.Prefs.AUTOUPDATE_PRESSURE_ENABLE
import com.launcher_control_android.AppConstants.Prefs.BLUETOOTH_DEVICE
import com.launcher_control_android.AppConstants.Prefs.FIRE_DELAY
import com.launcher_control_android.AppConstants.Prefs.GUNSHOT_AFTER_LAUNCH_ENABLE
import com.launcher_control_android.AppConstants.Prefs.RANDOMIZED_SOUND_AND_FIRE
import com.launcher_control_android.AppConstants.Prefs.RANDOMIZED_SOUND_ENABLE
import com.launcher_control_android.AppConstants.Prefs.RANDOM_FIRE_DELAY
import com.launcher_control_android.AppConstants.Prefs.RANDOM_FIRE_DELAY_MAX
import com.launcher_control_android.AppConstants.Prefs.RANDOM_FIRE_DELAY_MIN
import com.launcher_control_android.AppConstants.Prefs.RANDOM_SOUND_COUNT
import com.launcher_control_android.AppConstants.Prefs.RANDOM_SOUND_COUNT_MAX
import com.launcher_control_android.AppConstants.Prefs.RANDOM_SOUND_COUNT_MIN
import com.launcher_control_android.AppConstants.Prefs.RANDOM_SOUND_DELAY
import com.launcher_control_android.AppConstants.Prefs.RANDOM_SOUND_DELAY_MAX
import com.launcher_control_android.AppConstants.Prefs.RANDOM_SOUND_DELAY_MIN
import com.launcher_control_android.AppConstants.Prefs.SAME_SOUND_FOR_MULTIPLE_SOUNDS
import com.launcher_control_android.AppConstants.Prefs.SELECTED_SOUND_FOR_RANDOMIZE
import com.launcher_control_android.AppConstants.Prefs.SHOW_BLUETOOTH_AUDIO
import com.launcher_control_android.AppConstants.Prefs.SOUND_COUNT
import com.launcher_control_android.AppConstants.Prefs.SOUND_DELAY
import com.launcher_control_android.AppConstants.Prefs.UNIT_1_MODEL
import com.launcher_control_android.AppConstants.Prefs.UNIT_2_MODEL
import com.launcher_control_android.AppConstants.Prefs.UNIT_3_MODEL
import com.launcher_control_android.AppConstants.Prefs.UNIT_4_MODEL
import com.launcher_control_android.Strings
import com.launcher_control_android.data.model.response.DeviceModel
import com.launcher_control_android.data.model.response.UnitModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class PrefUtil
@Inject
constructor(@ApplicationContext context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(context.getString(Strings.app_name), Context.MODE_PRIVATE)

    private val prefEditor: SharedPreferences.Editor

    init {
        prefEditor = prefs.edit()
    }

    var authToken: String?
        get() = prefs.getString(AUTH_TOKEN, "")
        set(authToken) {
            prefEditor.putString(AUTH_TOKEN, authToken)
            prefEditor.apply()
        }

    var savedBluetoothDevice: DeviceModel?
        get() = prefs.getString(BLUETOOTH_DEVICE, "")?.fromJson()
        set(data) {
            prefEditor.putString(BLUETOOTH_DEVICE, data?.toJson())
            prefEditor.apply()
        }

    var unit1Model: UnitModel
        get() = prefs.getString(UNIT_1_MODEL, "")?.fromJson() ?: UnitModel(1, 0, false, false, 0, 0)
        set(data) {
            prefEditor.putString(UNIT_1_MODEL, data?.toJson())
            prefEditor.apply()
        }

    var unit2Model: UnitModel
        get() = prefs.getString(UNIT_2_MODEL, "")?.fromJson() ?: UnitModel(2, 0, false, false, 0, 0)
        set(data) {
            prefEditor.putString(UNIT_2_MODEL, data?.toJson())
            prefEditor.apply()
        }

    var unit3Model: UnitModel
        get() = prefs.getString(UNIT_3_MODEL, "")?.fromJson() ?: UnitModel(3, 0, false, false, 0, 0)
        set(data) {
            prefEditor.putString(UNIT_3_MODEL, data?.toJson())
            prefEditor.apply()
        }

    var unit4Model: UnitModel
        get() = prefs.getString(UNIT_4_MODEL, "")?.fromJson() ?: UnitModel(4, 0, false, false, 0, 0)
        set(data) {
            prefEditor.putString(UNIT_4_MODEL, data?.toJson())
            prefEditor.apply()
        }

    var soundCount: Int
        get() = prefs.getInt(SOUND_COUNT, 1)
        set(data) {
            prefEditor.putInt(SOUND_COUNT, data)
            prefEditor.apply()
        }

    var soundDelay: Int
        get() = prefs.getInt(SOUND_DELAY, 3)
        set(data) {
            prefEditor.putInt(SOUND_DELAY, data)
            prefEditor.apply()
        }

    var fireDelay: Int
        get() = prefs.getInt(FIRE_DELAY, 3)
        set(data) {
            prefEditor.putInt(FIRE_DELAY, data)
            prefEditor.apply()
        }

    var showBluetoothAudio: Boolean
        get() = prefs.getBoolean(SHOW_BLUETOOTH_AUDIO, true)
        set(data) {
            prefEditor.putBoolean(SHOW_BLUETOOTH_AUDIO, data)
            prefEditor.apply()
        }

    var armedInterval: Int
        get() = prefs.getInt(ARMED_INTERVAL, 30)
        set(data) {
            prefEditor.putInt(ARMED_INTERVAL, data)
            prefEditor.apply()
        }

    var autoupdatePressureDelay: Int
        get() = prefs.getInt(AUTOUPDATE_PRESSURE_DELAY, 5)
        set(data) {
            prefEditor.putInt(AUTOUPDATE_PRESSURE_DELAY, data)
            prefEditor.apply()
        }

    var autoupdatePressureEnable: Boolean
        get() = prefs.getBoolean(AUTOUPDATE_PRESSURE_ENABLE, true)
        set(data) {
            prefEditor.putBoolean(AUTOUPDATE_PRESSURE_ENABLE, data)
            prefEditor.apply()
        }

    var gunshotAfterLaunchEnable: Boolean
        get() = prefs.getBoolean(GUNSHOT_AFTER_LAUNCH_ENABLE, false)
        set(data) {
            prefEditor.putBoolean(GUNSHOT_AFTER_LAUNCH_ENABLE, data)
            prefEditor.apply()
        }

    var advancedControlIsStandardEnable: Boolean
        get() = prefs.getBoolean(ADVANCED_CONTROL_IS_STANDARD, true)
        set(data) {
            prefEditor.putBoolean(ADVANCED_CONTROL_IS_STANDARD, data)
            prefEditor.apply()
        }

    var randomizedSoundAndFireEnable: Boolean
        get() = prefs.getBoolean(RANDOMIZED_SOUND_AND_FIRE, false)
        set(data) {
            prefEditor.putBoolean(RANDOMIZED_SOUND_AND_FIRE, data)
            prefEditor.apply()
        }

    var randomSoundCountEnable: Boolean
        get() = prefs.getBoolean(RANDOM_SOUND_COUNT, false)
        set(data) {
            prefEditor.putBoolean(RANDOM_SOUND_COUNT, data)
            prefEditor.apply()
        }

    var randomSoundCountMin: Int
        get() = prefs.getInt(RANDOM_SOUND_COUNT_MIN, 1)
        set(data) {
            prefEditor.putInt(RANDOM_SOUND_COUNT_MIN, data)
            prefEditor.apply()
        }

    var randomSoundCountMax: Int
        get() = prefs.getInt(RANDOM_SOUND_COUNT_MAX, 5)
        set(data) {
            prefEditor.putInt(RANDOM_SOUND_COUNT_MAX, data)
            prefEditor.apply()
        }

    var randomSoundDelayEnable: Boolean
        get() = prefs.getBoolean(RANDOM_SOUND_DELAY, false)
        set(data) {
            prefEditor.putBoolean(RANDOM_SOUND_DELAY, data)
            prefEditor.apply()
        }

    var randomSoundDelayMin: Int
        get() = prefs.getInt(RANDOM_SOUND_DELAY_MIN, 2)
        set(data) {
            prefEditor.putInt(RANDOM_SOUND_DELAY_MIN, data)
            prefEditor.apply()
        }

    var randomSoundDelayMax: Int
        get() = prefs.getInt(RANDOM_SOUND_DELAY_MAX, 10)
        set(data) {
            prefEditor.putInt(RANDOM_SOUND_DELAY_MAX, data)
            prefEditor.apply()
        }

    var randomFireDelayEnable: Boolean
        get() = prefs.getBoolean(RANDOM_FIRE_DELAY, false)
        set(data) {
            prefEditor.putBoolean(RANDOM_FIRE_DELAY, data)
            prefEditor.apply()
        }

    var randomFireDelayMin: Int
        get() = prefs.getInt(RANDOM_FIRE_DELAY_MIN, 2)
        set(data) {
            prefEditor.putInt(RANDOM_FIRE_DELAY_MIN, data)
            prefEditor.apply()
        }

    var randomFireDelayMax: Int
        get() = prefs.getInt(RANDOM_FIRE_DELAY_MAX, 10)
        set(data) {
            prefEditor.putInt(RANDOM_FIRE_DELAY_MAX, data)
            prefEditor.apply()
        }

    var randomizedSoundEnable: Boolean
        get() = prefs.getBoolean(RANDOMIZED_SOUND_ENABLE, false)
        set(data) {
            prefEditor.putBoolean(RANDOMIZED_SOUND_ENABLE, data)
            prefEditor.apply()
        }

    var canSameSoundForMultipleSounds: Boolean
        get() = prefs.getBoolean(SAME_SOUND_FOR_MULTIPLE_SOUNDS, false)
        set(data) {
            prefEditor.putBoolean(SAME_SOUND_FOR_MULTIPLE_SOUNDS, data)
            prefEditor.apply()
        }

    var selectedSoundForRandomize: MutableSet<String>
        get() = prefs.getStringSet(SELECTED_SOUND_FOR_RANDOMIZE, mutableSetOf()) ?: mutableSetOf()
        set(data) {
            prefEditor.putStringSet(SELECTED_SOUND_FOR_RANDOMIZE, data)
            prefEditor.apply()
        }

    fun hasKey(key: String) = prefs.contains(key)

    fun clearPrefs() {
        prefs.all.forEach {
            prefEditor.remove(it.key)
        }
        prefEditor.apply()
    }
}
