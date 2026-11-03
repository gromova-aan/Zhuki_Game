package com.example.zhuki_game

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class SkinsEditorViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private class FakeSkinRepository : SkinRepository(object : SkinStorage {
        override fun getAll(): List<SkinSet> = emptyList()
        override fun save(set: SkinSet) = Unit
        override fun delete(id: Long) = Unit
        override fun getActiveSetId(): Long = SkinSet.DEFAULT_ID
        override fun setActiveSetId(id: Long) = Unit
    }) {
        val storedSets = mutableListOf<SkinSet>()
        var activeId = SkinSet.DEFAULT_ID
        var saveCalls = 0

        override fun getSets(): List<SkinSet> = storedSets.toList()

        override fun activeSet(): SkinSet =
            if (activeId == SkinSet.DEFAULT_ID) {
                SkinSet.DEFAULT
            } else {
                storedSets.firstOrNull { it.id == activeId } ?: SkinSet.DEFAULT
            }

        override fun setActive(id: Long) {
            activeId = id
        }

        override fun saveSet(set: SkinSet): SkinSaveResult {
            saveCalls++
            val errors = set.errors()
            if (errors.isNotEmpty()) return SkinSaveResult.Failure(errors)

            val isNew = storedSets.none { it.id == set.id }
            val saved = if (isNew) {
                set.copy(id = (storedSets.maxOfOrNull { it.id } ?: 0L) + 1)
            } else {
                set
            }
            val index = storedSets.indexOfFirst { it.id == saved.id }
            if (index >= 0) {
                storedSets[index] = saved
            } else {
                storedSets.add(saved)
            }
            return SkinSaveResult.Success(saved)
        }

        override fun deleteSet(id: Long) {
            storedSets.removeAll { it.id == id }
            if (activeId == id) activeId = SkinSet.DEFAULT_ID
        }

        fun addSet(
            name: String,
            variants: List<SkinVariant> = listOf(SkinSet.DEFAULT_VARIANT)
        ): SkinSet {
            val result = saveSet(SkinSet(id = 0L, name = name, variants = variants))
            saveCalls = 0
            return (result as SkinSaveResult.Success).set
        }
    }

    private lateinit var repository: FakeSkinRepository
    private lateinit var viewModel: SkinsEditorViewModel

    @Before
    fun setUp() {
        repository = FakeSkinRepository()
        viewModel = SkinsEditorViewModel(repository)
    }

    private fun editableSet(
        name: String = "Мой набор",
        variants: List<SkinVariant> = listOf(SkinSet.DEFAULT_VARIANT)
    ): SkinSet {
        val set = repository.addSet(name, variants)
        viewModel = SkinsEditorViewModel(repository)
        viewModel.selectSet(set.id)
        return set
    }

    @Test
    fun newVariant_isAddedToDraftAndSaved() {
        editableSet()

        viewModel.startNewVariant()

        assertEquals(-1, viewModel.editingIndex.value)
        assertEquals(SkinVariant.MIN_SIZE_PERCENT, viewModel.draft.value!!.sizePercent)
        assertEquals(1, viewModel.variants.value!!.size)

        viewModel.updateDraftSize(120)
        viewModel.updateDraftTexture(Texture.BUG_2)
        viewModel.saveForm("Мой набор")

        val saved = repository.storedSets.single()
        assertEquals(2, saved.variants.size)
        assertEquals(120, saved.variants[1].sizePercent)
        assertEquals(Texture.BUG_2, saved.variants[1].texture)
        assertEquals(1, viewModel.editingIndex.value)
        assertEquals(saved.variants[1], viewModel.draft.value)
    }

    @Test
    fun deleteVariant_whenOnlyOne_isForbidden() {
        val set = editableSet()
        repository.saveCalls = 0

        viewModel.deleteVariant()

        assertEquals(R.string.skin_error_last_variant, viewModel.messageRes.value)
        assertEquals(0, repository.saveCalls)
        assertEquals(set, repository.storedSets.single())
    }

    @Test
    fun saveForm_withBlankName_doesNotCallRepository() {
        val set = editableSet()
        repository.saveCalls = 0

        viewModel.saveForm("   ")

        assertEquals(R.string.skin_error_name, viewModel.messageRes.value)
        assertEquals(0, repository.saveCalls)
        assertEquals(set, repository.storedSets.single())
    }

    @Test
    fun newSet_atLimit_showsError() {
        repeat(SkinRepository.MAX_SETS) { repository.addSet("Набор $it") }
        viewModel = SkinsEditorViewModel(repository)
        repository.saveCalls = 0

        viewModel.startNewSet()

        assertEquals(R.string.skin_error_sets_limit, viewModel.messageRes.value)
        assertEquals(0, repository.saveCalls)
        assertEquals(SkinRepository.MAX_SETS, repository.storedSets.size)
    }

    @Test
    fun newVariant_atLimit_showsError() {
        val variants = List(SkinSet.MAX_VARIANTS) {
            SkinVariant(null, 100, Texture.RANDOM, Mood.FRIENDLY)
        }
        editableSet(variants = variants)
        repository.saveCalls = 0

        viewModel.startNewVariant()

        assertEquals(R.string.skin_error_variants_limit, viewModel.messageRes.value)
        assertEquals(0, repository.saveCalls)
        assertEquals(SkinSet.MAX_VARIANTS, repository.storedSets.single().variants.size)
    }

    @Test
    fun applyActive_changesActiveSet() {
        val set = editableSet()

        viewModel.applyActive()

        assertEquals(set.id, repository.activeId)
        assertEquals(set.id, repository.activeSet().id)
        assertEquals(set.id, viewModel.activeSetId.value)
        assertEquals(R.string.skin_applied, viewModel.messageRes.value)
    }
}
