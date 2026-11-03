package com.example.zhuki_game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SkinTextureMappingTest {

    @Test
    fun randomTexture_hasNoDrawable() {
        assertNull(Texture.RANDOM.drawableRes)
    }

    @Test
    fun concreteTextures_haveNonNullAndDistinctDrawables() {
        val concrete = Texture.entries.filter { it != Texture.RANDOM }
        val drawables = concrete.map { it.drawableRes }

        assertEquals(5, concrete.size)
        assertTrue(drawables.all { it != null })
        assertEquals(drawables.size, drawables.toSet().size)
    }
}
