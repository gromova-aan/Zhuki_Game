package com.example.zhuki_game

import android.content.Context
import android.content.SharedPreferences

interface SkinStorage {
    fun getAll(): List<SkinSet>
    fun save(set: SkinSet)
    fun delete(id: Long)
    fun getActiveSetId(): Long
    fun setActiveSetId(id: Long)
}

object SkinCodec {

    private const val FIELD_SEPARATOR = '|'
    private const val VARIANT_SEPARATOR = ';'

    fun encode(sets: List<SkinSet>): String = sets.joinToString("\n") { encode(it) }

    fun encode(set: SkinSet): String {
        val variants = set.variants.joinToString(VARIANT_SEPARATOR.toString()) { encodeVariant(it) }
        return "${set.id}$FIELD_SEPARATOR${set.name}$FIELD_SEPARATOR$variants"
    }

    private fun encodeVariant(variant: SkinVariant): String {
        val color = variant.color?.toString() ?: ""
        return "$color@${variant.sizePercent}#${variant.texture.name}&${variant.mood.name}"
    }

    fun decode(document: String): List<SkinSet> {
        if (document.isBlank()) return emptyList()
        return document.lineSequence()
            .mapNotNull { decodeLine(it) }
            .toList()
    }

    private fun decodeLine(line: String): SkinSet? {
        if (line.isBlank()) return null
        val parts = line.split(FIELD_SEPARATOR)
        if (parts.size != 3) return null
        val id = parts[0].toLongOrNull() ?: return null
        val variants = parts[2]
            .split(VARIANT_SEPARATOR)
            .mapNotNull { decodeVariant(it) }
        val set = SkinSet(id = id, name = parts[1], variants = variants)
        return set.takeIf { it.isValid() }
    }

    private fun decodeVariant(text: String): SkinVariant? {
        val parts = text.split('@', '#', '&')
        if (parts.size != 4) return null
        val color = if (parts[0].isEmpty()) {
            null
        } else {
            parts[0].toIntOrNull() ?: return null
        }
        val size = parts[1].toIntOrNull() ?: return null
        val texture = Texture.entries.firstOrNull { it.name == parts[2] } ?: return null
        val mood = Mood.entries.firstOrNull { it.name == parts[3] } ?: return null
        return SkinVariant(color, size, texture, mood)
    }
}

class SharedPreferencesSkinStorage(context: Context) : SkinStorage {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun getAll(): List<SkinSet> = SkinCodec.decode(prefs.getString(KEY_SETS, "") ?: "")

    override fun save(set: SkinSet) {
        val current = getAll().toMutableList()
        val index = current.indexOfFirst { it.id == set.id }
        if (index >= 0) {
            current[index] = set
        } else {
            current.add(set)
        }
        prefs.edit().putString(KEY_SETS, SkinCodec.encode(current)).apply()
    }

    override fun delete(id: Long) {
        val remaining = getAll().filter { it.id != id }
        prefs.edit().putString(KEY_SETS, SkinCodec.encode(remaining)).apply()
    }

    override fun getActiveSetId(): Long = prefs.getLong(KEY_ACTIVE, SkinSet.DEFAULT_ID)

    override fun setActiveSetId(id: Long) {
        prefs.edit().putLong(KEY_ACTIVE, id).apply()
    }

    companion object {
        const val PREFS_NAME = "skins"
        const val KEY_SETS = "sets"
        const val KEY_ACTIVE = "active_set_id"
    }
}
