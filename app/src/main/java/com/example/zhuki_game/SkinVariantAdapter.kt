package com.example.zhuki_game

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView

class SkinVariantAdapter(private val context: Context) : BaseAdapter() {

    private var variants: List<SkinVariant> = emptyList()
    private var selectedIndex: Int = -1

    fun replace(newVariants: List<SkinVariant>) {
        variants = newVariants
        notifyDataSetChanged()
    }

    fun setSelectedIndex(index: Int) {
        if (selectedIndex == index) return
        selectedIndex = index
        notifyDataSetChanged()
    }

    override fun getCount(): Int = variants.size

    override fun getItem(position: Int): SkinVariant = variants[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view = convertView
            ?: LayoutInflater.from(context).inflate(R.layout.item_skin_variant, parent, false)

        val variant = getItem(position)

        view.setBackgroundResource(
            if (position == selectedIndex) R.drawable.bg_texture_selected
            else R.drawable.bg_field
        )

        val preview = view.findViewById<ImageView>(R.id.ivVariantPreview)
        preview.setImageResource(variant.texture.drawableRes ?: R.drawable.bug_1)
        preview.clearColorFilter()
        variant.color?.let { preview.setColorFilter(it) }

        view.findViewById<ImageView>(R.id.ivVariantMood).setImageResource(
            if (variant.mood == Mood.ANGRY) R.drawable.ic_mood_angry
            else R.drawable.ic_mood_friendly
        )

        val moodLabel = context.getString(
            if (variant.mood == Mood.ANGRY) R.string.skin_mood_angry
            else R.string.skin_mood_friendly
        )
        val textureLabel = if (variant.texture == Texture.RANDOM) {
            context.getString(R.string.skin_texture_random)
        } else {
            context.getString(
                R.string.skin_texture_label,
                variant.texture.name.removePrefix("BUG_")
            )
        }

        view.findViewById<TextView>(R.id.tvVariantInfo).text = context.getString(
            R.string.skin_variant_info,
            variant.sizePercent,
            moodLabel,
            textureLabel
        )

        return view
    }
}
