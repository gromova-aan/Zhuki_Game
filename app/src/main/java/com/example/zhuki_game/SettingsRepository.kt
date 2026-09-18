package com.example.zhuki_game

import android.content.Context
import android.content.SharedPreferences

interface SettingsStorage {
    fun getInt(key: String, defaultValue: Int): Int
    fun putInt(key: String, value: Int)
}

class SharedPreferencesStorage(context: Context) : SettingsStorage {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun getInt(key: String, defaultValue: Int): Int =
        prefs.getInt(key, defaultValue)

    override fun putInt(key: String, value: Int) {
        prefs.edit().putInt(key, value).apply()
    }

    companion object {
        const val PREFS_NAME = "game_settings"
    }
}

class SettingsRepository(private val storage: SettingsStorage) {

    fun load(): GameSettings {
        val settings = GameSettings(
            speed = storage.getInt(KEY_SPEED, GameSettings.DEFAULTS.speed),
            maxRoaches = storage.getInt(KEY_MAX_ROACHES, GameSettings.DEFAULTS.maxRoaches),
            bonusInterval = storage.getInt(KEY_BONUS_INTERVAL, GameSettings.DEFAULTS.bonusInterval),
            roundDuration = storage.getInt(KEY_ROUND_DURATION, GameSettings.DEFAULTS.roundDuration)
        )
        return if (settings.isValid()) settings else GameSettings.DEFAULTS
    }

    fun save(settings: GameSettings) {
        storage.putInt(KEY_SPEED, settings.speed)
        storage.putInt(KEY_MAX_ROACHES, settings.maxRoaches)
        storage.putInt(KEY_BONUS_INTERVAL, settings.bonusInterval)
        storage.putInt(KEY_ROUND_DURATION, settings.roundDuration)
    }

    fun resetToDefaults(): GameSettings {
        save(GameSettings.DEFAULTS)
        return GameSettings.DEFAULTS
    }

    companion object {
        const val KEY_SPEED = "speed"
        const val KEY_MAX_ROACHES = "max_roaches"
        const val KEY_BONUS_INTERVAL = "bonus_interval"
        const val KEY_ROUND_DURATION = "round_duration"
    }
}