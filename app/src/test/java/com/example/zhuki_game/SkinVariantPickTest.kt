package com.example.zhuki_game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class SkinVariantPickTest {

    private fun variant(size: Int = 100) =
        SkinVariant(null, size, Texture.RANDOM, Mood.FRIENDLY)

    @Test
    fun pick_singleVariant_isDeterministic() {
        val set = SkinSet(1, "Один", listOf(variant(150)))

        assertEquals(variant(150), set.pickVariant(Random(42)))
        assertEquals(variant(150), set.pickVariant(Random(0)))
    }

    @Test
    fun pick_alwaysReturnsVariantFromOwnSet() {
        val variants = List(5) { variant(size = 50 + it * 10) }
        val set = SkinSet(1, "Пять", variants)
        val random = Random(42)

        repeat(100) {
            assertTrue(set.pickVariant(random) in variants)
        }
    }

    @Test
    fun pick_allTenVariantsAreReachable() {
        val variants = List(10) { variant(size = 50 + it * 10) }
        val set = SkinSet(1, "Десять", variants)
        val random = Random(42)
        val visited = mutableSetOf<SkinVariant>()

        repeat(1000) { visited.add(set.pickVariant(random)) }

        assertEquals(variants.toSet(), visited)
    }

    @Test
    fun pick_emptySet_fallsBackToDefaultVariant() {
        val set = SkinSet(1, "Пусто", emptyList())

        assertEquals(SkinSet.DEFAULT_VARIANT, set.pickVariant(Random(42)))
    }
}
