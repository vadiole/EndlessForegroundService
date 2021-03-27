package vadiole.foregroundservisetest.preferences

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.lifecycle.LiveData
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset.UTC


@SuppressLint("StaticFieldLeak")
object Config {
    private const val SHARED_PREFERENCES_NAME = "preferences"

    //  region preferences operators
    private lateinit var preferences: SharedPreferences
    private lateinit var context: Context

    /**
     * call on start up to init preferences
     */
    @Suppress("DEPRECATION")
    fun init(context: Context) {
        preferences = context.applicationContext.getSharedPreferences(
            SHARED_PREFERENCES_NAME,
            Context.MODE_PRIVATE
        )
        Config.context = context
    }

    /**
     * puts a value for the given [key].
     */
    operator fun SharedPreferences.set(key: String, value: Any?) = when (value) {
        is String? -> edit { putString(key, value) }
        is Int -> edit { putInt(key, value) }
        is Boolean -> edit { putBoolean(key, value) }
        is Float -> edit { putFloat(key, value) }
        is Long -> edit { putLong(key, value) }
        is LocalTime -> edit { putInt(key, value.toSecondOfDay()) }
        is LocalDateTime -> edit {
            putLong(key, value.toEpochSecond(UTC))
        }
        else -> throw UnsupportedOperationException("Not yet implemented")
    }

    /**
     * finds a preference based on the given [key].
     * [T] is the type of value
     * @param default optional defaultValue - will take a default defaultValue if it is not specified
     */
    inline operator fun <reified T : Any> SharedPreferences.get(
        key: String,
        default: T? = null,
    ): T = when (T::class) {
        String::class -> getString(key, default as? String ?: "") as T
        Int::class -> getInt(key, default as? Int ?: -1) as T
        Boolean::class -> getBoolean(key, default as? Boolean ?: false) as T
        Float::class -> getFloat(key, default as? Float ?: -1f) as T
        Long::class -> getLong(key, default as? Long ?: -1) as T
        LocalTime::class -> {
            LocalTime.ofSecondOfDay(
                getInt(
                    key,
                    (default as? LocalTime)?.toSecondOfDay() ?: 0
                ).toLong()
            ) as T
        }
        LocalDateTime::class -> {
            val defaultSeconds =
                (default as? LocalDateTime)?.toEpochSecond(UTC) ?: 0L

            val seconds = getLong(key, defaultSeconds)
            (if (seconds == 0L) LocalDateTime.MIN else LocalDateTime.ofEpochSecond(
                seconds, 0, UTC
            )) as T
        }


        else -> throw UnsupportedOperationException("Not yet implemented")
    }


    private inline fun <reified T> SharedPreferences.liveData(
        key: String,
        crossinline getter: () -> T,
        notifyInitialValue: Boolean = true,
    ): LiveData<T> {
        return object : PreferenceLiveData<T>(
            this,
            key,
            notifyInitialValue,
        ) {
            override fun getPreferencesValue(): T = getter.invoke()
        }
    }


    // endregion


//    private const val KEY = "preferenceKey"
//    var template: String
//        get() = preferences[KEY, null]
//        set(value) = preferences.set(KEY, value)

    private const val LOAD_ON_BOOT = "loadOnBoot"
    var loadOnBootEnabled: Boolean
        get() = preferences[LOAD_ON_BOOT, true]
        set(value) = preferences.set(LOAD_ON_BOOT, value)

    private const val ON_DESTROY_KEY = "onDestroyTime"
    val onDestroyTimeLiveData by lazy {
        preferences.liveData(ON_DESTROY_KEY, { onDestroyTime })
    }
    var onDestroyTime: LocalDateTime?
        get() = preferences[ON_DESTROY_KEY, LocalDateTime.MIN]
        set(value) = preferences.set(ON_DESTROY_KEY, value)

    private const val ON_CREATE_KEY = "onCreateTime"
    val onCreateTimeLiveData by lazy {
        preferences.liveData(ON_CREATE_KEY, { onCreateTime })
    }
    var onCreateTime: LocalDateTime?
        get() = preferences[ON_CREATE_KEY, LocalDateTime.MIN]
        set(value) = preferences.set(ON_CREATE_KEY, value)
}
