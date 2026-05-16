package com.filament.sense.ui.screen.settings

import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.filament.sense.domain.model.DeviceState
import com.filament.sense.domain.repository.DeviceRepository
import com.filament.sense.domain.repository.SpoolRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val PREF_NOTIFICATIONS_ENABLED = "notifications_enabled"

data class SettingsUiState(
    val deviceName: String = "",
    val deviceState: DeviceState = DeviceState.DISCONNECTED,
    val mac: String? = null,
    val notificationsEnabled: Boolean = false,
    /** Значення mqtt_host, зчитане з CONFIG-характеристики пристрою. */
    val mqttHostFromDevice: String = "",
    /** Поточний текст у полі вводу (редагований користувачем). */
    val draftMqttHost: String = "",
    val isMqttHostDirty: Boolean = false,
    val snackbarMessage: String? = null,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val deviceRepo: DeviceRepository,
    private val spoolRepo: SpoolRepository,
    private val prefs: SharedPreferences,
) : ViewModel() {

    private val _state = MutableStateFlow(
        SettingsUiState(notificationsEnabled = prefs.getBoolean(PREF_NOTIFICATIONS_ENABLED, false))
    )
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                deviceRepo.deviceState,
                deviceRepo.deviceName,
                spoolRepo.configData,
            ) { deviceState, deviceName, config ->
                Triple(deviceState, deviceName, config)
            }.collect { (deviceState, deviceName, config) ->
                val current = _state.value
                val mqttFromDevice = config?.mqttHost ?: ""
                _state.value = current.copy(
                    deviceState = deviceState,
                    deviceName = deviceName,
                    mac = deviceRepo.lastConnectedMac,
                    mqttHostFromDevice = mqttFromDevice,
                    // Якщо ще не редагував — підтягнути з пристрою
                    draftMqttHost = if (!current.isMqttHostDirty) mqttFromDevice else current.draftMqttHost,
                )
            }
        }
    }

    fun onMqttHostDraftChange(value: String) {
        _state.value = _state.value.copy(
            draftMqttHost = value,
            isMqttHostDirty = value != _state.value.mqttHostFromDevice,
        )
    }

    fun saveMqttHost() {
        val host = _state.value.draftMqttHost.trim()
        viewModelScope.launch {
            spoolRepo.setMqttHost(host)
            _state.value = _state.value.copy(
                isMqttHostDirty = false,
                mqttHostFromDevice = host,
                snackbarMessage = "Надіслано на пристрій",
            )
        }
    }

    fun disconnect() {
        viewModelScope.launch { deviceRepo.disconnect() }
    }

    fun reconnect() {
        deviceRepo.autoConnect()
    }

    fun toggleNotifications(enabled: Boolean) {
        prefs.edit().putBoolean(PREF_NOTIFICATIONS_ENABLED, enabled).apply()
        _state.value = _state.value.copy(notificationsEnabled = enabled)
    }

    fun clearSnackbar() {
        _state.value = _state.value.copy(snackbarMessage = null)
    }
}
