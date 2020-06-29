package com.strawhat.securedapplication.vm.events

import com.strawhat.securedapplication.sttings.SettingsModel

sealed class ViewResult

data class PasswordAttemptResult(val success: Boolean, val settingsModel: SettingsModel) : ViewResult()
object PasswordAttemptSkipResult : ViewResult()
data class InitialLoadingResult( val settingsModel: SettingsModel) : ViewResult()
data class SetPasswordResult(val settingsModel: SettingsModel) : ViewResult()
data class RemovePasswordResult(val settingsModel: SettingsModel) : ViewResult()
object LoadingResult : ViewResult()
object EnablePasswordClickedResult : ViewResult()
object EnterFirstPasswordClicked : ViewResult()
object PasswordsDoNotMatchResult : ViewResult()
data class FirstPasswordChangedResult(val password: String) : ViewResult()
data class SecondPasswordChangedResult(val password: String) : ViewResult()