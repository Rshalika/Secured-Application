package com.strawhat.securedapplication.vm.events

import com.strawhat.securedapplication.sttings.SettingsModel
import java.util.*

data class MainViewState(
    val passwordUnlocked: Boolean = false,
    val splashScreenVisible: Boolean = false,
    val loginScreenVisible: Boolean = false,
    val passwordEnabled: Boolean = false,
    val enterFirstPasswordViewVisible: Boolean = false,
    val enterSecondPasswordViewVisible: Boolean = false,
    val firstPassword: String? = null,
    val secondPassword: String? = null,
    val mainScreenVisible: Boolean = false,
    val settingsModel: SettingsModel = SettingsModel(),
    val loading: Boolean = false,
    val loginErrorMessage: String? = null,
    val blockErrorMessage: String? = null,
    val secondPasswordErrorMessage: String? = null,
    val availableFrom: Date? = null,
    var ignoreUpdate: Boolean = false
)