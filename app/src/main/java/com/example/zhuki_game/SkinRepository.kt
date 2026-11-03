package com.example.zhuki_game

sealed class SkinSaveResult {
    data class Success(val set: SkinSet) : SkinSaveResult()
    data class Failure(val errors: List<String>) : SkinSaveResult()
}

open class SkinRepository(private val storage: SkinStorage) {

    open fun getSets(): List<SkinSet> = storage.getAll()

    open fun activeSet(): SkinSet {
        val id = storage.getActiveSetId()
        if (id == SkinSet.DEFAULT_ID) return SkinSet.DEFAULT
        return storage.getAll().firstOrNull { it.id == id } ?: SkinSet.DEFAULT
    }

    open fun setActive(id: Long) {
        if (id == SkinSet.DEFAULT_ID || storage.getAll().any { it.id == id }) {
            storage.setActiveSetId(id)
        }
    }

    open fun saveSet(set: SkinSet): SkinSaveResult {
        val errors = set.errors()
        if (errors.isNotEmpty()) return SkinSaveResult.Failure(errors)

        val existing = storage.getAll()
        val isNew = existing.none { it.id == set.id }
        if (isNew && existing.size >= MAX_SETS) {
            return SkinSaveResult.Failure(listOf("too many sets, max $MAX_SETS"))
        }

        val saved = if (isNew) set.copy(id = nextId(existing)) else set
        storage.save(saved)
        return SkinSaveResult.Success(saved)
    }

    open fun deleteSet(id: Long) {
        storage.delete(id)
        if (storage.getActiveSetId() == id) {
            storage.setActiveSetId(SkinSet.DEFAULT_ID)
        }
    }

    private fun nextId(existing: List<SkinSet>): Long =
        (existing.maxOfOrNull { it.id } ?: 0L) + 1

    companion object {
        const val MAX_SETS = 20
    }
}
