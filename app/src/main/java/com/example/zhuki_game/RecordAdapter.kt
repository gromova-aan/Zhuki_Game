package com.example.zhuki_game

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RecordAdapter(
    private val context: Context,
    private val records: List<RecordWithUser>
) : BaseAdapter() {

    private val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())

    override fun getCount(): Int = records.size

    override fun getItem(position: Int): RecordWithUser = records[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view = convertView
            ?: LayoutInflater.from(context).inflate(R.layout.item_record, parent, false)

        val record = getItem(position)
        view.findViewById<TextView>(R.id.tvRecordName).text = record.userName
        view.findViewById<TextView>(R.id.tvRecordScore).text = "Очки: ${record.score}"
        view.findViewById<TextView>(R.id.tvRecordDifficulty).text =
            "Сложность: ${record.difficulty}"
        view.findViewById<TextView>(R.id.tvRecordDate).text =
            dateFormat.format(Date(record.dateMillis))

        return view
    }
}
