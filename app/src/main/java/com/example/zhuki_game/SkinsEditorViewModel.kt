package com.example.zhuki_game

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel


//состояние редактора скинов: список наборов, выбранный набор, его варианты и черновик варианта
//живёт дольше экрана, поэтому переживает поворот

class SkinsEditorViewModel(private val skinRepository: SkinRepository) : ViewModel() {

    private val _sets = MutableLiveData<List<SkinSet>>(emptyList())
    val sets: LiveData<List<SkinSet>> = _sets

    private val _activeSetId = MutableLiveData(SkinSet.DEFAULT_ID)
    val activeSetId: LiveData<Long> = _activeSetId

    private val _selectedSetId = MutableLiveData(SkinSet.DEFAULT_ID)
    val selectedSetId: LiveData<Long> = _selectedSetId

    private val _variants = MutableLiveData<List<SkinVariant>>(emptyList())
    val variants: LiveData<List<SkinVariant>> = _variants

    private val _draft = MutableLiveData(SkinSet.DEFAULT_VARIANT)
    val draft: LiveData<SkinVariant> = _draft

    private val _editingIndex = MutableLiveData(-1)
    val editingIndex: LiveData<Int> = _editingIndex

    private val _messageRes = MutableLiveData<Int?>()
    val messageRes: LiveData<Int?> = _messageRes

    init {
        refresh()
        selectSet(_activeSetId.value ?: SkinSet.DEFAULT_ID)
    }

    // чтение состояния

    private fun refresh() {
        _sets.value = listOf(SkinSet.DEFAULT) + skinRepository.getSets()
        _activeSetId.value = skinRepository.activeSet().id
    }

    private fun currentSet(): SkinSet? {
        val id = _selectedSetId.value ?: SkinSet.DEFAULT_ID
        return _sets.value?.firstOrNull { it.id == id }
            ?: if (id == SkinSet.DEFAULT_ID) SkinSet.DEFAULT else null
    }

    private fun draftValue(): SkinVariant = _draft.value ?: SkinSet.DEFAULT_VARIANT

    // наборы

    fun selectSet(id: Long) {
        val set = _sets.value?.firstOrNull { it.id == id }
            ?: if (id == SkinSet.DEFAULT_ID) SkinSet.DEFAULT else return

        _selectedSetId.value = set.id
        _variants.value = set.variants
        _editingIndex.value = if (set.variants.isNotEmpty()) 0 else -1
        _draft.value = set.variants.firstOrNull() ?: SkinSet.DEFAULT_VARIANT
    }

    fun startNewSet() {
        if (skinRepository.getSets().size >= SkinRepository.MAX_SETS) {
            _messageRes.value = R.string.skin_error_sets_limit
            return
        }

        val newSet = SkinSet(
            id = 0L,
            name = "Новый набор",
            variants = listOf(SkinSet.DEFAULT_VARIANT)
        )

        when (val result = skinRepository.saveSet(newSet)) {
            is SkinSaveResult.Success -> {
                refresh()
                selectSet(result.set.id)
                _messageRes.value = R.string.skin_set_created
            }
            is SkinSaveResult.Failure -> _messageRes.value = R.string.skin_error_save
        }
    }

    fun deleteSet() {
        val set = currentSet() ?: return
        if (set.id == SkinSet.DEFAULT_ID) {
            _messageRes.value = R.string.skin_error_readonly
            return
        }

        skinRepository.deleteSet(set.id)
        refresh()
        selectSet(SkinSet.DEFAULT_ID)
        _messageRes.value = R.string.skin_set_deleted
    }

    // варианты
    fun startNewVariant() {
        val set = currentSet() ?: return
        if (set.id == SkinSet.DEFAULT_ID) {
            _messageRes.value = R.string.skin_error_readonly
            return
        }
        if (set.variants.size >= SkinSet.MAX_VARIANTS) {
            _messageRes.value = R.string.skin_error_variants_limit
            return
        }

        _editingIndex.value = -1
        _draft.value = SkinSet.DEFAULT_VARIANT.copy(
            sizePercent = SkinVariant.MIN_SIZE_PERCENT
        )
    }

    fun editVariant(index: Int) {
        val list = _variants.value ?: return
        if (index !in list.indices) return

        _editingIndex.value = index
        _draft.value = list[index]
    }

    fun updateDraftColor(color: Int?) {
        _draft.value = draftValue().copy(color = color)
    }

    fun updateDraftSize(sizePercent: Int) {
        _draft.value = draftValue().copy(sizePercent = sizePercent)
    }

    fun updateDraftTexture(texture: Texture) {
        _draft.value = draftValue().copy(texture = texture)
    }

    fun updateDraftMood(mood: Mood) {
        _draft.value = draftValue().copy(mood = mood)
    }

    fun saveForm(name: String) {
        val set = currentSet() ?: return
        if (set.id == SkinSet.DEFAULT_ID) {
            _messageRes.value = R.string.skin_error_readonly
            return
        }

        val variant = draftValue()
        if (!variant.isValid()) {
            _messageRes.value = R.string.skin_error_variant
            return
        }
        if (name.trim().isEmpty()) {
            _messageRes.value = R.string.skin_error_name
            return
        }

        val variants = set.variants.toMutableList()
        val index = _editingIndex.value ?: -1
        val targetIndex: Int

        if (index in variants.indices) {
            variants[index] = variant
            targetIndex = index
        } else {
            if (variants.size >= SkinSet.MAX_VARIANTS) {
                _messageRes.value = R.string.skin_error_variants_limit
                return
            }
            variants.add(variant)
            targetIndex = variants.lastIndex
        }

        val updated = set.copy(name = name.trim(), variants = variants)

        when (val result = skinRepository.saveSet(updated)) {
            is SkinSaveResult.Success -> {
                refresh()
                _selectedSetId.value = result.set.id
                _variants.value = result.set.variants
                _editingIndex.value = targetIndex
                _draft.value = result.set.variants.getOrNull(targetIndex) ?: variant
                _messageRes.value = R.string.skin_saved
            }
            is SkinSaveResult.Failure -> _messageRes.value = R.string.skin_error_name
        }
    }

    fun deleteVariant() {
        val set = currentSet() ?: return
        if (set.id == SkinSet.DEFAULT_ID) {
            _messageRes.value = R.string.skin_error_readonly
            return
        }

        val index = _editingIndex.value ?: -1
        if (index !in set.variants.indices) return

        if (set.variants.size <= 1) {
            _messageRes.value = R.string.skin_error_last_variant
            return
        }

        val variants = set.variants.toMutableList().also { it.removeAt(index) }

        when (val result = skinRepository.saveSet(set.copy(variants = variants))) {
            is SkinSaveResult.Success -> {
                refresh()
                _selectedSetId.value = result.set.id
                _variants.value = result.set.variants
                _editingIndex.value = if (result.set.variants.isEmpty()) -1 else 0
                _draft.value = result.set.variants.firstOrNull() ?: SkinSet.DEFAULT_VARIANT
                _messageRes.value = R.string.skin_variant_deleted
            }
            is SkinSaveResult.Failure -> _messageRes.value = R.string.skin_error_save
        }
    }

    // применение

    fun applyActive() {
        val id = _selectedSetId.value ?: return
        skinRepository.setActive(id)
        refresh()
        _messageRes.value = R.string.skin_applied
    }

    fun consumeMessage() {
        _messageRes.value = null
    }
}
