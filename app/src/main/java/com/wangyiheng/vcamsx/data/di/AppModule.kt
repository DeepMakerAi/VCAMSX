package com.wangyiheng.vcamsx.data.di


import com.wangyiheng.vcamsx.utils.InfoManager
import org.koin.dsl.module
val appModule = module {
    single { InfoManager(get()) }
}