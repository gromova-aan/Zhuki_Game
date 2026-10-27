package com.example.zhuki_game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SkinSetTest {

    private fun variant(
        color: Int? = null,
        sizePercent: Int = 100,
        texture: Texture = Texture.RANDOM,
        mood: Mood = Mood.FRIENDLY
    ) = SkinVariant(color, sizePercent, texture, mood)

    private fun set(
        id: Long = 1L,
        name: String = "Тест",
        variants: List<SkinVariant> = listOf(variant())
    ) = SkinSet(id, name, variants)

    @Test
    fun default_isValid() {
        assertTrue(SkinSet.DEFAULT.isValid())
        assertTrue(SkinSet.DEFAULT_VARIANT.isValid())
        assertEquals(0, SkinSet.DEFAULT.errors().size)
    }

    @Test
    fun defaultVariant_matchesClassicLabBehavior() {
        val v = SkinSet.DEFAULT_VARIANT
        assertEquals(null, v.color)
        assertEquals(100, v.sizePercent)
        assertEquals(Texture.RANDOM, v.texture)
        assertEquals(Mood.FRIENDLY, v.mood)
    }

    @Test
    fun variant_sizePercent_mustBeInRange50to150() {
        assertTrue(variant(sizePercent = 50).isValid())
        assertTrue(variant(sizePercent = 150).isValid())
        assertFalse(variant(sizePercent = 49).isValid())
        assertFalse(variant(sizePercent = 151).isValid())
    }

    @Test
    fun variant_errors_mentionSizePercent() {
        val errors = variant(sizePercent = 10).errors()
        assertEquals(1, errors.size)
        assertTrue(errors[0].contains("sizePercent"))
    }

    @Test
    fun set_name_mustNotBeBlank() {
        assertFalse(set(name = "").isValid())
        assertFalse(set(name = "   ").isValid())
        assertTrue(set(name = "  Норма  ").isValid())
    }

    @Test
    fun set_name_mustNotContainSeparators() {
        assertFalse(set(name = "a|b").isValid())
        assertFalse(set(name = "a;b").isValid())
    }

    @Test
    fun set_name_length_mustBeAtMost30() {
        assertTrue(set(name = "a".repeat(30)).isValid())
        assertFalse(set(name = "a".repeat(31)).isValid())
    }

    @Test
    fun set_variants_mustBeBetween1And10() {
        assertTrue(set(variants = List(1) { variant() }).isValid())
        assertTrue(set(variants = List(10) { variant() }).isValid())
        assertFalse(set(variants = emptyList()).isValid())
        assertFalse(set(variants = List(11) { variant() }).isValid())
    }

    @Test
    fun set_invalidVariant_reportedWithItsIndex() {
        val broken = variant(sizePercent = 42)
        val errors = set(variants = listOf(variant(), broken)).errors()
        assertTrue(errors.any { it.contains("variant[1]") && it.contains("sizePercent") })
    }

    @Test
    fun mood_speedMultipliers() {
        assertEquals(1.0f, Mood.FRIENDLY.speedMultiplier, 0.0f)
        assertEquals(1.5f, Mood.ANGRY.speedMultiplier, 0.0f)
    }
}
