package com.filament.sense.ui.screen.spools

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.filament.sense.domain.usecase.DeleteSpoolUseCase
import com.filament.sense.domain.usecase.GetSpoolsUseCase
import com.filament.sense.domain.usecase.UpdateSpoolConfigUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SpoolEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getSpools: GetSpoolsUseCase,
    private val updateSpoolConfig: UpdateSpoolConfigUseCase,
    private val deleteSpool: DeleteSpoolUseCase,
) : ViewModel() {

    private val id: Int = checkNotNull(savedStateHandle["id"])

    private val _state = MutableStateFlow(SpoolFormUiState())
    val state: StateFlow<SpoolFormUiState> = _state.asStateFlow()

    private val _navigateBack = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val navigateBack: SharedFlow<Unit> = _navigateBack.asSharedFlow()

    init {
        viewModelScope.launch {
            val spool = getSpools().first().find { it.id == id } ?: return@launch
            _state.value = SpoolFormUiState(
                name = spool.name,
                colorArgb = spool.colorArgb,
                nominalWeightGrams = spool.nominalWeightGrams,
                baselineWeightGrams = spool.baselineWeight,
                isActive = spool.isActive,
            )
        }
    }

    fun onNameChange(value: String) { _state.value = _state.value.copy(name = value) }
    fun onColorChange(argb: Int) { _state.value = _state.value.copy(colorArgb = argb) }
    fun onNominalWeightChange(value: Int) { _state.value = _state.value.copy(nominalWeightGrams = value) }
    fun onBaselineWeightChange(value: Float) { _state.value = _state.value.copy(baselineWeightGrams = value) }

    fun save() {
        val s = _state.value
        viewModelScope.launch {
            updateSpoolConfig(
                id = id,
                name = s.name,
                colorArgb = s.colorArgb,
                nominalWeight = s.nominalWeightGrams,
                baselineWeight = s.baselineWeightGrams,
            )
            _navigateBack.emit(Unit)
        }
    }

    fun delete() {
        viewModelScope.launch { deleteSpool(id) }
    }
}
