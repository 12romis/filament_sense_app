package com.filament.sense.ui.screen.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.filament.sense.domain.model.DeviceState
import com.filament.sense.domain.model.EnvData
import com.filament.sense.domain.model.SpoolSlot
import com.filament.sense.domain.repository.DeviceRepository
import com.filament.sense.domain.repository.SpoolRepository
import com.filament.sense.domain.usecase.GetSpoolsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val AUTO_RECONNECT_TIMEOUT_MS = 10_000L

data class HomeUiState(
    val deviceState: DeviceState = DeviceState.DISCONNECTED,
    val deviceName: String = "",
    val spools: List<SpoolSlot> = emptyList(),
    val activeSpool: SpoolSlot? = null,
    val envData: EnvData? = null,
    /** true = тихий auto-reconnect не вдався, показати CTA "Шукати пристрій" */
    val showReconnectCta: Boolean = false,
    /** true = є збережений MAC для прямого підключення */
    val hasLastMac: Boolean = false,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getSpools: GetSpoolsUseCase,
    private val deviceRepo: DeviceRepository,
    private val spoolRepo: SpoolRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    init {
        val hasLastMac = deviceRepo.lastConnectedMac != null

        // Тихе пряме підключення без сканування (якщо є відомий MAC і не ручне відключення)
        if (hasLastMac && deviceRepo.deviceState.value == DeviceState.DISCONNECTED
            && !deviceRepo.wasManualDisconnect) {
            deviceRepo.autoConnect()
            viewModelScope.launch {
                delay(AUTO_RECONNECT_TIMEOUT_MS)
                if (_state.value.deviceState == DeviceState.DISCONNECTED) {
                    _state.value = _state.value.copy(showReconnectCta = true)
                }
            }
        }

        viewModelScope.launch {
            spoolRepo.envData.collect { env ->
                _state.value = _state.value.copy(envData = env)
            }
        }
        viewModelScope.launch {
            combine(
                deviceRepo.deviceState,
                deviceRepo.deviceName,
                getSpools(),
            ) { deviceState, deviceName, spools ->
                Triple(deviceState, deviceName, spools)
            }.collect { (deviceState, deviceName, spools) ->
                val current = _state.value
                // Несподіваний розрив — одразу намагаємось перепідключитись (не при ручному відключенні)
                if (deviceState == DeviceState.DISCONNECTED &&
                    current.deviceState == DeviceState.CONNECTED &&
                    hasLastMac && !deviceRepo.wasManualDisconnect) {
                    deviceRepo.autoConnect()
                    viewModelScope.launch {
                        delay(AUTO_RECONNECT_TIMEOUT_MS)
                        if (_state.value.deviceState == DeviceState.DISCONNECTED) {
                            _state.value = _state.value.copy(showReconnectCta = true)
                        }
                    }
                }
                _state.value = current.copy(
                    deviceState = deviceState,
                    deviceName = deviceName,
                    spools = spools,
                    activeSpool = spools.firstOrNull { it.isActive },
                    // При успішному підключенні — прибрати CTA
                    showReconnectCta = if (deviceState == DeviceState.CONNECTED) false
                                      else current.showReconnectCta,
                    hasLastMac = hasLastMac,
                )
            }
        }
    }

    fun triggerReconnect() {
        if (deviceRepo.lastConnectedMac != null) {
            deviceRepo.autoConnect()
        }
    }
}
