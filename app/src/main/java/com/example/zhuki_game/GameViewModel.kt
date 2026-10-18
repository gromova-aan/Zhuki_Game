package com.example.zhuki_game

import android.os.SystemClock
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

class GameViewModel(
    private val settingsRepository: SettingsRepository,
    private val userRepository: UserRepository,
    private val goldRateRepository: GoldRateRepository
) : ViewModel() {

    companion object {
        const val FRAME_MS = 16L
        const val INSECT_SIZE_DP = 60
        const val POINTS_PER_KILL = 1
        const val MISS_PENALTY = 1
        const val RESPAWN_INTERVAL_MS = 700L
        const val SPEED_MIN_DP = 10f
        const val SPEED_MAX_DP = 56f
        const val SPEED_GROWTH = 1.5f
        const val SPEED_AFTER_LEVEL_DP = 12f
        const val BONUS_SIZE_DP = 64
        const val TILT_MODE_DURATION_MS = 10_000L
        const val TILT_ACCEL_FACTOR = 18f
        const val MAX_SPEED_DP = 260f
        const val GOLD_SPAWN_INTERVAL_MS = 20_000L
        const val GOLD_SIZE_DP = 70
        const val GOLD_POINTS_DIVISOR = 1000
        const val DEFAULT_GOLD_RATE = 11000.0
    }

    enum class GameState { READY, PLAYING, FINISHED }

    data class InsectState(
        val id: Long,
        val drawableRes: Int,
        val golden: Boolean,
        var xDp: Float,
        var yDp: Float,
        var vxDp: Float,
        var vyDp: Float,
        var flipX: Boolean = false
    )

    data class BonusState(
        var xDp: Float,
        var yDp: Float,
        var vxDp: Float,
        var vyDp: Float
    )

    private val _state = MutableLiveData(GameState.READY)
    val state: LiveData<GameState> = _state

    private val _score = MutableLiveData(0)
    val score: LiveData<Int> = _score

    private val _timeLeft = MutableLiveData(0)
    val timeLeft: LiveData<Int> = _timeLeft

    private val _recordResult = MutableLiveData<RecordSaveResult?>()
    val recordResult: LiveData<RecordSaveResult?> = _recordResult

    private val insectDrawables = intArrayOf(
        R.drawable.bug_1,
        R.drawable.bug_2,
        R.drawable.bug_3,
        R.drawable.bug_4,
        R.drawable.bug_5
    )

    val insects = mutableListOf<InsectState>()
    var bonus: BonusState? = null
        private set

    var settings: GameSettings = GameSettings.DEFAULTS
        private set

    var goldRate: Double = DEFAULT_GOLD_RATE
        private set

    private var fieldWidthDp = 0f
    private var fieldHeightDp = 0f

    var gravityX = 0f
    var gravityY = 0f

    private var startMillis = 0L
    private var lastRespawnUptime = 0L
    private var lastGoldSpawnUptime = 0L
    private var bonusNextSec = 0f
    private var tiltModeUntil = 0L
    private var nextInsectId = 1L

    val isRunning: Boolean
        get() = _state.value == GameState.PLAYING

    val tiltActive: Boolean
        get() = isRunning && SystemClock.uptimeMillis() < tiltModeUntil

    fun updateFieldSize(widthDp: Float, heightDp: Float) {
        fieldWidthDp = widthDp
        fieldHeightDp = heightDp
    }

    fun startGame() {
        settings = settingsRepository.load()
        insects.clear()
        bonus = null
        _score.value = 0
        _timeLeft.value = settings.roundDuration
        _recordResult.value = null
        startMillis = SystemClock.uptimeMillis()
        lastRespawnUptime = 0L
        lastGoldSpawnUptime = SystemClock.uptimeMillis()
        bonusNextSec = settings.bonusInterval.toFloat()
        tiltModeUntil = 0L
        _state.value = GameState.PLAYING
        loadGoldRate()
        repeat(settings.maxRoaches) { spawnInsect() }
    }

    fun resetGame() {
        insects.clear()
        bonus = null
        _score.value = 0
        _timeLeft.value = 0
        tiltModeUntil = 0L
        _state.value = GameState.READY
    }

    fun endGame() {
        if (_state.value == GameState.PLAYING) {
            finish()
        }
    }

    private fun finish() {
        _state.value = GameState.FINISHED
        insects.clear()
        bonus = null
        _timeLeft.value = 0
        saveScore()
    }

    fun tick(nowMillis: Long, deltaSec: Float) {
        if (!isRunning) return

        val elapsedSec = (nowMillis - startMillis) / 1000f
        if (elapsedSec >= settings.roundDuration) {
            finish()
            return
        }

        _timeLeft.value = (settings.roundDuration - elapsedSec).toInt()

        stepInsects(deltaSec) //двигаем всех жуков и бонус

        val normalCount = insects.count { !it.golden }
        if (nowMillis - lastRespawnUptime >= RESPAWN_INTERVAL_MS && normalCount < settings.maxRoaches) {
            lastRespawnUptime = nowMillis
            spawnInsect()
        }

        if (bonus == null && elapsedSec >= bonusNextSec) {
            bonusNextSec = elapsedSec + settings.bonusInterval
            spawnBonus()
        }

        if (nowMillis - lastGoldSpawnUptime >= GOLD_SPAWN_INTERVAL_MS) {
            lastGoldSpawnUptime = nowMillis
            spawnInsect(golden = true)
        }
    }

    fun onInsectClicked(id: Long) {
        if (!isRunning) return
        val insect = insects.firstOrNull { it.id == id } ?: return
        insects.remove(insect)
        _score.value = (_score.value ?: 0) + if (insect.golden) goldPoints() else POINTS_PER_KILL
    }

    fun applyMiss() {
        if (!isRunning) return
        _score.value = ((_score.value ?: 0) - MISS_PENALTY).coerceAtLeast(0)
    }

    fun onBonusClicked() {
        if (!isRunning) return
        bonus = null
        tiltModeUntil = SystemClock.uptimeMillis() + TILT_MODE_DURATION_MS
        insects.forEach {
            it.vxDp = 0f
            it.vyDp = 0f
        }
    }

    private fun spawnInsect(golden: Boolean = false) {
        val sizeDp = if (golden) GOLD_SIZE_DP else INSECT_SIZE_DP
        val drawable = insectDrawables[Random.nextInt(insectDrawables.size)]

        val maxX = (fieldWidthDp - sizeDp).coerceAtLeast(0f)
        val maxY = (fieldHeightDp - sizeDp).coerceAtLeast(0f)

        val angle = Random.nextFloat() * 2 * Math.PI
        val speedDp = currentSpeedDp()

        insects.add(
            InsectState(
                id = nextInsectId++,
                drawableRes = drawable,
                golden = golden,
                xDp = Random.nextFloat() * maxX,
                yDp = Random.nextFloat() * maxY,
                vxDp = (cos(angle) * speedDp).toFloat(),
                vyDp = (sin(angle) * speedDp).toFloat()
            )
        )
    }

    private fun spawnBonus() {
        val maxX = (fieldWidthDp - BONUS_SIZE_DP).coerceAtLeast(0f)
        val maxY = (fieldHeightDp - BONUS_SIZE_DP).coerceAtLeast(0f)

        val angle = Random.nextFloat() * 2 * Math.PI
        val speedDp = currentSpeedDp()

        bonus = BonusState(
            xDp = Random.nextFloat() * maxX,
            yDp = Random.nextFloat() * maxY,
            vxDp = (cos(angle) * speedDp).toFloat(),
            vyDp = (sin(angle) * speedDp).toFloat()
        )
    }

    private fun currentSpeedDp(): Float {
        val t = (settings.speed - 1).toFloat()
        var speedDp = SPEED_MAX_DP - (SPEED_MAX_DP - SPEED_MIN_DP) * exp(-SPEED_GROWTH * t)
        if (settings.speed > 1) {
            speedDp *= 1.5f
        }
        if (settings.speed > 5) {
            speedDp += (settings.speed - 5) * SPEED_AFTER_LEVEL_DP
        }
        return speedDp
    }

    //движение
    private fun stepInsects(deltaSec: Float) {
        val fieldW = fieldWidthDp
        val fieldH = fieldHeightDp
        val tilt = tiltActive

        for (insect in insects) {
            if (tilt) {
                //прибавляем к горизонтальной и вертикальной скорости ускорение от гравитации
                insect.vxDp += gravityX * TILT_ACCEL_FACTOR * deltaSec
                insect.vyDp += gravityY * TILT_ACCEL_FACTOR * deltaSec
                //ограничиваем скорость от бесконечного разгона
                val s2 = insect.vxDp * insect.vxDp + insect.vyDp * insect.vyDp
                if (s2 > MAX_SPEED_DP * MAX_SPEED_DP) {
                    val scale = MAX_SPEED_DP / sqrt(s2)
                    insect.vxDp *= scale
                    insect.vyDp *= scale
                }
            }

            //двигаем: позиция += скорость × время
            insect.xDp += insect.vxDp * deltaSec
            insect.yDp += insect.vyDp * deltaSec

            val size = if (insect.golden) GOLD_SIZE_DP.toFloat() else INSECT_SIZE_DP.toFloat()

            //от левой стенки
            if (insect.xDp < 0f) {
                insect.xDp = 0f
                if (tilt) {
                    insect.vxDp = 0f
                } else {
                    insect.vxDp = abs(insect.vxDp) //полож скорость
                    insect.flipX = false
                }
                //правый край жука ушёл за поле
            } else if (insect.xDp + size > fieldW) {
                insect.xDp = (fieldW - size).coerceAtLeast(0f)
                if (tilt) {
                    insect.vxDp = 0f
                } else {
                    insect.vxDp = -abs(insect.vxDp)
                    insect.flipX = true
                }
            }

            //от верхней стенки
            if (insect.yDp < 0f) {
                insect.yDp = 0f
                if (tilt) {
                    insect.vyDp = 0f
                } else {
                    insect.vyDp = abs(insect.vyDp)
                }
            } else if (insect.yDp + size > fieldH) {
                insect.yDp = (fieldH - size).coerceAtLeast(0f)
                if (tilt) {
                    insect.vyDp = 0f
                } else {
                    insect.vyDp = -abs(insect.vyDp)
                }
            }
        }

        //движение бонуса
        val b = bonus ?: return
        b.xDp += b.vxDp * deltaSec
        b.yDp += b.vyDp * deltaSec

        val size = BONUS_SIZE_DP.toFloat()

        if (b.xDp < 0f) {
            b.xDp = 0f
            b.vxDp = abs(b.vxDp)
        } else if (b.xDp + size > fieldW) {
            b.xDp = (fieldW - size).coerceAtLeast(0f)
            b.vxDp = -abs(b.vxDp)
        }

        if (b.yDp < 0f) {
            b.yDp = 0f
            b.vyDp = abs(b.vyDp)
        } else if (b.yDp + size > fieldH) {
            b.yDp = (fieldH - size).coerceAtLeast(0f)
            b.vyDp = -abs(b.vyDp)
        }
    }

    fun goldPoints(): Int =
        (goldRate / GOLD_POINTS_DIVISOR).roundToInt().coerceAtLeast(1)

    private fun loadGoldRate() {
        goldRateRepository.cachedRate()?.let { goldRate = it }
        viewModelScope.launch {
            val rate = goldRateRepository.fetchGoldRate()
            if (rate != null) goldRate = rate
        }
    }

    private fun saveScore() {
        val finalScore = _score.value ?: 0
        viewModelScope.launch {
            _recordResult.value = userRepository.saveRecord(finalScore)
        }
    }

    fun consumeRecordResult() {
        _recordResult.value = null
    }

    private fun abs(value: Float): Float = kotlin.math.abs(value)
}
