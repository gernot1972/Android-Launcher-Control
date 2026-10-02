package com.launcher_control_android.data.model.response

import com.google.gson.annotations.SerializedName

data class ProgramItemModel(
    @SerializedName("index")
    var index: Int = 1,              // Schrittnummer (1..20)

    @SerializedName("delay")
    var delay: Int = 3,              // Verzögerung in Sekunden (1..300, Standard: 3)

    @SerializedName("fireUnit")
    var fireUnit: Int = 0,           // Ziel-Unit (0 = Keine, 1..4)

    @SerializedName("action")
    var action: Int = 0,             // 0 = No Action, 1 = Launch Dummy, 2..7 = Sounds

    @SerializedName("volumeStep")
    var volumeStep: Int = 4,          // Lautstärkestufe (1=15%, 2=30%, 3=50%, 4=100%)

    @SerializedName("targetChannel")
    var targetChannel: Int = 0       // Zielkanal (0 = nächster freier Kanal "-", 1..12 = fester Kanal)
) {
    val isUseSound: Boolean
        get() = action in 2..7 && fireUnit in 1..4

    val isEligibleForFire: Boolean
        get() = action == 1 && fireUnit in 1..4

    val isValidSequence: Boolean
        get() = isUseSound || isEligibleForFire

    val actionName: String
        get() = actionTitles.getOrNull(action) ?: "No Action"

    val shortActionName: String
        get() = when (action) {
            1 -> "Launch"
            2 -> "Duck"
            3 -> "Pheasant"
            4 -> "Goose"
            5 -> "Brrr"
            6 -> "Gunshot"
            7 -> "Magpie"
            else -> "None"
        }

    companion object {
        val actionTitles = listOf(
            "No Action",
            "Launch Dummy",
            "Play Duck",
            "Play Pheasant",
            "Play Goose",
            "Play Brrr",
            "Play Gunshot",
            "Play Magpie"
        )

        fun getDefaultSequence(): MutableList<ProgramItemModel> {
            return MutableList(20) { i ->
                ProgramItemModel(index = i + 1)
            }
        }
    }
}