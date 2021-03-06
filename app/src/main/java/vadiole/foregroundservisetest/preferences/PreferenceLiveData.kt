package vadiole.foregroundservisetest.preferences

import android.content.SharedPreferences
import androidx.lifecycle.LiveData

abstract class PreferenceLiveData<T>(
    private val preferences: SharedPreferences,
    private val key: String,
    private val notifyInitValue: Boolean,
) : LiveData<T>() {

    private val listener = PreferenceListener()

    override fun onActive() {
        super.onActive()
        listener.register()
    }

    override fun onInactive() {
        super.onInactive()
        listener.unregister()
    }

    abstract fun getPreferencesValue(): T

    inner class PreferenceListener : SharedPreferences.OnSharedPreferenceChangeListener {
        private var needCheckWhenRegister = false

        init {
            if (notifyInitValue) {
                value = getPreferencesValue()
            }
        }

        fun register() {
            preferences.registerOnSharedPreferenceChangeListener(listener)
            if (needCheckWhenRegister) {
                needCheckWhenRegister = false
                updateValue(getPreferencesValue())
            }
        }

        fun unregister() {
            preferences.unregisterOnSharedPreferenceChangeListener(listener)
            needCheckWhenRegister = true
        }

        override fun onSharedPreferenceChanged(
            sharedPreferences: SharedPreferences?,
            key: String?,
        ) {
            if (key == this@PreferenceLiveData.key) {
                updateValue(getPreferencesValue())
            }
        }

        private fun updateValue(newValue: T) {
            if (value != newValue) {
                value = newValue
            }
        }
    }
}
