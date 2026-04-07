package com.filament.sense.ui.screen.scan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.filament.sense.data.ble.BleManager
import com.filament.sense.domain.model.DeviceState
import com.filament.sense.domain.repository.DeviceRepository
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
            bleManager.scanResults.collect { peripheral ->
                val existing = _state.value.devices
                val alreadyAdded = existing.any { it.address == peripheral.address }
                if (!alreadyAdded) {
                    val device = ScannedDevice(
                        address = peripheral.address,
                        name = peripheral.name ?: "Unknown",
                        rssi = -70, // rssi недоступний з BluetoothPeripheral; ScanResult не проксується через BleManager
                        isFilamentSense = peripheral.name == com.filament.sense.data.ble.GattConstants.DEVICE_NAME,
                        peripheral = peripheral,
                    )
                    _state.value = _state.value.copy(devices = existing + device)
                }
            }
        }
    }

    fun startScan() {
        _state.value = _state.value.copy(devices = emptyList())
        viewModelScope.launch { deviceRepo.startScan() }
    }

    fun connect(device: ScannedDevice) {
        viewModelScope.launch { deviceRepo.connectToDevice(device.address) }
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch { deviceRepo.stopScan() }
    }
}