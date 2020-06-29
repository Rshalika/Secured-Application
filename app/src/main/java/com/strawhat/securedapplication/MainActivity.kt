package com.strawhat.securedapplication

import android.content.Context
import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.CompoundButton
import android.widget.TextView.OnEditorActionListener
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import com.strawhat.securedapplication.utils.toVisibility
import com.strawhat.securedapplication.vm.MainViewModel
import com.strawhat.securedapplication.vm.events.MainViewState
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.exceptions.OnErrorNotImplementedException
import io.reactivex.rxjava3.kotlin.subscribeBy
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import kotlinx.android.synthetic.main.enter_first_password.*
import kotlinx.android.synthetic.main.enter_second_password.*
import kotlinx.android.synthetic.main.login_screen.*
import kotlinx.android.synthetic.main.splash_screen.*


class MainActivity : AppCompatActivity() {

    private val viewModel by viewModels<MainViewModel>()

    private val disposable = CompositeDisposable()

    private var editing = false

    private val timers = mutableListOf<CountDownTimer>()

    private val switchListener: (CompoundButton, Boolean) -> Unit = { buttonView, isChecked ->
        if (editing.not()) {
            if (isChecked) {
                viewModel.enablePasswordClicked()
            } else {
                viewModel.disablePasswordClicked()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.onResume()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        (application as MyApplication).appComponent.inject(viewModel)

        disposable.add(
            viewModel.viewStateRelay.subscribeBy(
                onNext = {
                    updateState(it)
                },
                onError = {
                    throw OnErrorNotImplementedException(it)
                }
            )
        )

        first_password_continue_btn.setOnClickListener {
            viewModel.enterFirstPasswordClicked()
        }
        first_password_text_field.addTextChangedListener {
            if (editing.not()) {
                viewModel.firstPasswordChanged(it?.toString() ?: "")
            }
        }
        second_password_continue_btn.setOnClickListener {
            viewModel.enterSecondPasswordClicked()
        }
        second_password_text_field.addTextChangedListener {
            if (editing.not()) {
                viewModel.secondPasswordChanged(it?.toString() ?: "")
            }
        }


        first_password_text_field.setOnEditorActionListener(OnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE && first_password_continue_btn.isEnabled) {
                viewModel.enterFirstPasswordClicked()
            }
            false
        })


        second_password_text_field.setOnEditorActionListener(OnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE && second_password_continue_btn.isEnabled) {
                viewModel.enterSecondPasswordClicked()
            }
            false
        })

        login_button.setOnClickListener {
            viewModel.passwordAttempt(password_text_field.text.toString())
        }

        password_switch.setOnCheckedChangeListener(switchListener)

    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        viewModel.afterInit()
    }


    private fun setCustomTitle(title: String) {
        custom_title.text = title
    }


    private fun updateState(state: MainViewState) {
        if (state.ignoreUpdate) {
            return
        }

        second_password_continue_btn.isEnabled = state.secondPassword != null && state.secondPassword.length == 4
        first_password_continue_btn.isEnabled = state.firstPassword != null && state.firstPassword.length == 4

        if (state.mainScreenVisible) {
            hideSoftKeyBoard()
        }

        if (state.loginScreenVisible) {
            password_text_field.requestFocus()
            setCustomTitle(getString(R.string.enter_pass_code))
        }

        if (state.enterFirstPasswordViewVisible) {
            setCustomTitle(getString(R.string.new_passcode))
            first_password_text_field.requestFocus()
        }
        if (state.enterSecondPasswordViewVisible) {
            second_password_text_field.requestFocus()
            setCustomTitle(getString(R.string.confirm_passcode))
        }
        if (state.mainScreenVisible) {
            setCustomTitle(getString(R.string.settings))
        }
        toolbar.visibility = state.splashScreenVisible.not().toVisibility()
        if (state.loginScreenVisible.not()) {
            editing = true
            password_text_field.setText("")
            editing = false
        }

        if (state.enterFirstPasswordViewVisible.not()) {
            editing = true
            first_password_text_field.setText("")
            editing = false
        }

        if (state.enterSecondPasswordViewVisible.not()) {
            editing = true
            second_password_text_field.setText("")
            editing = false
        }
        splash_screen.visibility = state.splashScreenVisible.toVisibility()
        login_screen.visibility = state.loginScreenVisible.toVisibility()
        first_password_screen.visibility = state.enterFirstPasswordViewVisible.toVisibility()
        second_password_screen.visibility = state.enterSecondPasswordViewVisible.toVisibility()
        password_switch.visibility = state.mainScreenVisible.toVisibility()

        password_error_message.visibility = (state.loginErrorMessage != null).toVisibility()
        password_error_message.text = state.loginErrorMessage

        blocked_error_message.visibility = (state.blockErrorMessage != null).toVisibility()

        if (state.availableFrom != null) {
            val seconds = (state.availableFrom.time - System.currentTimeMillis()) / 1000
            if (seconds > 0) {
                blocked_error_message.text = getString(R.string.too_many_attempts, seconds)
                timers.forEach {
                    it.cancel()
                }
                timers.clear()
                val timer = object : CountDownTimer(seconds * 1000, 1000) {
                    override fun onTick(millisUntilFinished: Long) {
                        blocked_error_message.text = getString(R.string.too_many_attempts, millisUntilFinished / 1000)
                    }

                    override fun onFinish() {
                        blocked_error_message.visibility = View.GONE
                    }
                }
                timers.add(timer)
                timer.start()
            }
        }

        second_password_error_message.visibility = (state.secondPasswordErrorMessage != null).toVisibility()

        this.editing = true
        password_switch.isChecked = state.passwordEnabled
        this.editing = false
    }

    private fun hideSoftKeyBoard() {
        val imm =
            getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        if (imm.isAcceptingText && currentFocus != null) {
            imm.hideSoftInputFromWindow(currentFocus!!.windowToken, 0)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        disposable.dispose()
    }

}