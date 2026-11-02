package com.example.zhuki_game

import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import org.koin.androidx.viewmodel.ext.android.viewModel

class SkinsFragment : Fragment() {

    private val viewModel: SkinsEditorViewModel by viewModel()

    private lateinit var lvSets: ListView
    private lateinit var lvVariants: ListView
    private lateinit var btnNewSet: Button
    private lateinit var btnNewVariant: Button
    private lateinit var etSetName: EditText
    private lateinit var llColors: LinearLayout
    private lateinit var sbSize: SeekBar
    private lateinit var tvSizeValue: TextView
    private lateinit var llTextures: LinearLayout
    private lateinit var rgMood: RadioGroup
    private lateinit var rbFriendly: RadioButton
    private lateinit var rbAngry: RadioButton
    private lateinit var ivPreview: ImageView
    private lateinit var ivMoodBadge: ImageView
    private lateinit var btnSaveVariant: Button
    private lateinit var btnDeleteVariant: Button
    private lateinit var btnApply: Button
    private lateinit var btnDeleteSet: Button

    private lateinit var setAdapter: SkinSetAdapter
    private lateinit var variantAdapter: SkinVariantAdapter

    private var binding = false

    private val density: Float
        get() = resources.displayMetrics.density

    private val palette = listOf(
        0xFFE53935.toInt(),
        0xFFFB8C00.toInt(),
        0xFFFDD835.toInt(),
        0xFF43A047.toInt(),
        0xFF00ACC1.toInt(),
        0xFF1E88E5.toInt(),
        0xFF8E24AA.toInt(),
        0xFF6D4C41.toInt()
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_skins, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        lvSets = view.findViewById(R.id.lvSets)
        lvVariants = view.findViewById(R.id.lvVariants)
        btnNewSet = view.findViewById(R.id.btnNewSet)
        btnNewVariant = view.findViewById(R.id.btnNewVariant)
        etSetName = view.findViewById(R.id.etSetName)
        llColors = view.findViewById(R.id.llColors)
        sbSize = view.findViewById(R.id.sbSize)
        tvSizeValue = view.findViewById(R.id.tvSizeValue)
        llTextures = view.findViewById(R.id.llTextures)
        rgMood = view.findViewById(R.id.rgMood)
        rbFriendly = view.findViewById(R.id.rbFriendly)
        rbAngry = view.findViewById(R.id.rbAngry)
        ivPreview = view.findViewById(R.id.ivPreview)
        ivMoodBadge = view.findViewById(R.id.ivMoodBadge)
        btnSaveVariant = view.findViewById(R.id.btnSaveVariant)
        btnDeleteVariant = view.findViewById(R.id.btnDeleteVariant)
        btnApply = view.findViewById(R.id.btnApply)
        btnDeleteSet = view.findViewById(R.id.btnDeleteSet)

        setAdapter = SkinSetAdapter(requireContext())
        variantAdapter = SkinVariantAdapter(requireContext())
        lvSets.adapter = setAdapter
        lvVariants.adapter = variantAdapter

        lvSets.setOnItemClickListener { _, _, position, _ ->
            viewModel.selectSet(setAdapter.getItem(position).id)
        }
        lvVariants.setOnItemClickListener { _, _, position, _ ->
            viewModel.editVariant(position)
        }

        buildColorChips()
        buildTextureButtons()

        btnNewSet.setOnClickListener { viewModel.startNewSet() }
        btnNewVariant.setOnClickListener { viewModel.startNewVariant() }
        btnSaveVariant.setOnClickListener {
            viewModel.saveForm(etSetName.text?.toString().orEmpty())
        }
        btnDeleteVariant.setOnClickListener { viewModel.deleteVariant() }
        btnApply.setOnClickListener { viewModel.applyActive() }
        btnDeleteSet.setOnClickListener { viewModel.deleteSet() }

        sbSize.max = SkinVariant.MAX_SIZE_PERCENT - SkinVariant.MIN_SIZE_PERCENT
        sbSize.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (!fromUser || binding) return
                viewModel.updateDraftSize(SkinVariant.MIN_SIZE_PERCENT + progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        rgMood.setOnCheckedChangeListener { _, checkedId ->
            if (binding) return@setOnCheckedChangeListener
            when (checkedId) {
                R.id.rbFriendly -> viewModel.updateDraftMood(Mood.FRIENDLY)
                R.id.rbAngry -> viewModel.updateDraftMood(Mood.ANGRY)
            }
        }

        observeViewModel()
    }

    private fun observeViewModel() {
        viewModel.sets.observe(viewLifecycleOwner) { sets ->
            setAdapter.replace(sets)
            renderSelectedSet()
        }

        viewModel.activeSetId.observe(viewLifecycleOwner) { id ->
            setAdapter.setActive(id)
        }

        viewModel.selectedSetId.observe(viewLifecycleOwner) {
            renderSelectedSet()
        }

        viewModel.variants.observe(viewLifecycleOwner) { variants ->
            variantAdapter.replace(variants)
        }

        viewModel.editingIndex.observe(viewLifecycleOwner) { index ->
            variantAdapter.setSelectedIndex(index)
        }

        viewModel.draft.observe(viewLifecycleOwner) { draft ->
            renderDraft(draft)
        }

        viewModel.messageRes.observe(viewLifecycleOwner) { messageRes ->
            if (messageRes == null) return@observe
            Toast.makeText(requireContext(), messageRes, Toast.LENGTH_SHORT).show()
            viewModel.consumeMessage()
        }
    }

    private fun renderSelectedSet() {
        val id = viewModel.selectedSetId.value ?: SkinSet.DEFAULT_ID
        val set = viewModel.sets.value?.firstOrNull { it.id == id } ?: return

        setAdapter.setSelected(set.id)
        etSetName.setText(set.name)

        val editable = set.id != SkinSet.DEFAULT_ID
        setFormEnabled(editable)
    }

    private fun setFormEnabled(enabled: Boolean) {
        etSetName.isEnabled = enabled
        sbSize.isEnabled = enabled
        rbFriendly.isEnabled = enabled
        rbAngry.isEnabled = enabled
        btnNewVariant.isEnabled = enabled
        btnSaveVariant.isEnabled = enabled
        btnDeleteVariant.isEnabled = enabled
        btnDeleteSet.isEnabled = enabled
        setChildrenEnabled(llColors, enabled)
        setChildrenEnabled(llTextures, enabled)
    }

    private fun setChildrenEnabled(container: LinearLayout, enabled: Boolean) {
        for (index in 0 until container.childCount) {
            val child = container.getChildAt(index)
            child.isEnabled = enabled
            child.alpha = if (enabled) 1f else 0.4f
        }
    }

    private fun renderDraft(draft: SkinVariant) {
        binding = true
        sbSize.progress = draft.sizePercent - SkinVariant.MIN_SIZE_PERCENT
        tvSizeValue.text = getString(R.string.skin_size_value, draft.sizePercent)
        rgMood.check(
            if (draft.mood == Mood.ANGRY) R.id.rbAngry else R.id.rbFriendly
        )
        binding = false

        ivPreview.setImageResource(draft.texture.drawableRes ?: R.drawable.bug_1)
        ivPreview.clearColorFilter()
        draft.color?.let { ivPreview.setColorFilter(it) }

        val scale = draft.sizePercent / 100f
        ivPreview.scaleX = scale
        ivPreview.scaleY = scale

        ivMoodBadge.setImageResource(
            if (draft.mood == Mood.ANGRY) R.drawable.ic_mood_angry
            else R.drawable.ic_mood_friendly
        )

        updateColorSelection(draft.color)
        updateTextureSelection(draft.texture)
    }

    // палитра

    private fun buildColorChips() {
        llColors.removeAllViews()
        (listOf<Int?>(null) + palette).forEach { color ->
            val chip = TextView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(dp(40), dp(40)).apply {
                    marginEnd = dp(8)
                }
                gravity = Gravity.CENTER
                isFocusable = true
                isClickable = true
                text = if (color == null) "✕" else ""
                textSize = 16f
                setTextColor(0xFF3700B3.toInt())
                tag = color
                contentDescription = if (color == null) {
                    getString(R.string.skin_color_none)
                } else {
                    getString(R.string.skin_color_desc)
                }
                setOnClickListener { viewModel.updateDraftColor(color) }
            }
            llColors.addView(chip)
        }
    }

    private fun updateColorSelection(selectedColor: Int?) {
        for (index in 0 until llColors.childCount) {
            val chip = llColors.getChildAt(index)
            val chipColor = chip.tag as? Int
            val selected = if (selectedColor == null) {
                chip.tag == null
            } else {
                chipColor == selectedColor
            }
            chip.background = colorChipDrawable(chipColor, selected)
        }
    }

    private fun colorChipDrawable(color: Int?, selected: Boolean): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(color ?: 0xFFEDE7F6.toInt())
            if (selected) {
                setStroke(dp(3), 0xFF6200EE.toInt())
            } else if (color == null) {
                setStroke(dp(1), 0xFFB39DDB.toInt())
            }
        }
    }

    // текстуры

    private fun buildTextureButtons() {
        llTextures.removeAllViews()
        Texture.entries.forEach { texture ->
            val holder = FrameLayout(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(dp(56), dp(56)).apply {
                    marginEnd = dp(8)
                }
                setPadding(dp(6), dp(6), dp(6), dp(6))
                isFocusable = true
                isClickable = true
                tag = texture
                setOnClickListener { viewModel.updateDraftTexture(texture) }
            }

            val drawableRes = texture.drawableRes
            if (drawableRes == null) {
                holder.addView(TextView(requireContext()).apply {
                    layoutParams = FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                    )
                    gravity = Gravity.CENTER
                    text = "?"
                    textSize = 22f
                    setTextColor(0xFF3700B3.toInt())
                    setTypeface(typeface, Typeface.BOLD)
                    contentDescription = getString(R.string.skin_texture_random)
                })
            } else {
                holder.addView(ImageView(requireContext()).apply {
                    layoutParams = FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                    )
                    scaleType = ImageView.ScaleType.FIT_CENTER
                    setImageResource(drawableRes)
                    contentDescription = getString(
                        R.string.skin_texture_label,
                        texture.name.removePrefix("BUG_")
                    )
                })
            }

            llTextures.addView(holder)
        }
    }

    private fun updateTextureSelection(selectedTexture: Texture) {
        for (index in 0 until llTextures.childCount) {
            val holder = llTextures.getChildAt(index)
            val selected = holder.tag == selectedTexture
            holder.setBackgroundResource(
                if (selected) R.drawable.bg_texture_selected else R.drawable.bg_field
            )
        }
    }

    private fun dp(value: Int): Int = (value * density).toInt()
}
