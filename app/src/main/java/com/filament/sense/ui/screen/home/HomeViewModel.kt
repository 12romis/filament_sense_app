package com.filament.sense.ui.screen.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.filament.sense.domain.model.DeviceState
import com.filament.sense.domain.model.EnvData
import com.filament.sense.domain.model.Measurement
import com.filament.sense.domain.model.SpoolSlot
import com.filament.sense.domain.repository.DeviceRepository
import com.filament.sense.domain.repository.SpoolRepository
import com.filament.sense.domain.usecase.GetMeasurementsUseCase
import com.filament.sense.domain.usecase.GetSpoolsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Час очікування успішного підключення. Якщо CONNECTING довше — скасовуємо. */
private const val CONNECT_TIMEOUT_MS = 15_000L

data class HomeUiState(
    val deviceState: DeviceState = DeviceState.DISCONNECTED,
    val deviceName: String = "",
    val spools: List<SpoolSlot> = emptyList(),
    val activeSpool: SpoolSlot? = null,
    val envData: EnvData? = null,
    val activeMeasurements: List<Measurement> = emptyList(),
    /** true = тихий auto-reconnect не вдався, показати CTA "Шукати пристрій" */
    val showReconnectCta: Boolean = false,
    /** true = є збережений MAC для прямого підключення */
    val hasLastMac: Boolean = false,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getSpools: GetSpoolsUseCase,
    private val getMeasurements: GetMeasurementsUseCase,
    private val deviceRepo: DeviceRepository,
    private val spoolRepo: SpoolRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    /** Job таймауту підключення — скасовується при успішному connect або новому виклику. */
    private var connectTimeoutJob: Job? = null

    init {
        val hasLastMac = deviceRepo.lastConnectedMac != null

        // Тихе пряме підключення без сканування (якщо є відомий MAC і не ручне відключення)
        if (hasLastMac && deviceRepo.deviceState.value == DeviceState.DISCONNECTED
            && !deviceRepo.wasManualDisconnect) {
            startAutoConnect()
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

                when {
                    // Успішне підключення — скасовуємо таймаут
                    deviceState == DeviceState.CONNECTED -> {
                        connectTimeoutJob?.cancel()
                        connectTimeoutJob = null
                    }
                    // Несподіваний розрив із CONNECTED → авторепідключення
                    deviceState == DeviceState.DISCONNECTED &&
                    current.deviceState == DeviceState.CONNECTED &&
                    hasLastMac && !deviceRepo.wasManualDisconnect -> {
                        startAutoConnect()
                    }
                }

                _state.value = current.copy(
                    deviceState = deviceState,
                    deviceName = deviceName,
                    spools = spools,
                    activeSpool = spools.firstOrNull { it.isActive },
                    showReconnectCta = if (deviceState == DeviceState.CONNECTED) false
                                      else current.showReconnectCta,
                    hasLastMac = hasLastMac,
                )
            }
        }

        // Відстежуємо активну котушку; при зміні перепідписуємось на її bucketed measurements
        viewModelScope.launch {
            @Suppress("OPT_IN_USAGE")
            getSpools()
                .map { spools -> spools.firstOrNull { it.isActive }?.id }
                .distinctUntilChanged()
                .flatMapLatest { spoolId ->
                    if (spoolId == null) flowOf(emptyList())
                    else getMeasurements(spoolId)
                }
                .collect { measurements ->
                    _state.value = _state.value.copy(activeMeasurements = measurements)
                }
        }
    }

    /**
     * Запускає autoConnect + таймаут 15 с.
     * Якщо за цей час стан не перейшов у CONNECTED — скасовуємо підключення,
     * BleManager викличе onDisconnectedPeripheral → стан → DISCONNECTED → показуємо CTA.
     */
    private fun startAutoConnect() {
        connectTimeoutJob?.cancel()
        deviceRepo.autoConnect()
        connectTimeoutJob = viewModelScope.launch {
            delay(CONNECT_TIMEOUT_MS)
            if (_state.value.deviceState == DeviceState.CONNECTING) {
                deviceRepo.cancelConnection()
                _state.value = _state.value.copy(showReconnectCta = true)
            }
        }
    }

    fun triggerReconnect() {
        if (deviceRepo.lastConnectedMac != null) {
            startAutoConnect()
        }
    }
}
