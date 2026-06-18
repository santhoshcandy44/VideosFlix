package com.flix.videos

import android.app.Application
import org.koin.android.ext.koin.androidContext
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.core.context.startKoin

@Module
@ComponentScan("com.flix.videos")
class AppModule

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@App)
            modules(AppModule().module())
        }
    }
}
