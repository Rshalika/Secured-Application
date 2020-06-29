package com.strawhat.securedapplication

import android.app.Application
import com.strawhat.securedapplication.injection.ApplicationComponent
import com.strawhat.securedapplication.injection.ApplicationModule
import com.strawhat.securedapplication.injection.DaggerApplicationComponent

class MyApplication : Application() {

    val appComponent: ApplicationComponent =
        DaggerApplicationComponent
            .builder()
            .applicationModule(ApplicationModule(this))
            .build()
}