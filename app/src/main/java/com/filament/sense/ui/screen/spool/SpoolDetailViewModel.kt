package com.filament.sense.ui.screen.spool

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.filament.sense.domain.model.Measurement
import com.filament.sense.domain.model.SpoolSlot
import com.filament.sense.domain.usecase.GetMeasurementsUseCase
import com.filament.sense.domain.usecase.GetSpoolByIdUseCase
import com.filament.sense.domain.usecase.GetThresholdsUseCase
import com.filament.sense.domain.usecase.SetActiveSpoolUseCase
import com.filament.sense.domain.usecase.SendManualReportUseCase
import com.filament.sense.domain.usecase.SetBaselineUseCase
import com.filament.sense.domain.usecase.SetThresholdsUseCase
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
    val thresholdWarning: Int = 500,
    val thresholdCritical: Int = 100,
    val thresholdEmpty: Int = 10,
    val snackbarMessage: String? = null,
)

@HiltViewModel
class SpoolDetailViewModel @Inject constructor(
    private val getSpoolById: GetSpoolByIdUseCase,
    private val getMeasurements: GetMeasurementsUseCase,
    private val getThresholds: GetThresholdsUseCase,
    private val setActiveSpool: SetActiveSpoolUseCase,
    private val setBaseline: SetBaselineUseCase,
    private val sendManualReport: SendManualReportUseCase,
    private val setThresholds: SetThresholdsUseCase,
    private val updateConfig: UpdateSpoolConfigUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(SpoolDetailUiState())
    val state: StateFlow<SpoolDetailUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            getThresholds().collect { (w, c, e) ->
                _state.value = _state.value.copy(
                    thresholdWarning = w,
                    thresholdCritical = c,
                    thresholdEmpty = e,
                )
            }
        }
    }

    fun loadSpool(id: Int) {
        viewModelScope.launch {
            getSpoolById(id).collect { spool ->
                _state.value = _state.value.copy(spool = spool)
            }
        }
        viewModelScope.launch {
            getMeasurements(id).collect { list ->
                _state.value = _state.value.copy(measurements = list)
            }
        }
    }

    fun onToggleActive(id: Int, currentlyActive: Boolean) {
        if (!currentlyActive) {
            _state.value = _state.value.copy(showSetActiveDialog = true)
        }
    }

    fun confirmSetActive() {
        val id = _state.value.spool?.id ?: return
        viewModelScope.launch {
            setActiveSpool(id)
            _state.value = _state.value.copy(showSetActiveDialog = false)
        }
    }

    fun dismissDialog() {
        _state.value = _state.value.copy(showSetActiveDialog = false)
    }

    fun updateThresholds(warning: Int, critical: Int, empty: Int) {
        _state.value = _state.value.copy(
            thresholdWarning = warning,
            thresholdCritical = critical,
            thresholdEmpty = empty,
        )
        viewModelScope.launch { setThresholds(warning, critical, empty) }
    }

    fun saveBaseline(id: Int) {
        viewModelScope.launch {
            setBaseline(id)
            _state.value = _state.value.copy(snackbarMessage = "Команду надіслано на пристрій")
        }
    }

    fun sendReport() {
        viewModelScope.launch {
            sendManualReport()
            _state.value = _state.value.copy(snackbarMessage = "Команду надіслано на пристрій")
        }
    }

    fun clearSnackbar() {
        _state.value = _state.value.copy(snackbarMessage = null)
    }

    fun updateSpoolConfig(id: Int, name: String, colorArgb: Int, nominalWeight: Int, baselineWeight: Float) {
        viewModelScope.launch {
            updateConfig(id, name, colorArgb, nominalWeight, baselineWeight)
        }
    }
}
