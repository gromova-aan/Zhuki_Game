package com.example.zhuki_game

data class GameSettings(
    val speed: Int,
    val maxRoaches: Int,
    val bonusInterval: Int,
    val roundDuration: Int
) {

    fun errors(): List<String> {
        val result = mutableListOf<String>()
        if (speed !in MIN_SPEED..MAX_SPEED) {
            result.add("speed must be in $MIN_SPEED..$MAX_SPEED, got $speed")
        }
        if (maxRoaches !in MIN_ROACHES..MAX_ROACHES) {
            result.add("maxRoaches must be in $MIN_ROACHES..$MAX_ROACHES, got $maxRoaches")
        }
        if (bonusInterval !in MIN_BONUS_INTERVAL..MAX_BONUS_INTERVAL) {
            result.add("bonusInterval must be in $MIN_BONUS_INTERVAL..$MAX_BONUS_INTERVAL, got $bonusInterval")
        }
        if (roundDuration !in MIN_ROUND_DURATION..MAX_ROUND_DURATION) {
            result.add("roundDuration must be in $MIN_ROUND_DURATION..$MAX_ROUND_DURATION, got $roundDuration")
        }
        return result
    }

    fun isValid(): Boolean = errors().isEmpty()

    companion object {
        const val MIN_SPEED = 1
        const val MAX_SPEED = 10

        const val MIN_ROACHES = 1
        const val MAX_ROACHES = 20

        const val MIN_BONUS_INTERVAL = 1
        const val MAX_BONUS_INTERVAL = 30

        const val MIN_ROUND_DURATION = 5
        const val MAX_ROUND_DURATION = 300

        val DEFAULTS = GameSettings(
            speed = 5,
            maxRoaches = 10,
            bonusInterval = 10,
            roundDuration = 60
        )
    }
}