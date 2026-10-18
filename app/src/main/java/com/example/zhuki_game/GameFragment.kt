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
import org.koin.androidx.viewmodel.ext.android.viewModel

class GameFragment : Fragment() {

    private val viewModel: GameViewModel by viewModel()

    private lateinit var field: FrameLayout
    private lateinit var tvScore: TextView
    private lateinit var tvTimer: TextView
    private lateinit var tvMessage: TextView
    private lateinit var btnStart: Button

    private val handler = Handler(Looper.getMainLooper())
    private val insectViews = mutableMapOf<Long, ImageView>()
    private var bonusImageView: ImageView? = null
    private var lastFrameTime = 0L
    private var loopRunning = false

    private var screamPlayer: MediaPlayer? = null
    private var sensorManager: SensorManager? = null
    private var gravitySensor: Sensor? = null

    private val density: Float
        get() = resources.displayMetrics.density

    private val goldColor: Int = 0xFFFFD700.toInt()

    private val sensorListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            if (event.sensor.type == Sensor.TYPE_GRAVITY) {
                viewModel.gravityX = event.values[0] //наклон влево/вправо
                viewModel.gravityY = event.values[1] //вперед/назад
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    private val gameLoop = object : Runnable {
        override fun run() {
            if (!viewModel.isRunning) {
                loopRunning = false
                updateSound()
                return
            }

            val now = SystemClock.uptimeMillis()
            val deltaSec = if (lastFrameTime == 0L) 0f else (now - lastFrameTime) / 1000f
            lastFrameTime = now

            viewModel.tick(now, deltaSec)
            renderInsects()
            renderBonus()
            updateSound()

            if (viewModel.isRunning) {
                handler.postDelayed(this, GameViewModel.FRAME_MS)
            } else {
                loopRunning = false
                renderInsects()
                renderBonus()
                stopScream()
            }
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

        field = view.findViewById(R.id.gameField)
        tvScore = view.findViewById(R.id.tvScore)
        tvTimer = view.findViewById(R.id.tvTimer)
        tvMessage = view.findViewById(R.id.tvMessage)
        btnStart = view.findViewById(R.id.btnStart)

        field.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN && viewModel.isRunning) {
                viewModel.applyMiss()
            }
            true
        }

        //изменение размера поля
        field.addOnLayoutChangeListener { _, left, top, right, bottom, _, _, _, _ ->
            viewModel.updateFieldSize(
                (right - left) / density,
                (bottom - top) / density
            )
        }

        btnStart.setOnClickListener {
            if (viewModel.isRunning) {
                viewModel.resetGame()
            } else {
                viewModel.startGame()
            }
        }

        observeViewModel()
        renderInsects()
        renderBonus()
    }

    override fun onResume() {
        super.onResume()
        registerSensor()
        if (viewModel.isRunning) {
            startLoop()
        }
    }

    override fun onPause() {
        super.onPause()
        stopScream()
        sensorManager?.unregisterListener(sensorListener)
        stopLoop()
        if (viewModel.isRunning && !requireActivity().isChangingConfigurations) {
            viewModel.endGame()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopLoop()
        stopScream()
        insectViews.clear()
        bonusImageView = null
    }

    private fun observeViewModel() {
        viewModel.state.observe(viewLifecycleOwner) { state ->
            when (state) {
                GameViewModel.GameState.READY -> {
                    tvMessage.text = getString(R.string.game_message_ready)
                    btnStart.text = getString(R.string.game_start)
                    btnStart.isEnabled = true
                    tvScore.text = "0"
                    tvTimer.text = "0"
                    stopLoop()
                }

                GameViewModel.GameState.PLAYING -> {
                    tvMessage.text = getString(R.string.game_message_play)
                    btnStart.text = getString(R.string.game_reset)
                    btnStart.isEnabled = true
                    startLoop()
                }

                GameViewModel.GameState.FINISHED -> {
                    tvTimer.text = "0"
                    tvMessage.text =
                        getString(R.string.game_over_score, viewModel.score.value ?: 0)
                    btnStart.text = getString(R.string.game_restart)
                    btnStart.isEnabled = true
                    stopLoop()
                }
            }
        }

        viewModel.score.observe(viewLifecycleOwner) { score ->
            tvScore.text = score.toString()
            if (viewModel.state.value == GameViewModel.GameState.FINISHED) {
                tvMessage.text = getString(R.string.game_over_score, score)
            }
        }

        viewModel.timeLeft.observe(viewLifecycleOwner) { time ->
            tvTimer.text = time.coerceAtLeast(0).toString()
        }

        viewModel.recordResult.observe(viewLifecycleOwner) { result ->
            if (result == null) return@observe
            val message = when (result) {
                is RecordSaveResult.NoUser -> getString(R.string.game_no_user)
                is RecordSaveResult.Created -> getString(R.string.game_record_saved, result.score)
                is RecordSaveResult.Beaten -> getString(R.string.game_record_beaten, result.score)
                is RecordSaveResult.Kept -> getString(R.string.game_record_kept, result.bestScore)
            }
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
            viewModel.consumeRecordResult()
        }
    }

    //синхронизация жуков
    private fun renderInsects() {
        val activeIds = HashSet<Long>(viewModel.insects.size)

        for (insect in viewModel.insects) {
            activeIds.add(insect.id)
            //insectViews - пустая карта нового фрагмента
            //insectViews[1L] == null -> создаём новый ImageView
            val insectView = insectViews[insect.id] ?: createInsectView(insect).also {
                insectViews[insect.id] = it
                field.addView(it)
            }
            insectView.translationX = insect.xDp * density
            insectView.translationY = insect.yDp * density
            insectView.scaleX = if (insect.flipX) -1f else 1f
        }

        val iterator = insectViews.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (entry.key !in activeIds) {
                field.removeView(entry.value)
                iterator.remove()
            }
        }
    }

    private fun createInsectView(insect: GameViewModel.InsectState): ImageView {
        val sizeDp =
            if (insect.golden) GameViewModel.GOLD_SIZE_DP else GameViewModel.INSECT_SIZE_DP
        val sizePx = (sizeDp * density).toInt()
        return ImageView(requireContext()).apply {
            setImageResource(insect.drawableRes)
            scaleType = ImageView.ScaleType.FIT_CENTER
            contentDescription = getString(
                if (insect.golden) R.string.game_golden_desc else R.string.game_insect_desc
            )
            layoutParams = FrameLayout.LayoutParams(sizePx, sizePx)
            if (insect.golden) setColorFilter(goldColor)
            setOnClickListener { viewModel.onInsectClicked(insect.id) }
        }
    }

    private fun renderBonus() {
        val bonus = viewModel.bonus
        if (bonus == null) {
            bonusImageView?.let { field.removeView(it) }
            bonusImageView = null
            return
        }

        val bonusView = bonusImageView ?: ImageView(requireContext()).apply {
            val sizePx = (GameViewModel.BONUS_SIZE_DP * density).toInt()
            setImageResource(R.drawable.ic_bonus)
            scaleType = ImageView.ScaleType.FIT_CENTER
            contentDescription = getString(R.string.game_bonus_desc)
            layoutParams = FrameLayout.LayoutParams(sizePx, sizePx)
            //новый ImageView бонус
            setOnClickListener { viewModel.onBonusClicked() }
        }.also {
            bonusImageView = it
            field.addView(it)
        }

        bonusView.translationX = bonus.xDp * density
        bonusView.translationY = bonus.yDp * density
    }

    private fun startLoop() {
        if (loopRunning) return
        loopRunning = true
        lastFrameTime = 0L
        handler.removeCallbacks(gameLoop)
        handler.post(gameLoop)
    }

    private fun stopLoop() {
        loopRunning = false
        handler.removeCallbacks(gameLoop)
    }

    private fun registerSensor() {
        sensorManager =
            requireContext().getSystemService(Context.SENSOR_SERVICE) as SensorManager?
        gravitySensor = sensorManager?.getDefaultSensor(Sensor.TYPE_GRAVITY)
        gravitySensor?.let {
            sensorManager?.registerListener(sensorListener, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    private fun updateSound() {
        if (viewModel.tiltActive) startScream() else stopScream()
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
}
