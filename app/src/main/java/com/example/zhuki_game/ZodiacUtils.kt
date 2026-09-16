package com.example.zhuki_game

private val zodiacTable = listOf(
    ZodiacEntry("Козерог",     12, 22,  1, 19, R.drawable.zodiac_capricorn),
    ZodiacEntry("Водолей",      1, 20,  2, 18, R.drawable.zodiac_aquarius),
    ZodiacEntry("Рыбы",         2, 19,  3, 20, R.drawable.zodiac_pisces),
    ZodiacEntry("Овен",         3, 21,  4, 19, R.drawable.zodiac_aries),
    ZodiacEntry("Телец",        4, 20,  5, 20, R.drawable.zodiac_taurus),
    ZodiacEntry("Близнецы",     5, 21,  6, 20, R.drawable.zodiac_gemini),
    ZodiacEntry("Рак",          6, 21,  7, 22, R.drawable.zodiac_cancer),
    ZodiacEntry("Лев",          7, 23,  8, 22, R.drawable.zodiac_leo),
    ZodiacEntry("Дева",         8, 23,  9, 22, R.drawable.zodiac_virgo),
    ZodiacEntry("Весы",         9, 23, 10, 22, R.drawable.zodiac_libra),
    ZodiacEntry("Скорпион",    10, 23, 11, 21, R.drawable.zodiac_scorpio),
    ZodiacEntry("Стрелец",     11, 22, 12, 21, R.drawable.zodiac_sagittarius)
)

fun getZodiac(day: Int, month: Int): String {
    return getZodiacEntry(day, month)?.name ?: "Неизвестно"
}

fun getZodiacImage(zodiac: String): Int {
    return zodiacTable.firstOrNull { it.name == zodiac }?.drawable
        ?: R.drawable.zodiac_aries
}

private fun getZodiacEntry(day: Int, month: Int): ZodiacEntry? {
    for (entry in zodiacTable) {
        if (isDateInRange(day, month, entry.startMonth, entry.startDay, entry.endMonth, entry.endDay)) {
            return entry
        }
    }
    return null
}

private fun isDateInRange(day: Int, month: Int,
                          startMonth: Int, startDay: Int,
                          endMonth: Int, endDay: Int): Boolean {
    if (startMonth <= endMonth) {
        return (month > startMonth || (month == startMonth && day >= startDay)) &&
               (month < endMonth   || (month == endMonth   && day <= endDay))
    }

    return (month > startMonth || (month == startMonth && day >= startDay)) ||
           (month < endMonth   || (month == endMonth   && day <= endDay))
}
