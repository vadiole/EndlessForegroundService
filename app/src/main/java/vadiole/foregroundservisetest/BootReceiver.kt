package vadiole.foregroundservisetest

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import timber.log.Timber
import vadiole.foregroundservisetest.preferences.Config

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        Timber.i("action: ${intent?.action}")
        if (Config.loadOnBootEnabled) {
            MyForegroundService.start(context)
        }
    }
}
