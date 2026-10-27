package com.example.zhuki_game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SkinRepositoryTest {

    private class FakeSkinStorage : SkinStorage {
        val sets = mutableListOf<SkinSet>()
        var activeId = SkinSet.DEFAULT_ID

        override fun getAll(): List<SkinSet> = sets.toList()

        override fun save(set: SkinSet) {
            val index = sets.indexOfFirst { it.id == set.id }
            if (index >= 0) {
                sets[index] = set
            } else {
                sets.add(set)
            }
        }

        override fun delete(id: Long) {
            sets.removeAll { it.id == id }
        }

        override fun getActiveSetId(): Long = activeId

        override fun setActiveSetId(id: Long) {
            activeId = id
        }
    }

    private fun set(id: Long = 0L, name: String = "Тест") =
        SkinSet(id, name, listOf(SkinSet.DEFAULT_VARIANT))

    @Test
    fun activeSet_returnsDefaultWhenStorageIsEmpty() {
        val repository = SkinRepository(FakeSkinStorage())

        assertEquals(SkinSet.DEFAULT, repository.activeSet())
    }

    @Test
    fun saveSet_assignsIncrementalIdsToNewSets() {
        val storage = FakeSkinStorage()
        val repository = SkinRepository(storage)

        val first = repository.saveSet(set())
        val second = repository.saveSet(set())

        assertEquals(1L, (first as SkinSaveResult.Success).set.id)
        assertEquals(2L, (second as SkinSaveResult.Success).set.id)
        assertEquals(2, storage.sets.size)
    }

    @Test
    fun saveSet_invalidSet_failsAndKeepsStorageUntouched() {
        val storage = FakeSkinStorage()
        val result = SkinRepository(storage).saveSet(set(name = ""))

        assertTrue(result is SkinSaveResult.Failure)
        assertTrue((result as SkinSaveResult.Failure).errors.any { it.contains("name") })
        assertTrue(storage.sets.isEmpty())
    }

    @Test
    fun saveSet_updatesExistingSetByIdWithoutDuplicates() {
        val storage = FakeSkinStorage()
        val repository = SkinRepository(storage)
        val created = (repository.saveSet(set(name = "Один")) as SkinSaveResult.Success).set

        val updated = created.copy(
            variants = listOf(
                SkinSet.DEFAULT_VARIANT,
                SkinVariant(-1, 150, Texture.BUG_3, Mood.ANGRY)
            )
        )
        val result = repository.saveSet(updated)

        assertEquals(created.id, (result as SkinSaveResult.Success).set.id)
        assertEquals(1, storage.sets.size)
        assertEquals(2, storage.sets[0].variants.size)
        assertEquals("Один", storage.sets[0].name)
    }

    @Test
    fun saveSet_rejectsSetBeyondLimit() {
        val storage = FakeSkinStorage()
        val repository = SkinRepository(storage)
        repeat(SkinRepository.MAX_SETS) { repository.saveSet(set()) }

        val result = repository.saveSet(set())

        assertTrue(result is SkinSaveResult.Failure)
        assertTrue((result as SkinSaveResult.Failure).errors[0].contains("max ${SkinRepository.MAX_SETS}"))
        assertEquals(SkinRepository.MAX_SETS, storage.sets.size)
    }

    @Test
    fun deleteActiveSet_resetsActiveToDefault() {
        val storage = FakeSkinStorage()
        val repository = SkinRepository(storage)
        val created = (repository.saveSet(set()) as SkinSaveResult.Success).set
        repository.setActive(created.id)

        repository.deleteSet(created.id)

        assertEquals(SkinSet.DEFAULT_ID, storage.activeId)
        assertEquals(SkinSet.DEFAULT, repository.activeSet())
    }

    @Test
    fun setActive_unknownId_isIgnored() {
        val storage = FakeSkinStorage()
        val repository = SkinRepository(storage)

        repository.setActive(999L)

        assertEquals(SkinSet.DEFAULT_ID, storage.activeId)
    }

    @Test
    fun activeSet_returnsStoredSetAfterSave() {
        val storage = FakeSkinStorage()
        val repository = SkinRepository(storage)
        val custom = SkinSet(0, "Мой", listOf(SkinVariant(-65281, 120, Texture.BUG_4, Mood.ANGRY)))
        val saved = (repository.saveSet(custom) as SkinSaveResult.Success).set
        repository.setActive(saved.id)

        assertEquals(saved, repository.activeSet())
    }

    @Test
    fun activeSet_survivesReloadThroughCodec() {
        val saved = (SkinRepository(FakeSkinStorage()).saveSet(set(name = "Долгий")) as SkinSaveResult.Success).set
        val document = SkinCodec.encode(listOf(saved))
        val storage = FakeSkinStorage().apply {
            sets.addAll(SkinCodec.decode(document))
            activeId = saved.id
        }

        assertEquals(saved, SkinRepository(storage).activeSet())
    }
}
