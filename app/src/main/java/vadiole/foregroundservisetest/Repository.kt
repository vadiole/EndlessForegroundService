package vadiole.foregroundservisetest

import vadiole.foregroundservisetest.preferences.Config
import java.time.LocalDateTime

object Repository {
    fun getOnCreateTime() = Config.onCreateTime

    fun setOnCreateTime(value: LocalDateTime) {
        Config.onCreateTime = value
    }

    fun getOnDestroyTime() = Config.onDestroyTime

    fun setOnDestroyTime(value: LocalDateTime) {
        Config.onDestroyTime = value
    }
}
