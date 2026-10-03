package com.example.zhuki_game

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

class GameFragment : Fragment() {

    private companion object {
        const val FRAME_MS = 16L //16мс на кадр
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
        val GOLD_COLOR: Int = 0xFFFFD700.toInt()
    }

    private data class Insect(
        val view: ImageView,
        var vx: Float,
        var vy: Float,
        val golden: Boolean = false
    )

    private val insectDrawables = intArrayOf(
        R.drawable.bug_1,
        R.drawable.bug_2,
        R.drawable.bug_3,
        R.drawable.bug_4,
        R.drawable.bug_5
    )

    private lateinit var repository: SettingsRepository

    private lateinit var field: FrameLayout
    private lateinit var tvScore: TextView
    private lateinit var tvTimer: TextView
    private lateinit var tvMessage: TextView
    private lateinit var btnStart: Button

    private val handler = Handler(Looper.getMainLooper())
    private val insects = mutableListOf<Insect>()

    private var settings = GameSettings.DEFAULTS
    private var score = 0
    private var timeLeftSec = 0
    private var startMillis = 0L
    private var lastRespawn = 0L
    private var lastFrameTime = 0L
    private var gameRunning = false

    private var bonusView: ImageView? = null
    private var bonusSpawned = false
    private var bonusNextSec = 0f
    private var bonusVx = 0f
    private var bonusVy = 0f
    private var tiltModeUntil = 0L
    private var gravityX = 0f
    private var gravityY = 0f
    private var sensorManager: SensorManager? = null
    private var gravitySensor: Sensor? = null
    private var screamPlayer: MediaPlayer? = null
    private var lastGoldSpawn = 0L
    private var goldRate: Double = DEFAULT_GOLD_RATE

    private val density: Float
        get() = resources.displayMetrics.density

    private val tiltActive: Boolean
        get() = gameRunning && SystemClock.uptimeMillis() < tiltModeUntil

    private val sensorListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            if (event.sensor.type == Sensor.TYPE_GRAVITY) {
                gravityX = event.values[0]
                gravityY = event.values[1]
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    private val gameLoop = object : Runnable {
        override fun run() {
            if (!gameRunning) return

            val now = SystemClock.uptimeMillis()
            val elapsedSec = (now - startMillis) / 1000f

            if (elapsedSec >= settings.roundDuration) {
                endGame()
                return
            }

            val deltaSec = if (lastFrameTime == 0L) 0f else (now - lastFrameTime) / 1000f
            lastFrameTime = now

            timeLeftSec = (settings.roundDuration - elapsedSec).toInt()
            stepInsects(deltaSec)

            val normalCount = insects.count { !it.golden }
            if (now - lastRespawn >= RESPAWN_INTERVAL_MS && normalCount < settings.maxRoaches) {
                lastRespawn = now
                spawnInsect()
            }

            if (!bonusSpawned && elapsedSec >= bonusNextSec) {
                bonusNextSec = elapsedSec + settings.bonusInterval
                spawnBonus()
            }

            if (now - lastGoldSpawn >= GOLD_SPAWN_INTERVAL_MS) {
                lastGoldSpawn = now
                spawnInsect(golden = true)
            }

            if (!tiltActive) {
                stopScream()
            }

            updateHud()
            handler.postDelayed(this, FRAME_MS)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_game, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        repository = SettingsRepository(SharedPreferencesStorage(requireContext()))

        field = view.findViewById(R.id.gameField)
        tvScore = view.findViewById(R.id.tvScore)
        tvTimer = view.findViewById(R.id.tvTimer)
        tvMessage = view.findViewById(R.id.tvMessage)
        btnStart = view.findViewById(R.id.btnStart)

        field.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN && gameRunning) {
                applyMiss()
            }
            true
        }

        btnStart.setOnClickListener {
            if (gameRunning) {
                resetGame()
            } else {
                startGame()
            }
        }

        showReadyState()
    }

    override fun onResume() {
        super.onResume()
        sensorManager = requireContext().getSystemService(Context.SENSOR_SERVICE) as SensorManager?
        gravitySensor = sensorManager?.getDefaultSensor(Sensor.TYPE_GRAVITY)
        gravitySensor?.let {
            sensorManager?.registerListener(sensorListener, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    override fun onPause() {
        super.onPause()
        stopScream()
        sensorManager?.unregisterListener(sensorListener)
        if (gameRunning) {
            endGame()
        }
    }

    private fun startGame() {
        settings = repository.load()
        score = 0
        timeLeftSec = settings.roundDuration
        startMillis = SystemClock.uptimeMillis()
        lastRespawn = 0L
        lastFrameTime = 0L
        lastGoldSpawn = SystemClock.uptimeMillis()
        loadGoldRate()
        removeBonus()
        bonusNextSec = settings.bonusInterval.toFloat()
        tiltModeUntil = 0L
        stopScream()
        gameRunning = true
        btnStart.text = getString(R.string.game_reset)
        tvMessage.text = getString(R.string.game_message_play)

        clearInsects()
        repeat(settings.maxRoaches) { spawnInsect() }

        handler.removeCallbacks(gameLoop)
        handler.post(gameLoop)
        updateHud()
    }

    private fun spawnInsect(golden: Boolean = false) {
        val sizePx = ((if (golden) GOLD_SIZE_DP else INSECT_SIZE_DP) * density).toInt()
        val drawable = insectDrawables[Random.nextInt(insectDrawables.size)]

        val insectView = ImageView(requireContext()).apply {
            setImageResource(drawable)
            scaleType = ImageView.ScaleType.FIT_CENTER
            contentDescription = getString(
                if (golden) R.string.game_golden_desc else R.string.game_insect_desc
            )
            layoutParams = FrameLayout.LayoutParams(sizePx, sizePx)
            if (golden) setColorFilter(GOLD_COLOR)
        }

        val maxX = (field.width - sizePx).coerceAtLeast(0)
        val maxY = (field.height - sizePx).coerceAtLeast(0)

        val insect = Insect(
            view = insectView,
            vx = 0f,
            vy = 0f,
            golden = golden
        )

        val angle = Random.nextFloat() * 2 * Math.PI
        val speedPx = currentSpeedDp() * density
        insect.vx = (cos(angle) * speedPx).toFloat()
        insect.vy = (sin(angle) * speedPx).toFloat()

        insects.add(insect)
        insectView.setOnClickListener { onInsectClicked(insect) }
        field.addView(insectView)
        insectView.translationX = Random.nextFloat() * maxX
        insectView.translationY = Random.nextFloat() * maxY
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

    private fun spawnBonus() {
        val sizePx = (BONUS_SIZE_DP * density).toInt()
        val bonus = ImageView(requireContext()).apply {
            setImageResource(R.drawable.ic_bonus)
            scaleType = ImageView.ScaleType.FIT_CENTER
            contentDescription = getString(R.string.game_bonus_desc)
            layoutParams = FrameLayout.LayoutParams(sizePx, sizePx)
        }

        val maxX = (field.width - sizePx).coerceAtLeast(0)
        val maxY = (field.height - sizePx).coerceAtLeast(0)

        val angle = Random.nextFloat() * 2 * Math.PI
        val speedPx = currentSpeedDp() * density
        bonusVx = (cos(angle) * speedPx).toFloat()
        bonusVy = (sin(angle) * speedPx).toFloat()

        bonus.setOnClickListener { onBonusClicked() }
        field.addView(bonus)
        bonusView = bonus
        bonusSpawned = true
        bonus.translationX = Random.nextFloat() * maxX
        bonus.translationY = Random.nextFloat() * maxY
    }

    private fun onBonusClicked() {
        if (!gameRunning) return
        bonusView?.let { field.removeView(it) }
        bonusView = null
        bonusSpawned = false
        tiltModeUntil = SystemClock.uptimeMillis() + TILT_MODE_DURATION_MS
        insects.forEach {
            it.vx = 0f
            it.vy = 0f
        }
        startScream()
    }

    private fun removeBonus() {
        bonusView?.let { field.removeView(it) }
        bonusView = null
        bonusSpawned = false
    }

    private fun startScream() {
        if (screamPlayer == null) {
            screamPlayer = MediaPlayer.create(requireContext(), R.raw.bug_scream)?.apply {
                isLooping = true
                setVolume(0.7f, 0.7f)
                start()
            }
        }
    }

    private fun stopScream() {
        screamPlayer?.let {
            if (it.isPlaying) it.stop()
            it.release()
        }
        screamPlayer = null
    }

    private fun stepInsects(deltaSec: Float) {
        val fieldW = field.width
        val fieldH = field.height

        val iterator = insects.iterator()
        while (iterator.hasNext()) {
            val insect = iterator.next()

            if (tiltActive) {
                insect.vx += gravityX * TILT_ACCEL_FACTOR * deltaSec
                insect.vy += gravityY * TILT_ACCEL_FACTOR * deltaSec
                val maxV = MAX_SPEED_DP * density
                val maxV2 = maxV * maxV
                val s2 = insect.vx * insect.vx + insect.vy * insect.vy
                if (s2 > maxV2) {
                    val scale = maxV / kotlin.math.sqrt(s2)
                    insect.vx *= scale
                    insect.vy *= scale
                }
            }

            insect.view.translationX += insect.vx * deltaSec
            insect.view.translationY += insect.vy * deltaSec

            val w = insect.view.width.toFloat()
            val h = insect.view.height.toFloat()

            if (insect.view.translationX < 0f) {
                insect.view.translationX = 0f
                if (tiltActive) {
                    insect.vx = 0f
                } else {
                    insect.vx = abs(insect.vx)
                    insect.view.scaleX = 1f
                }
            } else if (insect.view.translationX + w > fieldW) {
                insect.view.translationX = (fieldW - w).coerceAtLeast(0f)
                if (tiltActive) {
                    insect.vx = 0f
                } else {
                    insect.vx = -abs(insect.vx)
                    insect.view.scaleX = -1f
                }
            }

            if (insect.view.translationY < 0f) {
                insect.view.translationY = 0f
                if (tiltActive) {
                    insect.vy = 0f
                } else {
                    insect.vy = abs(insect.vy)
                }
            } else if (insect.view.translationY + h > fieldH) {
                insect.view.translationY = (fieldH - h).coerceAtLeast(0f)
                if (tiltActive) {
                    insect.vy = 0f
                } else {
                    insect.vy = -abs(insect.vy)
                }
            }
        }

        val bonus = bonusView
        if (bonus != null) {
            bonus.translationX += bonusVx * deltaSec
            bonus.translationY += bonusVy * deltaSec

            val w = bonus.width.toFloat()
            val h = bonus.height.toFloat()

            if (bonus.translationX < 0f) {
                bonus.translationX = 0f
                bonusVx = abs(bonusVx)
            } else if (bonus.translationX + w > fieldW) {
                bonus.translationX = (fieldW - w).coerceAtLeast(0f)
                bonusVx = -abs(bonusVx)
            }

            if (bonus.translationY < 0f) {
                bonus.translationY = 0f
                bonusVy = abs(bonusVy)
            } else if (bonus.translationY + h > fieldH) {
                bonus.translationY = (fieldH - h).coerceAtLeast(0f)
                bonusVy = -abs(bonusVy)
            }
        }
    }

    private fun onInsectClicked(insect: Insect) {
        if (!gameRunning || field.indexOfChild(insect.view) < 0) return

        field.removeView(insect.view)
        insects.remove(insect)
        score += if (insect.golden) goldPoints() else POINTS_PER_KILL
        updateHud()
    }

    private fun applyMiss() {
        if (!gameRunning) return
        score = (score - MISS_PENALTY).coerceAtLeast(0)
        updateHud()
    }

    private fun endGame() {
        gameRunning = false
        handler.removeCallbacks(gameLoop)
        clearInsects()
        removeBonus()
        stopScream()
        tvTimer.text = "0"
        tvMessage.text = getString(R.string.game_over_score, score)
        btnStart.isEnabled = true
        btnStart.text = getString(R.string.game_restart)
        updateHud()
        saveScore()
    }

    private fun saveScore() {
        val finalScore = score
        viewLifecycleOwner.lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                UserRepository(requireContext().applicationContext)
                    .saveRecord(finalScore)
            }
            if (!isAdded) return@launch
            val message = when (result) {
                is RecordSaveResult.NoUser -> getString(R.string.game_no_user)
                is RecordSaveResult.Created ->
                    getString(R.string.game_record_saved, result.score)
                is RecordSaveResult.Beaten ->
                    getString(R.string.game_record_beaten, result.score)
                is RecordSaveResult.Kept ->
                    getString(R.string.game_record_kept, result.bestScore)
            }
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun resetGame() {
        gameRunning = false
        handler.removeCallbacks(gameLoop)
        clearInsects()
        removeBonus()
        stopScream()
        showReadyState()
    }

    private fun showReadyState() {
        tvMessage.text = getString(R.string.game_message_ready)
        btnStart.text = getString(R.string.game_start)
        btnStart.isEnabled = true
        tvScore.text = "0"
        tvTimer.text = "0"
    }

    private fun updateHud() {
        tvScore.text = score.toString()
        tvTimer.text = timeLeftSec.coerceAtLeast(0).toString()
    }

    private fun clearInsects() {
        insects.forEach { field.removeView(it.view) }
        insects.clear()
    }

    private fun goldPoints(): Int =
        (goldRate / GOLD_POINTS_DIVISOR).roundToInt().coerceAtLeast(1)

    private fun loadGoldRate() {
        val repository = GoldRateRepository.getInstance(requireContext().applicationContext)
        repository.cachedRate()?.let { goldRate = it }
        viewLifecycleOwner.lifecycleScope.launch {
            val rate = repository.fetchGoldRate()
            if (rate != null) goldRate = rate
        }
    }

    private fun abs(value: Float): Float = kotlin.math.abs(value)
}