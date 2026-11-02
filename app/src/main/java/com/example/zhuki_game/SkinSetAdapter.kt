package com.example.zhuki_game

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView

class SkinSetAdapter(private val context: Context) : BaseAdapter() {

    private var sets: List<SkinSet> = emptyList()
    private var activeId: Long = SkinSet.DEFAULT_ID
    private var selectedId: Long = SkinSet.DEFAULT_ID

    fun replace(newSets: List<SkinSet>) {
        sets = newSets
        notifyDataSetChanged()
    }

    fun setActive(id: Long) {
        if (activeId == id) return
        activeId = id
        notifyDataSetChanged()
    }

    fun setSelected(id: Long) {
        if (selectedId == id) return
        selectedId = id
        notifyDataSetChanged()
    }

    override fun getCount(): Int = sets.size

    override fun getItem(position: Int): SkinSet = sets[position]

    override fun getItemId(position: Int): Long = sets[position].id

    override fun hasStableIds(): Boolean = true

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view = convertView
            ?: LayoutInflater.from(context).inflate(R.layout.item_skin_set, parent, false)

        val set = getItem(position)

        view.setBackgroundResource(
            if (set.id == selectedId) R.drawable.bg_texture_selected else R.drawable.bg_field
        )

        view.findViewById<TextView>(R.id.tvSetName).text = set.name
        view.findViewById<TextView>(R.id.tvSetInfo).text =
            context.getString(R.string.skin_variants_count, set.variants.size)

        val preview = view.findViewById<ImageView>(R.id.ivSetPreview)
        val variant = set.variants.firstOrNull() ?: SkinSet.DEFAULT_VARIANT
        preview.setImageResource(variant.texture.drawableRes ?: R.drawable.bug_1)
        preview.clearColorFilter()
        variant.color?.let { preview.setColorFilter(it) }

        view.findViewById<TextView>(R.id.tvSetActive).visibility =
            if (set.id == activeId) View.VISIBLE else View.INVISIBLE

        return view
    }
}
