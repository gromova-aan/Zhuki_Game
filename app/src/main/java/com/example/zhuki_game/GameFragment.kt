package com.example.zhuki_game

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
import androidx.fragment.app.Fragment
import kotlin.math.cos
import kotlin.math.exp
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
    }

    private data class Insect(
        val view: ImageView,
        var vx: Float,
        var vy: Float
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

    private val density: Float
        get() = resources.displayMetrics.density

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

            val normalCount = insects.size
            if (now - lastRespawn >= RESPAWN_INTERVAL_MS && normalCount < settings.maxRoaches) {
                lastRespawn = now
                spawnInsect()
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

    override fun onPause() {
        super.onPause()
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
        gameRunning = true
        btnStart.text = getString(R.string.game_reset)
        tvMessage.text = getString(R.string.game_message_play)

        clearInsects()
        repeat(settings.maxRoaches) { spawnInsect() }

        handler.removeCallbacks(gameLoop)
        handler.post(gameLoop)
        updateHud()
    }

    private fun spawnInsect() {
        val sizePx = (INSECT_SIZE_DP * density).toInt()
        val drawable = insectDrawables[Random.nextInt(insectDrawables.size)]

        val insectView = ImageView(requireContext()).apply {
            setImageResource(drawable)
            scaleType = ImageView.ScaleType.FIT_CENTER
            contentDescription = getString(R.string.game_insect_desc)
            layoutParams = FrameLayout.LayoutParams(sizePx, sizePx)
        }

        val maxX = (field.width - sizePx).coerceAtLeast(0)
        val maxY = (field.height - sizePx).coerceAtLeast(0)

        val insect = Insect(
            view = insectView,
            vx = 0f,
            vy = 0f
        )

        val angle = Random.nextFloat() * 2 * Math.PI
        val t = (settings.speed - 1).toFloat()
        var speedDp = SPEED_MAX_DP - (SPEED_MAX_DP - SPEED_MIN_DP) * exp(-SPEED_GROWTH * t)
        if (settings.speed > 1) {
            speedDp *= 1.5f
        }
        if (settings.speed > 5) {
            speedDp += (settings.speed - 5) * SPEED_AFTER_LEVEL_DP
        }
        val speedPx = speedDp * density
        insect.vx = (cos(angle) * speedPx).toFloat()
        insect.vy = (sin(angle) * speedPx).toFloat()

        insects.add(insect)
        insectView.setOnClickListener { onInsectClicked(insect) }
        field.addView(insectView)
        insectView.translationX = Random.nextFloat() * maxX
        insectView.translationY = Random.nextFloat() * maxY
    }

    private fun stepInsects(deltaSec: Float) {
        val fieldW = field.width
        val fieldH = field.height

        val iterator = insects.iterator()
        while (iterator.hasNext()) {
            val insect = iterator.next()

            insect.view.translationX += insect.vx * deltaSec
            insect.view.translationY += insect.vy * deltaSec

            val w = insect.view.width.toFloat()
            val h = insect.view.height.toFloat()

            if (insect.view.translationX < 0f) {
                insect.view.translationX = 0f
                insect.vx = abs(insect.vx)
                insect.view.scaleX = 1f
            } else if (insect.view.translationX + w > fieldW) {
                insect.view.translationX = (fieldW - w).coerceAtLeast(0f)
                insect.vx = -abs(insect.vx)
                insect.view.scaleX = -1f
            }

            if (insect.view.translationY < 0f) {
                insect.view.translationY = 0f
                insect.vy = abs(insect.vy)
            } else if (insect.view.translationY + h > fieldH) {
                insect.view.translationY = (fieldH - h).coerceAtLeast(0f)
                insect.vy = -abs(insect.vy)
            }
        }
    }

    private fun onInsectClicked(insect: Insect) {
        if (!gameRunning || field.indexOfChild(insect.view) < 0) return

        field.removeView(insect.view)
        insects.remove(insect)
        score += POINTS_PER_KILL
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
        tvTimer.text = "0"
        tvMessage.text = getString(R.string.game_over_score, score)
        btnStart.isEnabled = true
        btnStart.text = getString(R.string.game_restart)
        updateHud()
    }

    private fun resetGame() {
        gameRunning = false
        handler.removeCallbacks(gameLoop)
        clearInsects()
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

    private fun abs(value: Float): Float = kotlin.math.abs(value)
}