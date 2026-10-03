package com.example.zhuki_game

import android.content.Context
import android.content.SharedPreferences
import android.util.Xml
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.io.StringReader
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class GoldRateRepository private constructor(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val api: CbrGoldApi = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(ScalarsConverterFactory.create())
        .build()
        .create(CbrGoldApi::class.java)

    fun cachedRate(): Double? = prefs.getString(KEY_RATE, null)?.toDoubleOrNull()

    suspend fun fetchGoldRate(): Double? = withContext(Dispatchers.IO) {
        val rate = loadFromNetwork()
        if (rate != null) {
            prefs.edit().putString(KEY_RATE, rate.toString()).apply()
            rate
        } else {
            cachedRate()
        }
    }

    private suspend fun loadFromNetwork(): Double? {
        return try {
            val format = SimpleDateFormat(PATTERN_DATE, Locale.US)
            val to = Calendar.getInstance()
            val from = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -DATE_RANGE_DAYS) }
            val response = api.getMetals(format.format(from.time), format.format(to.time))
            val xml = response.body()
            if (response.isSuccessful && !xml.isNullOrBlank()) {
                parseGoldRate(xml)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun parseGoldRate(xml: String): Double? {
        val parser = Xml.newPullParser()
        parser.setInput(StringReader(xml))
        var rate: Double? = null
        var goldRecord = false
        var inBuy = false
        while (parser.eventType != XmlPullParser.END_DOCUMENT) {
            when (parser.eventType) {
                XmlPullParser.START_TAG -> when (parser.name) {
                    "Record" -> goldRecord = parser.getAttributeValue(null, "Code") == GOLD_CODE
                    "Buy" -> inBuy = goldRecord
                }
                XmlPullParser.TEXT -> if (inBuy) {
                    parser.text.trim().replace(',', '.').toDoubleOrNull()?.let { value ->
                        if (value > 0) rate = value
                    }
                }
                XmlPullParser.END_TAG -> if (parser.name == "Buy") inBuy = false
            }
            parser.next()
        }
        return rate
    }

    companion object {
        private const val BASE_URL = "https://www.cbr.ru/"
        private const val PREFS_NAME = "gold_rate"
        private const val KEY_RATE = "rate"
        private const val PATTERN_DATE = "dd/MM/yyyy"
        private const val DATE_RANGE_DAYS = 7
        private const val GOLD_CODE = "1"

        @Volatile
        private var INSTANCE: GoldRateRepository? = null

        fun getInstance(context: Context): GoldRateRepository =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: GoldRateRepository(context.applicationContext).also { INSTANCE = it }
            }
    }
}
