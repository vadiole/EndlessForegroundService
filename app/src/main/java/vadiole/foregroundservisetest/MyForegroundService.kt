package vadiole.foregroundservisetest

import android.annotation.SuppressLint
import android.app.Notification
import android.app.Notification.*
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.NotificationManager.IMPORTANCE_DEFAULT
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.Intent.*
import android.content.IntentFilter
import android.graphics.Color
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.O
import android.os.Build.VERSION_CODES.Q
import android.os.PowerManager
import android.os.PowerManager.PARTIAL_WAKE_LOCK
import androidx.lifecycle.LifecycleService
import kotlinx.coroutines.*
import timber.log.Timber
import java.time.LocalDateTime


class MyForegroundService : LifecycleService() {


    private var wakeLock: PowerManager.WakeLock? = null
    private var isServiceStarted = false
    private var job: Job? = null

    private var refreshCounter: Int = 0
    private var counter: Int = 0


    private val screenReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == ACTION_SCREEN_ON) {
                refresh(context)
            }
        }
    }

    override fun onCreate() {
        Timber.i("onCreate")
        super.onCreate()
        registerReceiver(screenReceiver, IntentFilter(ACTION_SCREEN_ON))

        Repository.setOnCreateTime(LocalDateTime.now())

        val notification = createLoadingNotification()
        startForeground(NOTIFICATION_ID, notification)
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    @SuppressLint("WakelockTimeout")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Timber.i("onStartCommand: action - ${intent?.action ?: "null"}, flags - $flags, startId - $startId")
        super.onStartCommand(intent, flags, startId)

        if (intent != null) {
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            when (intent.action) {
                ACTION_START -> {
                    if (!isServiceStarted) {
                        isServiceStarted = true
                        val powerManager = (getSystemService(Context.POWER_SERVICE) as PowerManager)
                        wakeLock = powerManager.newWakeLock(PARTIAL_WAKE_LOCK, "vadiole:lock")
                        wakeLock?.acquire()

                        Timber.i("onStartCommand: starting")
                        job = GlobalScope.launch(Dispatchers.Default) {
                            delay(1000) //  test delay for show loading notification
                            while (isServiceStarted) {
                                if (powerManager.isInteractive) {
                                    Timber.v("update: counter - $counter")
                                    val initTime = Repository.getOnCreateTime()
                                    val title = "Refresh $refreshCounter times"
                                    val message =
                                        "onCreate time: $initTime\ncurrent time: ${LocalDateTime.now()}\ncounter: ${counter++}"
                                    val notification = createNotification(title, message)
                                    withContext(Dispatchers.Main) {
                                        notificationManager.notify(NOTIFICATION_ID, notification)
                                    }
                                } else {
                                    delay(1000)
                                }
                                delay(1000)
                            }
                        }
                    } else {
                        Timber.e("onStartCommand: service already started")
                    }
                }
                ACTION_REFRESH -> {
                    if (isServiceStarted) {
                        val initTime = Repository.getOnCreateTime()
                        val title = "Refresh ${refreshCounter++} times"
                        val message =
                            "onCreate time: $initTime\ncurrent time: ${LocalDateTime.now()}\ncounter: $counter"

                        val notification = createNotification(title, message)
                        notificationManager.notify(NOTIFICATION_ID, notification)
                    } else {
                        Timber.e("onStartCommand: service not started")
                        stopForeground(true)
                        stopSelf()
                    }
                }
                ACTION_STOP -> {
                    try {
                        wakeLock?.let {
                            if (it.isHeld) {
                                it.release()
                            }
                        }
                        stopForeground(true)
                        stopSelf()
                    } catch (e: Exception) {
                        Timber.e(e)
                    }
                }
            }

        } else {
            Timber.i("onStartCommand: intent == null")
        }

        return START_REDELIVER_INTENT
    }

    override fun onDestroy() {
        Timber.i("onDestroy")
        Repository.setOnDestroyTime(LocalDateTime.now())

        unregisterReceiver(screenReceiver)
        job?.cancel("onDestroy")
        job = null
        super.onDestroy()
    }

    @Suppress("DEPRECATION")
    private fun createNotification(
        title: String,
        message: String,
    ): Notification {

        createChannel()
        val intent = Intent(ACTION_MAIN, null).apply {
            setClass(this@MyForegroundService, MainActivity::class.java)
            flags = FLAG_ACTIVITY_NO_USER_ACTION or
                    FLAG_ACTIVITY_SINGLE_TOP or
                    FLAG_ACTIVITY_NEW_TASK or
                    FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(this, 12365, intent, FLAG_UPDATE_CURRENT)


        val builder: Builder = if (SDK_INT >= O) {
            Builder(this, CHANNEL_ID)
        } else {
            Builder(this)
        }


        return builder.apply {
            style = BigTextStyle().bigText(message)
            setContentIntent(pendingIntent)
            setContentText(message)
            setContentTitle(title)
            setSmallIcon(R.mipmap.ic_launcher)
            setVisibility(VISIBILITY_PUBLIC)
            setCategory(CATEGORY_PROGRESS)
            setPriority(PRIORITY_DEFAULT)
            setColor(Color.DKGRAY)
            setOnlyAlertOnce(true)
            setShowWhen(false)
            setOngoing(true)
            setVibrate(null)
            setSound(null)
            if (SDK_INT >= O) {
                setColorized(true)
            }
        }.build()
    }

    @Suppress("DEPRECATION")
    private fun createLoadingNotification(): Notification {
        createChannel()

        val builder: Builder = if (SDK_INT >= O) {
            Builder(this, CHANNEL_ID)
        } else {
            Builder(this)
        }

        val inboxStyle = InboxStyle().setSummaryText("Loading...")

        return builder.apply {
            style = inboxStyle
            setSmallIcon(R.mipmap.ic_launcher)
            setVisibility(VISIBILITY_PUBLIC)
            setCategory(CATEGORY_PROGRESS)
            setPriority(PRIORITY_DEFAULT)
            setColor(Color.DKGRAY)
            setOnlyAlertOnce(true)
            setShowWhen(false)
            setOngoing(true)
            setVibrate(null)
            setSound(null)
            if (SDK_INT >= O) {
                setColorized(true)
            }
        }.build()

    }

    private fun createChannel() {
        if (SDK_INT >= O) {
            val notificationManager = getSystemService(NotificationManager::class.java)
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, IMPORTANCE_DEFAULT).apply {
                description = "Foreground Service Description"
                setSound(null, null)
                enableVibration(false)
                enableLights(false)
                setShowBadge(false)
                if (SDK_INT >= Q) setAllowBubbles(false)
                lockscreenVisibility = VISIBILITY_PUBLIC
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    companion object {
        private const val ACTION_START = "vadiole.foreground.start"
        private const val ACTION_REFRESH = "vadiole.foreground.refresh"
        private const val ACTION_STOP = "vadiole.foreground.stop"
        private const val CHANNEL_ID = "vadiole.foreground.channel_id"
        private const val CHANNEL_NAME = "Foreground Service"
        private const val NOTIFICATION_ID = 811

        fun start(context: Context) {
            val intent = Intent(context.applicationContext, MyForegroundService::class.java)
            intent.action = ACTION_START
            try {
                if (SDK_INT >= 26) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                Timber.e(e, "exception when call startService")
            }
        }

        fun refresh(context: Context) {
            val intent = Intent(context.applicationContext, MyForegroundService::class.java)
            intent.action = ACTION_REFRESH
            try {
                if (SDK_INT >= 26) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                Timber.e(e, "exception when call startService")
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context.applicationContext, MyForegroundService::class.java)
            intent.action = ACTION_STOP
            try {
                if (SDK_INT >= 26) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                Timber.e(e, "exception when call startService")
            }

        }

    }
}
