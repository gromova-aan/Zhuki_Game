package com.example.zhuki_game

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView

class AuthorAdapter(
    private val context: Context,
    private val authors: List<Author>
) : BaseAdapter() {

    override fun getCount(): Int = authors.size

    override fun getItem(position: Int): Author = authors[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view = convertView
            ?: LayoutInflater.from(context).inflate(R.layout.item_author, parent, false)

        val author = getItem(position)
        view.findViewById<ImageView>(R.id.ivAuthorPhoto).setImageResource(author.photoResId)
        view.findViewById<TextView>(R.id.tvAuthorName).text = author.name

        return view
    }
}
