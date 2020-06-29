package com.strawhat.securedapplication.vm

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.jakewharton.rxrelay3.BehaviorRelay
import com.jakewharton.rxrelay3.PublishRelay
import com.strawhat.securedapplication.sttings.SettingsRepository
import com.strawhat.securedapplication.vm.events.*
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.ObservableTransformer
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.exceptions.OnErrorNotImplementedException
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.schedulers.Schedulers
import java.util.*
import javax.inject.Inject

class MainViewModel(application: Application) : AndroidViewModel(application) {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    private val viewActionRelay = PublishRelay.create<ViewAction>()

    private val viewResultsRelay = PublishRelay.create<ViewResult>()

    private val disposable = CompositeDisposable()

    private var previousState: MainViewState = MainViewState()

    val viewStateRelay: BehaviorRelay<MainViewState> = BehaviorRelay.create<MainViewState>()

    fun afterInit() {
        val passwordAttemptClear = ObservableTransformer<PasswordAttemptRemoveTimeAction, ViewResult> { event ->
            return@ObservableTransformer event.flatMap { action ->
                return@flatMap Observable.fromCallable(fun(): ViewResult {
                    val settingsModel = settingsRepository.readSettingsModel()
                    return if (settingsModel.failedAttempts >= 3 && settingsModel.availableFrom!!.time < System.currentTimeMillis()) {
                        settingsRepository.removeTime()
                        val attempt = settingsRepository.attempt(password = action.password)
                        PasswordAttemptResult(attempt.first, attempt.second)
                    } else {
                        PasswordAttemptSkipResult
                    }
                })
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .startWithItem(LoadingResult)

            }
        }

        val passwordAttempt = ObservableTransformer<PasswordAttemptAction, ViewResult> { event ->
            return@ObservableTransformer event.flatMap { action ->
                return@flatMap Observable.fromCallable(fun(): ViewResult {
                    val attempt = settingsRepository.attempt(action.password)
                    return PasswordAttemptResult(attempt.first, attempt.second)
                })
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .startWithItem(LoadingResult)

            }
        }

        val setPassword = ObservableTransformer<SetPasswordAction, ViewResult> { event ->
            return@ObservableTransformer event.flatMap { action ->
                return@flatMap Observable.fromCallable(fun(): ViewResult {
                    return SetPasswordResult(settingsRepository.savePassword(password = action.password))
                })
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .startWithItem(LoadingResult)
            }
        }

        val removePassword = ObservableTransformer<RemovePasswordAction, ViewResult> { event ->
            return@ObservableTransformer event.flatMap { action ->
                return@flatMap Observable.fromCallable(fun(): ViewResult {
                    return RemovePasswordResult(settingsRepository.removePassword())
                })
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .startWithItem(LoadingResult)
            }
        }
        val initialLoading = ObservableTransformer<InitialLoadingAction, ViewResult> { event ->
            return@ObservableTransformer event.flatMap { action ->
                return@flatMap Observable.fromCallable(fun(): ViewResult {
                    val settingsModel = settingsRepository.readSettingsModel()
                    return if (settingsModel.failedAttempts >= 3 && settingsModel.availableFrom!!.time < System.currentTimeMillis()) {
                        InitialLoadingResult(settingsRepository.removeTime())
                    } else {
                        InitialLoadingResult(settingsModel)
                    }
                })
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .startWithItem(LoadingResult)
            }
        }

        val UI = ObservableTransformer<ViewAction, ViewResult> { event ->
            return@ObservableTransformer event.publish { shared ->
                return@publish Observable.mergeArray(
                    shared.ofType(PasswordAttemptAction::class.java).compose(passwordAttempt),
                    shared.ofType(RemovePasswordAction::class.java).compose(removePassword),
                    shared.ofType(PasswordAttemptAction::class.java).compose(passwordAttempt),
                    shared.ofType(SetPasswordAction::class.java).compose(setPassword),
                    shared.ofType(InitialLoadingAction::class.java).compose(initialLoading),
                    shared.ofType(PasswordAttemptRemoveTimeAction::class.java).compose(passwordAttemptClear)
                )
            }
        }
        disposable.add(
            viewActionRelay
                .startWithItem(InitialLoadingAction)
                .compose(UI)
                .mergeWith(viewResultsRelay)
                .observeOn(AndroidSchedulers.mainThread())
                .scan(previousState, { state, result ->
                    return@scan reduce(state, result)
                })
                .filter { !it.ignoreUpdate }
                .subscribeBy(
                    onNext = {
                        emmit(it)
                    },
                    onError = {
                        throw OnErrorNotImplementedException(it)
                    }
                )
        )
    }

    fun emmit(state: MainViewState) {
        previousState = state
        viewStateRelay.accept(state)
    }

    private fun reduce(state: MainViewState, result: ViewResult): MainViewState {
        val newState = when (result) {
            is PasswordAttemptResult -> {
                val success = result.success
                if (success) {
                    state.copy(
                        loading = false,
                        settingsModel = result.settingsModel,
                        passwordEnabled = result.settingsModel.password != null,
                        mainScreenVisible = true,
                        loginScreenVisible = false,
                        enterFirstPasswordViewVisible = false,
                        enterSecondPasswordViewVisible = false,
                        splashScreenVisible = false,
                        blockErrorMessage = null,
                        loginErrorMessage = null,
                        secondPasswordErrorMessage = null
                    )
                } else {
                    if (result.settingsModel.failedAttempts >= 3) {
                        state.copy(
                            availableFrom = result.settingsModel.availableFrom!!,
                            passwordEnabled = result.settingsModel.password != null,
                            loading = false,
                            settingsModel = result.settingsModel,
                            mainScreenVisible = false,
                            loginScreenVisible = true,
                            enterFirstPasswordViewVisible = false,
                            enterSecondPasswordViewVisible = false,
                            splashScreenVisible = false,
                            blockErrorMessage = "Too many attempts",
                            loginErrorMessage = "Incorrect password",
                            secondPasswordErrorMessage = null
                        )
                    } else {
                        state.copy(
                            passwordEnabled = result.settingsModel.password != null,
                            loading = false,
                            settingsModel = result.settingsModel,
                            mainScreenVisible = false,
                            loginScreenVisible = true,
                            enterFirstPasswordViewVisible = false,
                            enterSecondPasswordViewVisible = false,
                            splashScreenVisible = false,
                            blockErrorMessage = null,
                            loginErrorMessage = "Incorrect password",
                            secondPasswordErrorMessage = null
                        )
                    }
                }
            }
            is SetPasswordResult -> {
                state.copy(
                    passwordEnabled = result.settingsModel.password != null,
                    enterSecondPasswordViewVisible = false,
                    enterFirstPasswordViewVisible = false,
                    splashScreenVisible = false,
                    mainScreenVisible = true,
                    loading = false,
                    loginErrorMessage = null,
                    settingsModel = result.settingsModel
                )
            }
            is RemovePasswordResult -> state.copy(
                passwordEnabled = result.settingsModel.password != null,
                loading = false,
                settingsModel = result.settingsModel,
                mainScreenVisible = true,
                loginScreenVisible = false,
                enterFirstPasswordViewVisible = false,
                enterSecondPasswordViewVisible = false,
                splashScreenVisible = false,
                blockErrorMessage = null,
                loginErrorMessage = null,
                secondPasswordErrorMessage = null
            )
            LoadingResult -> state.copy(loading = true)
            EnablePasswordClickedResult -> state.copy(
                loading = false,
                mainScreenVisible = false,
                enterFirstPasswordViewVisible = true,
                enterSecondPasswordViewVisible = false,
                blockErrorMessage = null,
                loginErrorMessage = null,
                secondPasswordErrorMessage = null
            )
            EnterFirstPasswordClicked -> {
                state.copy(
                    loading = false,
                    enterSecondPasswordViewVisible = true,
                    enterFirstPasswordViewVisible = false,
                    blockErrorMessage = null,
                    loginErrorMessage = null,
                    secondPasswordErrorMessage = null
                )
            }
            is FirstPasswordChangedResult -> {
                state.copy(firstPassword = result.password)
            }
            is SecondPasswordChangedResult -> {
                state.copy(secondPassword = result.password)
            }
            is InitialLoadingResult -> {
                val passwordEnabled = result.settingsModel.password != null
                var errorMessage: String? = null
                var availableFrom: Date? = null
                if (result.settingsModel.failedAttempts >= 3) {
                    errorMessage = "TooManyAttempts"
                    availableFrom = result.settingsModel.availableFrom!!
                }

                return state.copy(
                    settingsModel = result.settingsModel,
                    availableFrom = availableFrom,
                    mainScreenVisible = passwordEnabled.not(),
                    loginScreenVisible = passwordEnabled,
                    blockErrorMessage = errorMessage
                )
            }
            PasswordsDoNotMatchResult -> state.copy(
                loading = false,
                enterFirstPasswordViewVisible = false,
                enterSecondPasswordViewVisible = true,
                mainScreenVisible = false,
                splashScreenVisible = false,
                loginScreenVisible = false,
                secondPasswordErrorMessage = "PasswordsDoNoMatch",
                blockErrorMessage = null,
                loginErrorMessage = null
            )
            PasswordAttemptSkipResult -> state
        }
        newState.ignoreUpdate = result is PasswordAttemptSkipResult
        return newState
    }

    override fun onCleared() {
        super.onCleared()
        disposable.dispose()
    }

    fun enablePasswordClicked() {
        viewResultsRelay.accept(EnablePasswordClickedResult)
    }

    fun enterFirstPasswordClicked() {
        viewResultsRelay.accept(EnterFirstPasswordClicked)
    }

    fun enterSecondPasswordClicked() {
        if (previousState.firstPassword == previousState.secondPassword && previousState.firstPassword != null) {
            viewActionRelay.accept(SetPasswordAction(previousState.firstPassword!!))
        } else {
            viewResultsRelay.accept(PasswordsDoNotMatchResult)
        }
    }

    fun firstPasswordChanged(firstPassword: String) {
        viewResultsRelay.accept(FirstPasswordChangedResult(firstPassword))
    }

    fun secondPasswordChanged(secondPassword: String) {
        viewResultsRelay.accept(SecondPasswordChangedResult(secondPassword))
    }

    fun passwordAttempt(password: String) {
        if (previousState.settingsModel.failedAttempts >= 3) {
            viewActionRelay.accept(PasswordAttemptRemoveTimeAction(password))
        } else {
            viewActionRelay.accept(PasswordAttemptAction(password))
        }
    }

    fun disablePasswordClicked() {
        viewActionRelay.accept(RemovePasswordAction)
    }

    fun onResume() {
        viewActionRelay.accept(InitialLoadingAction)
    }

}