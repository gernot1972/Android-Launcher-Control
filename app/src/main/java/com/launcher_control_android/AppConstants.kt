package com.launcher_control_android

import java.util.UUID

class AppConstants {

    object App {
        val listOfSound = mutableListOf("Duck", "Pheasant", "Goose", "Brrr", "Gunshot", "Magpie")

        val listOfPressure = mutableListOf("0bar", "2bar", "4bar", "6bar", "8bar", "10bar")

        // UUID of the service containing the FFE1 characteristic
        var SERVICE_UUID = UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb")

        // UUID of the FFE1 characteristic
        val CHARACTERISTIC_UUID = UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb")
    }

    object Prefs {
        const val AUTH_TOKEN = "1"
        const val BLUETOOTH_DEVICE = "2"
        const val UNIT_1_MODEL = "3"
        const val UNIT_2_MODEL = "4"
        const val UNIT_3_MODEL = "5"
        const val UNIT_4_MODEL = "6"
        const val SOUND_COUNT = "7"
        const val SOUND_DELAY = "8"
        const val FIRE_DELAY = "9"
        const val SHOW_BLUETOOTH_AUDIO = "10"
        const val ARMED_INTERVAL = "11"
        const val AUTOUPDATE_PRESSURE_ENABLE = "12"
        const val AUTOUPDATE_PRESSURE_DELAY = "13"
        const val GUNSHOT_AFTER_LAUNCH_ENABLE = "14"
        const val ADVANCED_CONTROL_IS_STANDARD = "15"
        const val RANDOMIZED_SOUND_AND_FIRE = "16"
        const val RANDOM_SOUND_COUNT = "17"
        const val RANDOM_SOUND_COUNT_MIN = "18"
        const val RANDOM_SOUND_COUNT_MAX = "19"
        const val RANDOM_SOUND_DELAY = "20"
        const val RANDOM_SOUND_DELAY_MIN = "21"
        const val RANDOM_SOUND_DELAY_MAX = "22"
        const val RANDOM_FIRE_DELAY = "23"
        const val RANDOM_FIRE_DELAY_MIN = "24"
        const val RANDOM_FIRE_DELAY_MAX = "25"
        const val RANDOMIZED_SOUND_ENABLE = "26"
        const val SAME_SOUND_FOR_MULTIPLE_SOUNDS = "27"
        const val SELECTED_SOUND_FOR_RANDOMIZE = "28"

    }

    object Communication {

        object RequestCode {

        }

        object ResponseCode {

        }

        object Broadcast {

        }

        object BundleData {
            const val MAIN_ACT_HEADING = "1"
            const val IS_UNAUTHORISED = "2"
            const val INTENT_UNIT_MODEL = "3"
            const val INTENT_POSITION = "4"
        }
    }

    object Api {

        object ResponseCode {
            const val UNAUTHORIZED_CODE = 401
        }

        object EndUrl {
            const val LOGIN = "login"
            const val SIGN_UP = "signup"
        }
    }

    object CommandResponse {
        const val GOT_IT = "got it"
        const val NO_REPLY = "no reply"
        const val NO_RESPONSE = "no response"

        fun isSuccess(response: String?): Boolean {
            if (response == null) return false
            return response.equals(GOT_IT, ignoreCase = true) || response.equals("S", ignoreCase = true)
        }
    }

    object Command {
        const val testUnit1 = "1F" // Test Command for Unit 1
        const val testUnit2 = "2F" // Test Command for Unit 2
        const val testUnit3 = "3F" // Test Command for Unit 3
        const val testUnit4 = "4F" // Test Command for Unit 4

        const val reloadUnit1 = "10" // Reload Command for Unit 1
        const val reloadUnit2 = "20" // Reload Command for Unit 2
        const val reloadUnit3 = "30" // Reload Command for Unit 3
        const val reloadUnit4 = "40" // Reload Command for Unit 4

        const val fetchDataUnit1 = "1E" // Fetch Data Command for Unit 1
        const val fetchDataUnit2 = "2E" // Fetch Data Command for Unit 2
        const val fetchDataUnit3 = "3E" // Fetch Data Command for Unit 3
        const val fetchDataUnit4 = "4E" // Fetch Data Command for Unit 4

        const val channel1Unit1 = "11"
        const val channel2Unit1 = "12"
        const val channel3Unit1 = "13"
        const val channel4Unit1 = "14"
        const val channel5Unit1 = "15"
        const val channel6Unit1 = "16"
        const val channel7Unit1 = "17"
        const val channel8Unit1 = "18"
        const val channel9Unit1 = "19"
        const val channel10Unit1 = "1a"
        const val channel11Unit1 = "1b"
        const val channel12Unit1 = "1c"

        const val channel1Unit2 = "21"
        const val channel2Unit2 = "22"
        const val channel3Unit2 = "23"
        const val channel4Unit2 = "24"
        const val channel5Unit2 = "25"
        const val channel6Unit2 = "26"
        const val channel7Unit2 = "27"
        const val channel8Unit2 = "28"
        const val channel9Unit2 = "29"
        const val channel10Unit2 = "2a"
        const val channel11Unit2 = "2b"
        const val channel12Unit2 = "2c"

        const val channel1Unit3 = "31"
        const val channel2Unit3 = "32"
        const val channel3Unit3 = "33"
        const val channel4Unit3 = "34"
        const val channel5Unit3 = "35"
        const val channel6Unit3 = "36"
        const val channel7Unit3 = "37"
        const val channel8Unit3 = "38"
        const val channel9Unit3 = "39"
        const val channel10Unit3 = "3a"
        const val channel11Unit3 = "3b"
        const val channel12Unit3 = "3c"

        const val channel1Unit4 = "41"
        const val channel2Unit4 = "42"
        const val channel3Unit4 = "43"
        const val channel4Unit4 = "44"
        const val channel5Unit4 = "45"
        const val channel6Unit4 = "46"
        const val channel7Unit4 = "47"
        const val channel8Unit4 = "48"
        const val channel9Unit4 = "49"
        const val channel10Unit4 = "4a"
        const val channel11Unit4 = "4b"
        const val channel12Unit4 = "4c"

        const val sound1Unit1 = "A1"
        const val sound2Unit1 = "A2"
        const val sound3Unit1 = "A3"
        const val sound4Unit1 = "A4"
        const val sound5Unit1 = "A5"
        const val sound6Unit1 = "A6"

        const val sound1Unit2 = "B1"
        const val sound2Unit2 = "B2"
        const val sound3Unit2 = "B3"
        const val sound4Unit2 = "B4"
        const val sound5Unit2 = "B5"
        const val sound6Unit2 = "B6"

        const val sound1Unit3 = "C1"
        const val sound2Unit3 = "C2"
        const val sound3Unit3 = "C3"
        const val sound4Unit3 = "C4"
        const val sound5Unit3 = "C5"
        const val sound6Unit3 = "C6"

        const val sound1Unit4 = "D1"
        const val sound2Unit4 = "D2"
        const val sound3Unit4 = "D3"
        const val sound4Unit4 = "D4"
        const val sound5Unit4 = "D5"
        const val sound6Unit4 = "D6"

        val listOfTestCommand = listOf(testUnit1, testUnit2, testUnit3, testUnit4)
        val listOfReloadCommand = listOf(reloadUnit1, reloadUnit2, reloadUnit3, reloadUnit4)
        val listOfFetchDataCommand = listOf(fetchDataUnit1, fetchDataUnit2, fetchDataUnit3, fetchDataUnit4)
        val listOfChannelCommand = listOf(
            channel1Unit1,
            channel2Unit1,
            channel3Unit1,
            channel4Unit1,
            channel5Unit1,
            channel6Unit1,
            channel7Unit1,
            channel8Unit1,
            channel9Unit1,
            channel10Unit1,
            channel11Unit1,
            channel12Unit1,
            channel1Unit2,
            channel2Unit2,
            channel3Unit2,
            channel4Unit2,
            channel5Unit2,
            channel6Unit2,
            channel7Unit2,
            channel8Unit2,
            channel9Unit2,
            channel10Unit2,
            channel11Unit2,
            channel12Unit2,
            channel1Unit3,
            channel2Unit3,
            channel3Unit3,
            channel4Unit3,
            channel5Unit3,
            channel6Unit3,
            channel7Unit3,
            channel8Unit3,
            channel9Unit3,
            channel10Unit3,
            channel11Unit3,
            channel12Unit3,
            channel1Unit4,
            channel2Unit4,
            channel3Unit4,
            channel4Unit4,
            channel5Unit4,
            channel6Unit4,
            channel7Unit4,
            channel8Unit4,
            channel9Unit4,
            channel10Unit4,
            channel11Unit4,
            channel12Unit4
        )

        val listOfSoundCommand = listOf(
            sound1Unit1,
            sound2Unit1,
            sound3Unit1,
            sound4Unit1,
            sound5Unit1,
            sound6Unit1,
            sound1Unit2,
            sound2Unit2,
            sound3Unit2,
            sound4Unit2,
            sound5Unit2,
            sound6Unit2,
            sound1Unit3,
            sound2Unit3,
            sound3Unit3,
            sound4Unit3,
            sound5Unit3,
            sound6Unit3,
            sound1Unit4,
            sound2Unit4,
            sound3Unit4,
            sound4Unit4,
            sound5Unit4,
            sound6Unit4
        )
    }
}
