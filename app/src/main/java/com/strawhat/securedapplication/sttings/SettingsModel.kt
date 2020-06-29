package com.strawhat.securedapplication.sttings

import com.google.gson.annotations.SerializedName
import java.util.*

data class SettingsModel(

    @SerializedName("password")
    var password: String? = null,

    @SerializedName("last_attempt_time")
    var availableFrom: Date? = null,

    var failedAttempts: Int = 0
)