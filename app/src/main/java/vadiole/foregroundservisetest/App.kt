package vadiole.foregroundservisetest

import android.app.Application
import timber.log.Timber
import vadiole.foregroundservisetest.preferences.Config

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        Timber.plant(Timber.DebugTree())
        Config.init(applicationContext)
    }
}
