package com.filament.sense.data.ble

import android.content.Context
import com.filament.sense.domain.model.DeviceState
import com.filament.sense.domain.model.EnvData
import com.filament.sense.domain.model.SpoolSlot
import com.welie.blessed.BluetoothCentralManager
import com.welie.blessed.BluetoothCentralManagerCallback
import com.welie.blessed.BluetoothPeripheral
import com.welie.blessed.BluetoothPeripheralCallback
import com.welie.blessed.GattStatus
import com.welie.blessed.ScanFailure
import com.welie.blessed.WriteType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BleManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    // ── Public state ────────────────────────────────────────────────────────

    private val _deviceState = MutableStateFlow(DeviceState.DISCONNECTED)
    val deviceState: StateFlow<DeviceState> = _deviceState.asStateFlow()

    private val _deviceName = MutableStateFlow("")
    val deviceName: StateFlow<String> = _deviceName.asStateFlow()

    /** Оновлення по одному слоту. Key = index. */
    private val _spoolUpdates = MutableSharedFlow<SpoolSlot>(replay = GattConstants.SPOOL_DATA_UUIDS.size)
    val spoolUpdates: SharedFlow<SpoolSlot> = _spoolUpdates.asSharedFlow()

    private val _envData = MutableStateFlow<EnvData?>(null)
    val envData: StateFlow<EnvData?> = _envData.asStateFlow()

    /** Список пристроїв знайдених під час сканування */
    private val _scanResults = MutableSharedFlow<BluetoothPeripheral>(replay = 0)
    val scanResults: SharedFlow<BluetoothPeripheral> = _scanResults.asSharedFlow()

    // ── Internals ────────────────────────────────────────────────────────────

    private var connectedPeripheral: BluetoothPeripheral? = null

    private val peripheralCallback = object : BluetoothPeripheralCallback() {

        override fun onServicesDiscovered(peripheral: BluetoothPeripheral) {
            // Підписуємось на notify для кожного слоту
            GattConstants.SPOOL_DATA_UUIDS.forEach { uuid ->
                peripheral.setNotify(GattConstants.SERVICE_UUID, uuid, true)
            }
            peripheral.setNotify(GattConstants.SERVICE_UUID, GattConstants.ENV_DATA_UUID, true)
            _deviceState.value = DeviceState.CONNECTED
        }

        override fun onCharacteristicUpdate(
            peripheral: BluetoothPeripheral,
            value: ByteArray,
            characteristic: android.bluetooth.BluetoothGattCharacteristic,
            status: GattStatus,
        ) {
            if (status != GattStatus.SUCCESS) return
            val uuid = characteristic.uuid
            val slotIndex = GattConstants.SPOOL_DATA_UUIDS.indexOf(uuid)
            when {
                slotIndex >= 0 -> {
                    val spool = BleDataParser.parseSpoolData(value, slotIndex)
                    _spoolUpdates.tryEmit(spool)
                }
                uuid == GattConstants.ENV_DATA_UUID -> {
                    _envData.value = BleDataParser.parseEnvData(value)
                }
            }
        }
    }

    private val centralCallback = object : BluetoothCentralManagerCallback() {

        override fun onConnectedPeripheral(peripheral: BluetoothPeripheral) {
            connectedPeripheral = peripheral
            _deviceName.value = peripheral.name ?: GattConstants.DEVICE_NAME
            _deviceState.value = DeviceState.CONNECTING
        }

        override fun onDisconnectedPeripheral(peripheral: BluetoothPeripheral, status: GattStatus) {
            connectedPeripheral = null
            _deviceState.value = DeviceState.DISCONNECTED
            _envData.value = null
        }

        override fun onDiscoveredPeripheral(peripheral: BluetoothPeripheral, scanResult: android.bluetooth.le.ScanResult) {
            _scanResults.tryEmit(peripheral)
        }

        override fun onScanFailed(scanFailure: ScanFailure) {
            _deviceState.value = DeviceState.DISCONNECTED
        }
    }

    private val central: BluetoothCentralManager by lazy {
        BluetoothCentralManager(context, centralCallback)
    }

    // ── Public API ───────────────────────────────────────────────────────────

    fun startScan() {
        _deviceState.value = DeviceState.SCANNING
        central.scanForPeripheralsWithNames(arrayOf(GattConstants.DEVICE_NAME))
    }

    fun stopScan() {
        central.stopScan()
        if (_deviceState.value == DeviceState.SCANNING) {
            _deviceState.value = DeviceState.DISCONNECTED
        }
    }

    fun connect(peripheral: BluetoothPeripheral) {
        _deviceState.value = DeviceState.CONNECTING
        central.connectPeripheral(peripheral, peripheralCallback)
    }

    fun connectByAddress(address: String) {
        val peripheral = central.getPeripheral(address)
        connect(peripheral)
    }

    fun disconnect() {
        connectedPeripheral?.let { central.cancelConnection(it) }
    }

    fun sendCommand(json: String) {
        val peripheral = connectedPeripheral ?: return
        val bytes = json.toByteArray(Charsets.UTF_8)
        peripheral.writeCharacteristic(
            GattConstants.SERVICE_UUID,
            GattConstants.CMD_UUID,
            bytes,
            WriteType.WITHOUT_RESPONSE,
        )
    }
}