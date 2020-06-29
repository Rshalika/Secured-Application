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
import javax.inject.Inject

class MainViewModel(application: Application) : AndroidViewModel(application) {

    @Inject
    private lateinit var settingsRepository: SettingsRepository

    private val viewActionRelay = PublishRelay.create<ViewAction>()

    private val viewResultsRelay = PublishRelay.create<ViewResult>()

    private val disposable = CompositeDisposable()

    private var previousState: MainViewState = MainViewState()

    val viewStateRelay: BehaviorRelay<MainViewState> = BehaviorRelay.create<MainViewState>()

    init {
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
                    return InitialLoadingResult(settingsRepository.readSettingsModel())
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
                    shared.ofType(InitialLoadingAction::class.java).compose(initialLoading)
                )
            }
        }
        disposable.add(viewActionRelay.compose(UI)
            .mergeWith(viewResultsRelay)
            .observeOn(AndroidSchedulers.mainThread())
            .scan(previousState, { state, result ->
                return@scan reduce(state, result)
            })
            .subscribeBy(
                onNext = {
                    emmit(it)
                },
                onError = {
                    throw OnErrorNotImplementedException(it)
                }
            ))
    }

    fun emmit(state: MainViewState) {
        previousState = state
        viewStateRelay.accept(state)
    }

    private fun reduce(state: MainViewState, result: ViewResult): MainViewState {
        return when (result) {
            is PasswordAttemptResult -> state.copy(
                loading = false,
                settingsModel = result.settingsModel
            )
            is SetPasswordResult -> state.copy(
                loading = false,
                settingsModel = result.settingsModel
            )
            is RemovePasswordResult -> state.copy(
                loading = false,
                settingsModel = result.settingsModel
            )
            LoadingResult -> state.copy(loading = true)
            EnablePasswordClickedResult -> state.copy(
                loading = false,
                mainScreenVisible = false,
                enterFirstPasswordViewVisible = true,
                enterSecondPasswordViewVisible = false
            )
            EnterFirstPasswordClicked -> {
                state.copy(
                    loading = false,
                    enterSecondPasswordViewVisible = true,
                    enterFirstPasswordViewVisible = false
                )
            }
            is FirstPasswordChangedResult -> {
                state.copy(firstPassword = result.password)
            }
            is SecondPasswordChangedResult -> {
                state.copy(secondPassword = result.password)
            }
            is InitialLoadingResult -> {
                state.copy(splashScreenVisible = false, mainScreenVisible = true)
            }
            PasswordsDoNotMatchResult -> state.copy(
                loading = false,
                errorMessage = "PasswordsDoNoMatch"
            )
        }
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

}