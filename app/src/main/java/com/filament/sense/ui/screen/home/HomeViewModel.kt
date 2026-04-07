package com.filament.sense.ui.screen.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.filament.sense.domain.model.DeviceState
import com.filament.sense.domain.model.EnvData
import com.filament.sense.domain.model.SpoolSlot
import com.filament.sense.domain.repository.DeviceRepository
import com.filament.sense.domain.usecase.GetSpoolsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val deviceState: DeviceState = DeviceState.DISCONNECTED,
    val deviceName: String = "",
    val spools: List<SpoolSlot> = emptyList(),
    val activeSpool: SpoolSlot? = null,
    val envData: EnvData? = null,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getSpools: GetSpoolsUseCase,
    private val deviceRepo: DeviceRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                deviceRepo.deviceState,
                deviceRepo.deviceName,
                getSpools(),
            ) { deviceState, deviceName, spools ->
                HomeUiState(
                    deviceState = deviceState,
                    deviceName = deviceName,
                    spools = spools,
                    activeSpool = spools.firstOrNull { it.isActive },
                )
            }.collect { _state.value = it }
        }
    }
}