package com.anistream.app

import android.app.Application
import com.anistream.app.di.AppContainer

class AniStreamApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
