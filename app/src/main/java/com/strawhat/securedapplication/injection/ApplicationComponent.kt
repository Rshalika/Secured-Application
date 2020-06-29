package com.strawhat.securedapplication.injection

import com.strawhat.securedapplication.vm.MainViewModel
import com.strawhat.securedapplication.sttings.SettingsRepository
import dagger.Component

@Component(modules = [ApplicationModule::class])
interface ApplicationComponent {

    fun settingsRepository(): SettingsRepository

    fun inject(mainViewModel: MainViewModel)
}