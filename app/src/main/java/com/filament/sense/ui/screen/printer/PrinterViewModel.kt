package com.filament.sense.ui.screen.printer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.filament.sense.data.ble.BleDataParser
import com.filament.sense.data.ble.BleManager
import com.filament.sense.domain.model.DeviceState
import com.filament.sense.domain.model.PrinterStatus
import com.filament.sense.domain.model.SpoolSlot
import com.filament.sense.domain.usecase.GetSpoolsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PrinterViewModel @Inject constructor(
    private val bleManager: BleManager,
    private val getSpools: GetSpoolsUseCase,
) : ViewModel() {

    val deviceState: StateFlow<DeviceState> = bleManager.deviceState
    val printerStatus: StateFlow<PrinterStatus?> = bleManager.printerStatus
    val filesList: StateFlow<List<String>> = bleManager.filesList

    val activeSpool: StateFlow<SpoolSlot?> = getSpools()
        .map { spools -> spools.firstOrNull { it.isActive } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

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

    fun heatNozzle(targetCelsius: Int) {
        bleManager.sendCommand(BleDataParser.buildHeatNozzleCmd(targetCelsius))
    }

    fun reprint(fileOverride: String = "") {
        bleManager.sendCommand(BleDataParser.buildReprintCmd(fileOverride.trim()))
    }

    fun loadFilament(targetTempCelsius: Int = 250) {
        bleManager.sendCommand(BleDataParser.buildLoadFilamentCmd(targetTempCelsius))
    }

    fun unloadFilament() {
        bleManager.sendCommand(BleDataParser.buildUnloadFilamentCmd())
    }

    fun requestFilesList() {
        viewModelScope.launch {
            bleManager.sendCommand(BleDataParser.buildListFilesCmd())
            delay(400)
            bleManager.readFilesList()
        }
    }
}
