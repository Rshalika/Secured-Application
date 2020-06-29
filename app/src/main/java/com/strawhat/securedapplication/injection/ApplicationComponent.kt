package com.strawhat.securedapplication.injection

import com.strawhat.securedapplication.sttings.SettingsRepository
import com.strawhat.securedapplication.vm.MainViewModel
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(modules = [ApplicationModule::class])
interface ApplicationComponent {

    fun settingsRepository(): SettingsRepository

    fun inject(mainViewModel: MainViewModel)
}