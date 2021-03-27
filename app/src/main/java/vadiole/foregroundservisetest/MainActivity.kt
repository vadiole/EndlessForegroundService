package vadiole.foregroundservisetest

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import vadiole.foregroundservisetest.databinding.ActivityMainBinding
import vadiole.foregroundservisetest.preferences.Config

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val context = this
        ActivityMainBinding.inflate(layoutInflater, null, false).apply {
            setContentView(this.root)


            runOnBoot.isChecked = Config.loadOnBootEnabled
            runOnBoot.setOnCheckedChangeListener { buttonView, isChecked ->
                Config.loadOnBootEnabled = isChecked
            }


            buttonStart.setOnClickListener {
                MyForegroundService.start(context)
            }

            buttonRefresh.setOnClickListener {
                MyForegroundService.refresh(context)
            }

            buttonStop.setOnClickListener {
                MyForegroundService.stop(context)
            }

            Config.onCreateTimeLiveData.observe(this@MainActivity) { time ->
                onCreate.text = "onCreate time: $time"
            }


            Config.onDestroyTimeLiveData.observe(this@MainActivity) { time ->
                onDestroy.text = "onDestroy time: $time"
            }


        }

    }
}
