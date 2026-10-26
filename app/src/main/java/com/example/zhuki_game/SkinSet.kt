package com.example.zhuki_game

import kotlin.random.Random

enum class Texture(val drawableRes: Int?) {
    RANDOM(null),
    BUG_1(R.drawable.bug_1),
    BUG_2(R.drawable.bug_2),
    BUG_3(R.drawable.bug_3),
    BUG_4(R.drawable.bug_4),
    BUG_5(R.drawable.bug_5)
}

enum class Mood(val speedMultiplier: Float) {
    FRIENDLY(1.0f),
    ANGRY(1.5f)
}

data class SkinVariant(
    val color: Int?,
    val sizePercent: Int,
    val texture: Texture,
    val mood: Mood
) {

    fun errors(): List<String> {
        val result = mutableListOf<String>()
        if (sizePercent !in MIN_SIZE_PERCENT..MAX_SIZE_PERCENT) {
            result.add(
                "sizePercent must be in $MIN_SIZE_PERCENT..$MAX_SIZE_PERCENT, got $sizePercent"
            )
        }
        return result
    }

    fun isValid(): Boolean = errors().isEmpty()

    companion object {
        const val MIN_SIZE_PERCENT = 50
        const val MAX_SIZE_PERCENT = 150
    }
}

data class SkinSet(
    val id: Long,
    val name: String,
    val variants: List<SkinVariant>
) {

    fun errors(): List<String> {
        val result = mutableListOf<String>()
        val trimmed = name.trim()

        if (trimmed.isEmpty()) {
            result.add("name must not be blank")
        }
        if (trimmed.length > MAX_NAME_LENGTH) {
            result.add("name length must be <= $MAX_NAME_LENGTH, got ${trimmed.length}")
        }
        if (trimmed.contains('|') || trimmed.contains(';')) {
            result.add("name must not contain '|' or ';'")
        }
        if (variants.isEmpty()) {
            result.add("variants must not be empty")
        }
        if (variants.size > MAX_VARIANTS) {
            result.add("variants size must be <= $MAX_VARIANTS, got ${variants.size}")
        }
        variants.forEachIndexed { index, variant ->
            variant.errors().forEach { result.add("variant[$index]: $it") }
        }
        return result
    }

    fun isValid(): Boolean = errors().isEmpty()

    fun pickVariant(random: Random = Random.Default): SkinVariant {
        if (variants.isEmpty()) return DEFAULT_VARIANT
        return variants[random.nextInt(variants.size)]
    }

    companion object {
        const val MAX_NAME_LENGTH = 30
        const val MAX_VARIANTS = 10
        const val DEFAULT_ID = 0L

        val DEFAULT_VARIANT = SkinVariant(
            color = null,
            sizePercent = 100,
            texture = Texture.RANDOM,
            mood = Mood.FRIENDLY
        )

        val DEFAULT = SkinSet(
            id = DEFAULT_ID,
            name = "Классический",
            variants = listOf(DEFAULT_VARIANT)
        )
    }
}
