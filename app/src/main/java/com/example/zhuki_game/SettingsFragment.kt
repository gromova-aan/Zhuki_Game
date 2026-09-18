package com.example.zhuki_game

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment

class SettingsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val repository = SettingsRepository(SharedPreferencesStorage(requireContext()))
        val current = repository.load()

        val tvSpeedValue = view.findViewById<TextView>(R.id.tvSpeedValue)
        val tvRoachesValue = view.findViewById<TextView>(R.id.tvRoachesValue)
        val tvBonusValue = view.findViewById<TextView>(R.id.tvBonusValue)
        val tvRoundValue = view.findViewById<TextView>(R.id.tvRoundValue)

        val sbSpeed = view.findViewById<SeekBar>(R.id.sbSpeed)
        val sbRoaches = view.findViewById<SeekBar>(R.id.sbRoaches)
        val sbBonus = view.findViewById<SeekBar>(R.id.sbBonus)
        val sbRound = view.findViewById<SeekBar>(R.id.sbRound)

        bindSeekBar(
            seekBar = sbSpeed,
            label = tvSpeedValue,
            min = GameSettings.MIN_SPEED,
            max = GameSettings.MAX_SPEED,
            initial = current.speed
        )
        bindSeekBar(
            seekBar = sbRoaches,
            label = tvRoachesValue,
            min = GameSettings.MIN_ROACHES,
            max = GameSettings.MAX_ROACHES,
            initial = current.maxRoaches
        )
        bindSeekBar(
            seekBar = sbBonus,
            label = tvBonusValue,
            min = GameSettings.MIN_BONUS_INTERVAL,
            max = GameSettings.MAX_BONUS_INTERVAL,
            initial = current.bonusInterval
        )
        bindSeekBar(
            seekBar = sbRound,
            label = tvRoundValue,
            min = GameSettings.MIN_ROUND_DURATION,
            max = GameSettings.MAX_ROUND_DURATION,
            initial = current.roundDuration
        )

        view.findViewById<Button>(R.id.btnSaveSettings).setOnClickListener {
            repository.save(
                GameSettings(
                    speed = sbSpeed.progress + GameSettings.MIN_SPEED,
                    maxRoaches = sbRoaches.progress + GameSettings.MIN_ROACHES,
                    bonusInterval = sbBonus.progress + GameSettings.MIN_BONUS_INTERVAL,
                    roundDuration = sbRound.progress + GameSettings.MIN_ROUND_DURATION
                )
            )
            Toast.makeText(requireContext(), R.string.settings_saved_toast, Toast.LENGTH_SHORT).show()
        }

        view.findViewById<Button>(R.id.btnResetSettings).setOnClickListener {
            val defaults = repository.resetToDefaults()
            bindSeekBar(sbSpeed, tvSpeedValue, GameSettings.MIN_SPEED, GameSettings.MAX_SPEED, defaults.speed)
            bindSeekBar(sbRoaches, tvRoachesValue, GameSettings.MIN_ROACHES, GameSettings.MAX_ROACHES, defaults.maxRoaches)
            bindSeekBar(sbBonus, tvBonusValue, GameSettings.MIN_BONUS_INTERVAL, GameSettings.MAX_BONUS_INTERVAL, defaults.bonusInterval)
            bindSeekBar(sbRound, tvRoundValue, GameSettings.MIN_ROUND_DURATION, GameSettings.MAX_ROUND_DURATION, defaults.roundDuration)
            Toast.makeText(requireContext(), R.string.settings_reset_toast, Toast.LENGTH_SHORT).show()
        }
    }

    private fun bindSeekBar(
        seekBar: SeekBar,
        label: TextView,
        min: Int,
        max: Int,
        initial: Int
    ) {
        seekBar.max = max - min
        seekBar.progress = initial - min
        label.text = initial.toString()
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                label.text = (progress + min).toString()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }
}