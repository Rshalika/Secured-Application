package com.strawhat.securedapplication.injection

import android.content.Context
import com.google.gson.Gson
import com.strawhat.securedapplication.MyApplication
import com.strawhat.securedapplication.sttings.SettingsRepository
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class ApplicationModule(val application: MyApplication) {

    @Provides
    fun provideContext(): Context {
        return application.applicationContext
    }

    @Provides
    @Singleton
    fun provideGson(): Gson {
        return Gson()
    }

    @Provides
    @Singleton
    fun provideSettingsRepository(context: Context, gson: Gson): SettingsRepository {
        return SettingsRepository(context, gson)
    }

}