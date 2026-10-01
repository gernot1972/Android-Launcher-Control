package com.launcher_control_android.data.model.response

import com.google.gson.annotations.SerializedName
import java.util.UUID

data class ProgramProfileModel(
    @SerializedName("id")
    var id: String = UUID.randomUUID().toString(),

    @SerializedName("name")
    var name: String = "Default Program",

    @SerializedName("items")
    var items: List<ProgramItemModel> = ProgramItemModel.getDefaultSequence()
)