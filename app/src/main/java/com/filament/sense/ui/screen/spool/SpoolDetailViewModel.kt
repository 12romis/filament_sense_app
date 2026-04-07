package com.filament.sense.ui.screen.spool

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.filament.sense.domain.model.Measurement
import com.filament.sense.domain.model.SpoolSlot
import com.filament.sense.domain.usecase.GetMeasurementsUseCase
import com.filament.sense.domain.usecase.GetSpoolsUseCase
import com.filament.sense.domain.usecase.SetActiveSpoolUseCase
import com.filament.sense.domain.usecase.SetBaselineUseCase
import com.filament.sense.domain.usecase.UpdateSpoolConfigUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SpoolDetailUiState(
    val spool: SpoolSlot? = null,
    val measurements: List<Measurement> = emptyList(),
    val showSetActiveDialog: Boolean = false,
)

@HiltViewModel
class SpoolDetailViewModel @Inject constructor(
    private val getSpools: GetSpoolsUseCase,
    private val getMeasurements: GetMeasurementsUseCase,
    private val setActiveSpool: SetActiveSpoolUseCase,
    private val setBaseline: SetBaselineUseCase,
    private val updateConfig: UpdateSpoolConfigUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(SpoolDetailUiState())
    val state: StateFlow<SpoolDetailUiState> = _state.asStateFlow()

    fun loadSpool(index: Int) {
        viewModelScope.launch {
            getSpools().collect { spools ->
                _state.value = _state.value.copy(spool = spools.getOrNull(index))
            }
        }
        // Завантажуємо вимірювання за останні 24 год
        viewModelScope.launch {
            getMeasurements(index).collect { list ->
                _state.value = _state.value.copy(measurements = list)
            }
        }
    }

    fun onToggleActive(index: Int, currentlyActive: Boolean) {
        if (!currentlyActive) {
            _state.value = _state.value.copy(showSetActiveDialog = true)
        }
    }

    fun confirmSetActive() {
        val index = _state.value.spool?.index ?: return
        viewModelScope.launch {
            setActiveSpool(index)
            _state.value = _state.value.copy(showSetActiveDialog = false)
        }
    }

    fun dismissDialog() {
        _state.value = _state.value.copy(showSetActiveDialog = false)
    }

    fun saveBaseline(index: Int) {
        viewModelScope.launch { setBaseline(index) }
    }

    fun updateSpoolConfig(index: Int, name: String, colorArgb: Int, nominalWeight: Int, baselineWeight: Float) {
        viewModelScope.launch {
            updateConfig(index, name, colorArgb, nominalWeight, baselineWeight)
        }
    }
}