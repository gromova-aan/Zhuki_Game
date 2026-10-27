package com.example.zhuki_game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SkinCodecTest {

    private fun variant(
        color: Int? = null,
        size: Int = 100,
        texture: Texture = Texture.RANDOM,
        mood: Mood = Mood.FRIENDLY
    ) = SkinVariant(color, size, texture, mood)

    @Test
    fun encode_format_isStable() {
        val set = SkinSet(7, "A", listOf(variant(null, 130, Texture.BUG_2, Mood.ANGRY)))
        assertEquals("7|A|@130#BUG_2&ANGRY", SkinCodec.encode(set))
    }

    @Test
    fun roundTrip_preservesSetWithThreeVariants() {
        val original = SkinSet(
            id = 7,
            name = "Пёстрая стая",
            variants = listOf(
                variant(null, 130, Texture.BUG_2, Mood.ANGRY),
                variant(-16711936, 80, Texture.RANDOM, Mood.FRIENDLY),
                variant(0, 50, Texture.BUG_5, Mood.ANGRY)
            )
        )

        val decoded = SkinCodec.decode(SkinCodec.encode(listOf(original)))

        assertEquals(listOf(original), decoded)
    }

    @Test
    fun roundTrip_preservesMultipleSetsInOrder() {
        val sets = listOf(
            SkinSet(1, "Первый", listOf(variant())),
            SkinSet(2, "Второй", listOf(variant(size = 150), variant(mood = Mood.ANGRY))),
            SkinSet(10, "Третий", listOf(variant(color = 42)))
        )

        assertEquals(sets, SkinCodec.decode(SkinCodec.encode(sets)))
    }

    @Test
    fun decode_emptyDocument_returnsEmptyList() {
        assertTrue(SkinCodec.decode("").isEmpty())
        assertTrue(SkinCodec.decode("   ").isEmpty())
        assertTrue(SkinCodec.decode("\n\n").isEmpty())
    }

    @Test
    fun decode_garbageLines_areSkipped() {
        val document = listOf(
            "это не набор",
            "1|имя",
            "не-число|имя|@100#RANDOM&FRIENDLY",
            "2|имя|@100#NO_SUCH_TEXTURE&FRIENDLY",
            "3||@100#RANDOM&FRIENDLY",
            "4|Хороший|@100#BUG_3&ANGRY"
        ).joinToString("\n")

        val decoded = SkinCodec.decode(document)

        assertEquals(1, decoded.size)
        assertEquals("Хороший", decoded[0].name)
    }

    @Test
    fun decode_lineWithOneBrokenVariant_keepsValidVariant() {
        val decoded = SkinCodec.decode("5|Смешанный|битый;@120#BUG_1&ANGRY")

        assertEquals(1, decoded.size)
        assertEquals(1, decoded[0].variants.size)
        assertEquals(120, decoded[0].variants[0].sizePercent)
    }

    @Test
    fun decode_lineWithTooManyVariants_isSkipped() {
        val variants = List(11) { "@100#RANDOM&FRIENDLY" }.joinToString(";")

        assertTrue(SkinCodec.decode("6|Много|$variants").isEmpty())
    }
}
