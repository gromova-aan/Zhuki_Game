package com.example.zhuki_game

import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsRepositoryTest {

    private class InMemoryStorage : SettingsStorage {
        private val values = HashMap<String, Int>()
        override fun getInt(key: String, defaultValue: Int): Int = values[key] ?: defaultValue
        override fun putInt(key: String, value: Int) {
            values[key] = value
        }
    }

    @Test
    fun load_returnsDefaultsWhenStorageIsEmpty() {
        val settings = SettingsRepository(InMemoryStorage()).load()
        assertEquals(GameSettings.DEFAULTS, settings)
    }

    @Test
    fun saveThenLoad_returnsSavedSettings() {
        val storage = InMemoryStorage()
        val repository = SettingsRepository(storage)
        val custom = GameSettings(speed = 3, maxRoaches = 7, bonusInterval = 15, roundDuration = 120)

        repository.save(custom)
        val loaded = repository.load()

        assertEquals(custom, loaded)
    }

    @Test
    fun load_returnsDefaultsWhenStoredValuesAreInvalid() {
        val storage = InMemoryStorage()
        storage.putInt(SettingsRepository.KEY_SPEED, 999)
        storage.putInt(SettingsRepository.KEY_MAX_ROACHES, -5)
        storage.putInt(SettingsRepository.KEY_BONUS_INTERVAL, 0)
        storage.putInt(SettingsRepository.KEY_ROUND_DURATION, 1)

        val loaded = SettingsRepository(storage).load()

        assertEquals(GameSettings.DEFAULTS, loaded)
    }

    @Test
    fun resetToDefaults_overwritesStoredValues() {
        val storage = InMemoryStorage()
        val repository = SettingsRepository(storage)
        repository.save(GameSettings(speed = 9, maxRoaches = 19, bonusInterval = 29, roundDuration = 299))

        val afterReset = repository.resetToDefaults()

        assertEquals(GameSettings.DEFAULTS, afterReset)
        assertEquals(GameSettings.DEFAULTS, repository.load())
    }

    @Test
    fun load_keepsValidValuesSavedEarlier() {
        val storage = InMemoryStorage()
        storage.putInt(SettingsRepository.KEY_SPEED, 4)
        storage.putInt(SettingsRepository.KEY_MAX_ROACHES, 8)
        storage.putInt(SettingsRepository.KEY_BONUS_INTERVAL, 12)
        storage.putInt(SettingsRepository.KEY_ROUND_DURATION, 90)

        val loaded = SettingsRepository(storage).load()

        assertEquals(GameSettings(speed = 4, maxRoaches = 8, bonusInterval = 12, roundDuration = 90), loaded)
    }
}