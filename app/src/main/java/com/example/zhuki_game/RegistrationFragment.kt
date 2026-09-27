package com.example.zhuki_game

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class RegistrationFragment : Fragment() {

    private lateinit var repository: UserRepository
    private var users: List<User> = emptyList()
    private var selectedUser: User? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_registration, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        repository = UserRepository(requireContext())

        val spPlayer = view.findViewById<Spinner>(R.id.spPlayer)
        val etFullName = view.findViewById<EditText>(R.id.etFullName)
        val rgGender = view.findViewById<RadioGroup>(R.id.rgGender)
        val spCourse = view.findViewById<Spinner>(R.id.spCourse)
        val tvDifficulty = view.findViewById<TextView>(R.id.tvDifficulty)
        val sbDifficulty = view.findViewById<SeekBar>(R.id.sbDifficulty)
        val tvBirthDate = view.findViewById<TextView>(R.id.tvBirthDate)
        val btnSubmit = view.findViewById<Button>(R.id.btnSubmit)
        val tvResult = view.findViewById<TextView>(R.id.tvResult)
        val ivZodiac = view.findViewById<ImageView>(R.id.ivZodiac)
        val tvZodiacName = view.findViewById<TextView>(R.id.tvZodiacName)

        val courses = arrayOf("1 курс", "2 курс", "3 курс", "4 курс", "5 курс")
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            courses
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spCourse.adapter = adapter

        sbDifficulty.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                tvDifficulty.text = "Уровень сложности: $progress"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        var selectedDate: Calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        tvBirthDate.text = dateFormat.format(selectedDate.time)

        fun updateZodiac(date: Calendar) {
            val z = getZodiac(
                date.get(Calendar.DAY_OF_MONTH),
                date.get(Calendar.MONTH) + 1
            )
            ivZodiac.setImageResource(getZodiacImage(z))
            tvZodiacName.text = z
        }

        updateZodiac(selectedDate)

        fun clearForm() {
            etFullName.setText("")
            rgGender.check(R.id.rbMale)
            spCourse.setSelection(0)
            sbDifficulty.progress = 3
            selectedDate = Calendar.getInstance()
            tvBirthDate.text = dateFormat.format(selectedDate.time)
            updateZodiac(selectedDate)
            tvResult.text = ""
        }

        fun fillForm(user: User) {
            etFullName.setText(user.fullName)
            if (user.gender == "Мужской") {
                rgGender.check(R.id.rbMale)
            } else {
                rgGender.check(R.id.rbFemale)
            }
            spCourse.setSelection(courses.indexOf(user.course).takeIf { it >= 0 } ?: 0)
            sbDifficulty.progress = user.difficulty
            try {
                dateFormat.parse(user.birthDate)?.let {
                    selectedDate = Calendar.getInstance().apply { time = it }
                    tvBirthDate.text = dateFormat.format(selectedDate.time)
                    updateZodiac(selectedDate)
                }
            } catch (_: Exception) {
                // оставляем текущую дату
            }
        }

        fun refreshPlayers(selectId: Long = UserRepository.NO_USER) {
            viewLifecycleOwner.lifecycleScope.launch {
                users = withContext(Dispatchers.IO) { repository.getAllUsers() }
                val names = listOf(getString(R.string.player_new)) + users.map { it.fullName }
                spPlayer.adapter = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_spinner_item,
                    names
                ).also {
                    it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                }
                val wantedId =
                    if (selectId != UserRepository.NO_USER) selectId
                    else repository.getCurrentUserId()
                val pos = users.indexOfFirst { it.id == wantedId }
                    .takeIf { it >= 0 }?.plus(1) ?: 0
                spPlayer.setSelection(pos)
            }
        }

        spPlayer.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?, v: View?, position: Int, id: Long
            ) {
                if (position == 0) {
                    selectedUser = null
                    clearForm()
                } else {
                    selectedUser = users.getOrNull(position - 1)
                    selectedUser?.let {
                        fillForm(it)
                        repository.setCurrentUserId(it.id)
                    }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        refreshPlayers()

        tvBirthDate.setOnClickListener {
            DatePickerDialog(
                requireContext(),
                { _, year, month, dayOfMonth ->
                    selectedDate = Calendar.getInstance().apply {
                        set(year, month, dayOfMonth)
                    }
                    tvBirthDate.text = dateFormat.format(selectedDate.time)
                    updateZodiac(selectedDate)
                },
                selectedDate.get(Calendar.YEAR),
                selectedDate.get(Calendar.MONTH),
                selectedDate.get(Calendar.DAY_OF_MONTH)
            ).apply {
                datePicker.maxDate = System.currentTimeMillis()
                show()
            }
        }

        btnSubmit.setOnClickListener {
            val fullName = etFullName.text.toString().trim()

            if (fullName.isEmpty()) {
                Toast.makeText(requireContext(), "Введите ФИО", Toast.LENGTH_SHORT).show()
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

            val zodiac = getZodiac(
                selectedDate.get(Calendar.DAY_OF_MONTH),
                selectedDate.get(Calendar.MONTH) + 1
            )

            val user = User(
                id = selectedUser?.id ?: 0,
                fullName = fullName,
                gender = gender,
                course = course,
                difficulty = difficulty,
                birthDate = birthDate,
                zodiac = zodiac
            )

            viewLifecycleOwner.lifecycleScope.launch {
                val newId = withContext(Dispatchers.IO) { repository.register(user) }
                refreshPlayers(selectId = newId)
                tvResult.text = """
                    ФИО: ${user.fullName}
                    Пол: ${user.gender}
                    Курс: ${user.course}
                    Уровень сложности: ${user.difficulty}
                    Дата рождения: ${user.birthDate}
                    Знак зодиака: ${user.zodiac}
                """.trimIndent()
                Toast.makeText(
                    requireContext(),
                    R.string.player_saved,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}
