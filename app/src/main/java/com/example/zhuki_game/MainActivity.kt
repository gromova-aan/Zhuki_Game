package com.example.zhuki_game

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Находим все элементы
        val etFullName = findViewById<EditText>(R.id.etFullName)
        val rgGender = findViewById<RadioGroup>(R.id.rgGender)
        val spCourse = findViewById<Spinner>(R.id.spCourse)
        val tvDifficulty = findViewById<TextView>(R.id.tvDifficulty)
        val sbDifficulty = findViewById<SeekBar>(R.id.sbDifficulty)
        val tvBirthDate = findViewById<TextView>(R.id.tvBirthDate)
        val btnSubmit = findViewById<Button>(R.id.btnSubmit)
        val tvResult = findViewById<TextView>(R.id.tvResult)

        // Настройка Spinner с курсами
        val courses = arrayOf("1 курс", "2 курс", "3 курс", "4 курс", "5 курс")
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            courses
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spCourse.adapter = adapter

        // Обработка SeekBar
        sbDifficulty.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                tvDifficulty.text = "Уровень сложности: $progress"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Дата рождения (по умолчанию — текущая)
        var selectedDate: Calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        tvBirthDate.text = dateFormat.format(selectedDate.time)

        // По нажатию на поле — диалог выбора даты
        tvBirthDate.setOnClickListener {
            DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    selectedDate = Calendar.getInstance().apply {
                        set(year, month, dayOfMonth)
                    }
                    tvBirthDate.text = dateFormat.format(selectedDate.time)
                },
                selectedDate.get(Calendar.YEAR),
                selectedDate.get(Calendar.MONTH),
                selectedDate.get(Calendar.DAY_OF_MONTH)
            ).apply {
                datePicker.maxDate = System.currentTimeMillis()
                show()
            }
        }

        // Обработка кнопки
        btnSubmit.setOnClickListener {
            val fullName = etFullName.text.toString().trim()

            if (fullName.isEmpty()) {
                Toast.makeText(this, "Введите ФИО", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val gender = when (rgGender.checkedRadioButtonId) {
                R.id.rbMale -> "Мужской"
                R.id.rbFemale -> "Женский"
                else -> "Не указан"
            }

            val course = spCourse.selectedItem.toString()
            val difficulty = sbDifficulty.progress

            val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
            val birthDate = dateFormat.format(selectedDate.time)

            // Создаём объект Player
            val player = Player(
                fullName = fullName,
                gender = gender,
                course = course,
                difficulty = difficulty,
                birthDate = birthDate
            )

            // Вывод результата
            tvResult.text = """
                ФИО: ${player.fullName}
                Пол: ${player.gender}
                Курс: ${player.course}
                Уровень сложности: ${player.difficulty}
                Дата рождения: ${player.birthDate}
            """.trimIndent()
        }
    }
}