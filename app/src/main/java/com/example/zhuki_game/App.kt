package com.example.zhuki_game

import android.app.Application
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.dsl.module

val appModule = module {
    single<SettingsStorage> { SharedPreferencesStorage(androidContext()) }
    single { SettingsRepository(get()) }
    single { UserRepository(androidContext()) }
    single { GoldRateRepository.getInstance(androidContext()) }
    single<SkinStorage> { SharedPreferencesSkinStorage(androidContext()) }
    single { SkinRepository(get()) }
    viewModel { GameViewModel(get(), get(), get(), get()) }
}

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@App)
            modules(appModule)
        }
    }
}
