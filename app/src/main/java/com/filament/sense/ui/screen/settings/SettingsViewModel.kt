package com.filament.sense.ui.screen.settings

import android.content.SharedPreferences
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

private const val PREF_NOTIFICATIONS_ENABLED = "notifications_enabled"

data class SettingsUiState(
    val deviceName: String = "",
    val deviceState: DeviceState = DeviceState.DISCONNECTED,
    val notificationsEnabled: Boolean = true,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val deviceRepo: DeviceRepository,
    private val prefs: SharedPreferences,
) : ViewModel() {

    private val _state = MutableStateFlow(
        SettingsUiState(notificationsEnabled = prefs.getBoolean(PREF_NOTIFICATIONS_ENABLED, false))
    )
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
        prefs.edit().putBoolean(PREF_NOTIFICATIONS_ENABLED, enabled).apply()
        _state.value = _state.value.copy(notificationsEnabled = enabled)
    }
}