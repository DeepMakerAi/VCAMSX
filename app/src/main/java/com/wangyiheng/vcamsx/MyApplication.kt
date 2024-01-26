package com.wangyiheng.vcamsx

import android.app.Application
import com.wangyiheng.vcamsx.data.di.appModule
import com.wangyiheng.vcamsx.data.services.networkModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize Koin
        startKoin {
            // Declare modules to use
            androidContext(this@MyApplication)
            modules(appModule,networkModule)
        }
    }
}