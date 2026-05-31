package com.filament.sense.ui.screen.printer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.filament.sense.data.ble.BleDataParser
import com.filament.sense.data.ble.BleManager
import com.filament.sense.domain.model.DeviceState
import com.filament.sense.domain.model.PrinterStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PrinterViewModel @Inject constructor(
    private val bleManager: BleManager,
) : ViewModel() {

    val deviceState: StateFlow<DeviceState> = bleManager.deviceState
    val printerStatus: StateFlow<PrinterStatus?> = bleManager.printerStatus

    private val _lastSyncTime = MutableStateFlow<Long?>(null)
    val lastSyncTime: StateFlow<Long?> = _lastSyncTime.asStateFlow()

    init {
        viewModelScope.launch {
            bleManager.printerStatus.collect { status ->
                if (status != null) _lastSyncTime.value = System.currentTimeMillis()
            }
        }
    }

    fun refresh() {
        bleManager.sendCommand(BleDataParser.buildGetPrinterStatusCmd())
    }

    fun heatBed(targetCelsius: Int) {
        bleManager.sendCommand(BleDataParser.buildHeatBedCmd(targetCelsius))
    }

    fun reprint() {
        bleManager.sendCommand(BleDataParser.buildReprintCmd())
    }
}
