package com.filament.sense.ui.screen.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.filament.sense.domain.model.DeviceState
import com.filament.sense.domain.repository.DeviceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val deviceName: String = "",
    val deviceState: DeviceState = DeviceState.DISCONNECTED,
    val notificationsEnabled: Boolean = true,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val deviceRepo: DeviceRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            deviceRepo.deviceState.collect { state ->
                _state.value = _state.value.copy(deviceState = state)
            }
        }
        viewModelScope.launch {
            deviceRepo.deviceName.collect { name ->
                _state.value = _state.value.copy(deviceName = name)
            }
        }
    }

    fun disconnect() {
        viewModelScope.launch { deviceRepo.disconnect() }
    }

    fun toggleNotifications(enabled: Boolean) {
        _state.value = _state.value.copy(notificationsEnabled = enabled)
    }
}