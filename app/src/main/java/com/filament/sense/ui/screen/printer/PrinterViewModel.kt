package com.filament.sense.ui.screen.printer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.filament.sense.data.ble.BleDataParser
import com.filament.sense.data.ble.BleManager
import com.filament.sense.domain.model.DeviceState
import com.filament.sense.domain.model.PrinterStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
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
    val filesList: StateFlow<List<String>> = bleManager.filesList

    private val _lastSyncTime = MutableStateFlow<Long?>(null)
    val lastSyncTime: StateFlow<Long?> = _lastSyncTime.asStateFlow()

    // Загальний час друку: фіксується при переході → RUNNING; null = не відомо (буде розраховано)
    private val _totalPrintMinutes = MutableStateFlow<Int?>(null)
    val totalPrintMinutes: StateFlow<Int?> = _totalPrintMinutes.asStateFlow()

    init {
        viewModelScope.launch {
            var prevState = ""
            bleManager.printerStatus.collect { status ->
                if (status != null) {
                    _lastSyncTime.value = System.currentTimeMillis()
                    val state = status.gcodeState
                    if (prevState != "RUNNING" && state == "RUNNING") {
                        _totalPrintMinutes.value = status.remainingMinutes
                    }
                    prevState = state
                }
            }
        }
    }

    fun refresh() {
        bleManager.sendCommand(BleDataParser.buildGetPrinterStatusCmd())
    }

    fun heatBed(targetCelsius: Int) {
        bleManager.sendCommand(BleDataParser.buildHeatBedCmd(targetCelsius))
    }

    fun reprint(fileOverride: String = "") {
        bleManager.sendCommand(BleDataParser.buildReprintCmd(fileOverride.trim()))
    }

    fun requestFilesList() {
        viewModelScope.launch {
            bleManager.sendCommand(BleDataParser.buildListFilesCmd())
            delay(400)
            bleManager.readFilesList()
        }
    }
}
