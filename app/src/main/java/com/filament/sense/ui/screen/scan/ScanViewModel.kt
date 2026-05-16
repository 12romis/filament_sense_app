package com.filament.sense.ui.screen.scan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.filament.sense.data.ble.BleManager
import com.filament.sense.domain.model.DeviceState
import com.filament.sense.domain.model.SpoolSlot
import com.filament.sense.domain.repository.DeviceRepository
import com.filament.sense.domain.repository.SpoolRepository
import com.welie.blessed.BluetoothPeripheral
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ScanUiState(
    val isScanning: Boolean = false,
    val isConnecting: Boolean = false,
    val devices: List<ScannedDevice> = emptyList(),
    val activeSpool: SpoolSlot? = null,
    val scanError: String? = null,
    val debugInfo: String = "",
    /** Пристрій, підключення до якого очікує підтвердження в діалозі. */
    val pendingConnectDevice: ScannedDevice? = null,
)

data class ScannedDevice(
    val address: String,
    val name: String,
    val rssi: Int,
    val isFilamentSense: Boolean,
    val peripheral: BluetoothPeripheral,
)

@HiltViewModel
class ScanViewModel @Inject constructor(
    private val deviceRepo: DeviceRepository,
    private val bleManager: BleManager,
    private val spoolRepo: SpoolRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ScanUiState())
    val state: StateFlow<ScanUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            deviceRepo.deviceState.collect { deviceState ->
                _state.value = _state.value.copy(
                    isScanning = deviceState == DeviceState.SCANNING,
                    isConnecting = deviceState == DeviceState.CONNECTING,
                )
            }
        }
        viewModelScope.launch {
            bleManager.scanResults.collect { event ->
                val existing = _state.value.devices
                if (existing.none { it.address == event.peripheral.address }) {
                    val name = event.peripheral.name
                        ?: if (event.isFilamentSense) com.filament.sense.data.ble.GattConstants.DEVICE_NAME else null
                    val device = ScannedDevice(
                        address = event.peripheral.address,
                        name = name ?: event.peripheral.address,
                        rssi = -70,
                        isFilamentSense = event.isFilamentSense,
                        peripheral = event.peripheral,
                    )
                    _state.value = _state.value.copy(devices = existing + device)
                }
            }
        }
        viewModelScope.launch {
            bleManager.scanFailure.collect { error ->
                _state.value = _state.value.copy(scanError = error)
            }
        }
        viewModelScope.launch {
            bleManager.debugInfo.collect { info ->
                _state.value = _state.value.copy(debugInfo = info)
            }
        }
        viewModelScope.launch {
            spoolRepo.spools.collect { spools ->
                _state.value = _state.value.copy(activeSpool = spools.firstOrNull { it.isActive })
            }
        }
    }

    /** Ініціює з'єднання: спочатку показує діалог підтвердження. */
    fun requestConnect(device: ScannedDevice) {
        _state.value = _state.value.copy(pendingConnectDevice = device)
    }

    /** Підтверджує підключення після діалогу. */
    fun confirmConnect() {
        val device = _state.value.pendingConnectDevice ?: return
        _state.value = _state.value.copy(pendingConnectDevice = null)
        viewModelScope.launch { deviceRepo.connectToDevice(device.address) }
    }

    /** Скасовує діалог підтвердження. */
    fun cancelConnect() {
        _state.value = _state.value.copy(pendingConnectDevice = null)
    }

    fun startScan() {
        _state.value = _state.value.copy(devices = emptyList())
        viewModelScope.launch { deviceRepo.startScan() }
    }

    fun stopScan() {
        deviceRepo.stopScan()
    }

    override fun onCleared() {
        super.onCleared()
        deviceRepo.stopScan()
    }
}
