package com.filament.sense.ui.screen.spools

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.filament.sense.domain.usecase.CreateSpoolUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SpoolFormUiState(
    val name: String = "",
    val colorArgb: Int = Color.White.toArgb(),
    val nominalWeightGrams: Int = 1000,
    val baselineWeightGrams: Float = 0f,
    val isActive: Boolean = false,
)

@HiltViewModel
class SpoolCreateViewModel @Inject constructor(
    private val createSpool: CreateSpoolUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(SpoolFormUiState())
    val state: StateFlow<SpoolFormUiState> = _state.asStateFlow()

    fun onNameChange(value: String) { _state.value = _state.value.copy(name = value) }
    fun onColorChange(argb: Int) { _state.value = _state.value.copy(colorArgb = argb) }
    fun onNominalWeightChange(value: Int) { _state.value = _state.value.copy(nominalWeightGrams = value) }
    fun onBaselineWeightChange(value: Float) { _state.value = _state.value.copy(baselineWeightGrams = value) }

    fun save() {
        val s = _state.value
        viewModelScope.launch {
            createSpool(
                name = s.name,
                colorArgb = s.colorArgb,
                nominalWeight = s.nominalWeightGrams,
                baselineWeight = s.baselineWeightGrams,
            )
        }
    }
}
