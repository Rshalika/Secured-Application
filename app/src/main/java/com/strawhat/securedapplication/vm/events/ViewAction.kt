package com.strawhat.securedapplication.vm.events

sealed class ViewAction

object InitialLoadingAction : ViewAction()
data class PasswordAttemptAction(val password: String) : ViewAction()
data class SetPasswordAction(val password: String) : ViewAction()
object RemovePasswordAction : ViewAction()
