package com.strawhat.securedapplication.vm.events

import com.strawhat.securedapplication.sttings.SettingsModel

data class MainViewState(
    val passwordUnlocked: Boolean = false,
    val splashScreenVisible: Boolean = false,
    val passwordEnabled: Boolean = false,
    val enterFirstPasswordViewVisible: Boolean = false,
    val enterSecondPasswordViewVisible: Boolean = false,
    val firstPassword: String? = null,
    val secondPassword: String? = null,
    val mainScreenVisible: Boolean = false,
    val settingsModel: SettingsModel = SettingsModel(),
    val loading: Boolean = false,
    val errorMessage: String? = null
)