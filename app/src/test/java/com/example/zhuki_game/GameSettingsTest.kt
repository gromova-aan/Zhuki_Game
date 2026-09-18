package com.example.zhuki_game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GameSettingsTest {

    private fun defaultSettings(
        speed: Int = GameSettings.DEFAULTS.speed,
        maxRoaches: Int = GameSettings.DEFAULTS.maxRoaches,
        bonusInterval: Int = GameSettings.DEFAULTS.bonusInterval,
        roundDuration: Int = GameSettings.DEFAULTS.roundDuration
    ) = GameSettings(speed, maxRoaches, bonusInterval, roundDuration)

    @Test
    fun defaults_areValid() {
        assertTrue(GameSettings.DEFAULTS.isValid())
        assertEquals(0, GameSettings.DEFAULTS.errors().size)
    }

    @Test
    fun speed_mustBeInRange1to10() {
        assertTrue(defaultSettings(speed = 1).isValid())
        assertTrue(defaultSettings(speed = 10).isValid())
        assertFalse(defaultSettings(speed = 0).isValid())
        assertFalse(defaultSettings(speed = 11).isValid())
    }

    @Test
    fun maxRoaches_mustBeInRange1to20() {
        assertTrue(defaultSettings(maxRoaches = 1).isValid())
        assertTrue(defaultSettings(maxRoaches = 20).isValid())
        assertFalse(defaultSettings(maxRoaches = 0).isValid())
        assertFalse(defaultSettings(maxRoaches = 21).isValid())
    }

    @Test
    fun bonusInterval_mustBeInRange1to30() {
        assertTrue(defaultSettings(bonusInterval = 1).isValid())
        assertTrue(defaultSettings(bonusInterval = 30).isValid())
        assertFalse(defaultSettings(bonusInterval = 0).isValid())
        assertFalse(defaultSettings(bonusInterval = 31).isValid())
    }

    @Test
    fun roundDuration_mustBeInRange5to300() {
        assertTrue(defaultSettings(roundDuration = 5).isValid())
        assertTrue(defaultSettings(roundDuration = 300).isValid())
        assertFalse(defaultSettings(roundDuration = 4).isValid())
        assertFalse(defaultSettings(roundDuration = 301).isValid())
    }

    @Test
    fun errors_containsMessagesForEachInvalidField() {
        val invalid = GameSettings(speed = 99, maxRoaches = -1, bonusInterval = 0, roundDuration = 0)
        val errors = invalid.errors()
        assertEquals(4, errors.size)
        assertTrue(errors.any { it.contains("speed") })
        assertTrue(errors.any { it.contains("maxRoaches") })
        assertTrue(errors.any { it.contains("bonusInterval") })
        assertTrue(errors.any { it.contains("roundDuration") })
    }

    @Test
    fun isValid_falseWhenAnyFieldOutOfRange() {
        assertFalse(defaultSettings(speed = 0).isValid())
        assertFalse(defaultSettings(maxRoaches = 100).isValid())
        assertFalse(defaultSettings(bonusInterval = 500).isValid())
        assertFalse(defaultSettings(roundDuration = -10).isValid())
    }
}