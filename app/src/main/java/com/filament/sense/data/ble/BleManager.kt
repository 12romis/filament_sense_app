package com.filament.sense.data.ble

import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import com.filament.sense.domain.model.ConfigData
import com.filament.sense.domain.model.DeviceState
import com.filament.sense.domain.model.EnvData
import com.filament.sense.domain.model.SpoolSlot
import com.welie.blessed.BluetoothCentralManager
import com.welie.blessed.BluetoothCentralManagerCallback
import com.welie.blessed.BluetoothPeripheral
import com.welie.blessed.BluetoothPeripheralCallback
import com.welie.blessed.GattStatus
import com.welie.blessed.HciStatus
import com.welie.blessed.ScanFailure
import com.welie.blessed.WriteType
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

private const val PREF_LAST_MAC = "last_connected_mac"
private const val PREF_LAST_NAME = "last_connected_name"

@Singleton
class BleManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prefs: SharedPreferences,
) {

    // ── Public state ────────────────────────────────────────────────────────

    private val _deviceState = MutableStateFlow(DeviceState.DISCONNECTED)
    val deviceState: StateFlow<DeviceState> = _deviceState.asStateFlow()

    private val _deviceName = MutableStateFlow("")
    val deviceName: StateFlow<String> = _deviceName.asStateFlow()

    private val _spoolUpdates = MutableSharedFlow<SpoolSlot>(replay = 1)
    val spoolUpdates: SharedFlow<SpoolSlot> = _spoolUpdates.asSharedFlow()

    private val _envData = MutableStateFlow<EnvData?>(null)
    val envData: StateFlow<EnvData?> = _envData.asStateFlow()

    private val _configData = MutableStateFlow<ConfigData?>(null)
    val configData: StateFlow<ConfigData?> = _configData.asStateFlow()

    data class ScanEvent(val peripheral: BluetoothPeripheral, val isFilamentSense: Boolean)

    private val _scanResults = MutableSharedFlow<ScanEvent>(replay = 0, extraBufferCapacity = 64)
    val scanResults: SharedFlow<ScanEvent> = _scanResults.asSharedFlow()

    private val _scanFailure = MutableStateFlow<String?>(null)
    val scanFailure: StateFlow<String?> = _scanFailure.asStateFlow()

    private val _debugInfo = MutableStateFlow("idle")
    val debugInfo: StateFlow<String> = _debugInfo.asStateFlow()

    val lastConnectedMac: String?
        get() = prefs.getString(PREF_LAST_MAC, null)

    /** true якщо disconnect() був викликаний явно (не обрив зв'язку) */
    var wasManualDisconnect: Boolean = false
        private set

    // ── Internals ────────────────────────────────────────────────────────────

    private var connectedPeripheral: BluetoothPeripheral? = null
    private var connectingPeripheral: BluetoothPeripheral? = null

    private val peripheralCallback = object : BluetoothPeripheralCallback() {

        override fun onServicesDiscovered(peripheral: BluetoothPeripheral) {
            val cmdChar = peripheral.getCharacteristic(GattConstants.SERVICE_UUID, GattConstants.CMD_UUID)
            Log.d("BleManager", "onServicesDiscovered: cmdChar=$cmdChar props=${cmdChar?.properties}")
            // MTU negotiation must complete before writes — CONNECTED state moves to onMtuChanged
            peripheral.requestMtu(512)
            peripheral.setNotify(GattConstants.SERVICE_UUID, GattConstants.SPOOL_DATA_UUID, true)
            peripheral.setNotify(GattConstants.SERVICE_UUID, GattConstants.ENV_DATA_UUID, true)
            peripheral.readCharacteristic(GattConstants.SERVICE_UUID, GattConstants.CONFIG_UUID)
            // Позачергове читання поточного стану одразу після підключення —
            // нотифікації приходять лише при зміні, тому без цього дані
            // з'являються із затримкою або не з'являються взагалі.
            peripheral.readCharacteristic(GattConstants.SERVICE_UUID, GattConstants.SPOOL_DATA_UUID)
            peripheral.readCharacteristic(GattConstants.SERVICE_UUID, GattConstants.ENV_DATA_UUID)
        }

        override fun onMtuChanged(peripheral: BluetoothPeripheral, mtu: Int, status: GattStatus) {
            Log.d("BleManager", "MTU changed: mtu=$mtu status=$status")
            _deviceState.value = DeviceState.CONNECTED
        }
        override fun onCharacteristicUpdate(
            peripheral: BluetoothPeripheral,
            value: ByteArray,
            characteristic: android.bluetooth.BluetoothGattCharacteristic,
            status: GattStatus,
        ) {
            if (status != GattStatus.SUCCESS) return
            when (characteristic.uuid) {
                GattConstants.SPOOL_DATA_UUID -> {
                    _spoolUpdates.tryEmit(BleDataParser.parseSpoolData(value))
                }
                GattConstants.ENV_DATA_UUID -> {
                    val env = BleDataParser.parseEnvData(value)
                    // Ігноруємо пакет із нульовими значеннями (GATT ще не готовий)
                    if (env != null && (env.temperature != 0f || env.humidity != 0f)) {
                        _envData.value = env
                    }
                }
                GattConstants.CONFIG_UUID -> {
                    _configData.value = BleDataParser.parseConfig(value)
                }
            }
        }
    }

    private val centralCallback = object : BluetoothCentralManagerCallback() {

        override fun onConnectedPeripheral(peripheral: BluetoothPeripheral) {
            wasManualDisconnect = false
            connectingPeripheral = null
            connectedPeripheral = peripheral
            val peripheralName = peripheral.name
            val name = if (!peripheralName.isNullOrEmpty()) {
                prefs.edit().putString(PREF_LAST_NAME, peripheralName).apply()
                peripheralName
            } else {
                prefs.getString(PREF_LAST_NAME, null) ?: GattConstants.DEVICE_NAME
            }
            _deviceName.value = name
            _deviceState.value = DeviceState.CONNECTING
            prefs.edit().putString(PREF_LAST_MAC, peripheral.address).apply()
        }

        override fun onDisconnectedPeripheral(peripheral: BluetoothPeripheral, status: HciStatus) {
            connectingPeripheral = null
            connectedPeripheral = null
            if (_deviceState.value != DeviceState.SCANNING) {
                _deviceState.value = DeviceState.DISCONNECTED
            }
            _envData.value = null
        }

        override fun onDiscoveredPeripheral(
            peripheral: BluetoothPeripheral,
            scanResult: android.bluetooth.le.ScanResult,
        ) {
            val count = _debugInfo.value.substringAfter("callbacks=").substringBefore(" ").toIntOrNull() ?: 0
            _debugInfo.value = "callbacks=${count + 1} last=${peripheral.address}"
            val advertisedUuids = scanResult.scanRecord?.serviceUuids?.map { it.uuid } ?: emptyList()
            val isFilamentSense = advertisedUuids.contains(GattConstants.SERVICE_UUID)
                    || peripheral.name == GattConstants.DEVICE_NAME
            _scanResults.tryEmit(ScanEvent(peripheral, isFilamentSense))
        }

        override fun onScanFailed(scanFailure: ScanFailure) {
            _deviceState.value = DeviceState.DISCONNECTED
            _scanFailure.value = scanFailure.name
        }
    }

    private val central: BluetoothCentralManager by lazy {
        BluetoothCentralManager(context, centralCallback, Handler(Looper.getMainLooper()))
    }

    // ── Public API ───────────────────────────────────────────────────────────

    fun startScan() {
        _scanFailure.value = null
        _debugInfo.value = "starting…"
        connectingPeripheral?.let { central.cancelConnection(it) }
        connectingPeripheral = null

        val btManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val scanner = btManager.adapter?.bluetoothLeScanner
        if (scanner == null) {
            _debugInfo.value = "ERR: scanner=null (BT off?)"
            _deviceState.value = DeviceState.DISCONNECTED
            return
        }
        _debugInfo.value = "scanner=ok isScanning=${central.isScanning} callbacks=0"
        _deviceState.value = DeviceState.SCANNING
        central.scanForPeripherals()
    }

    fun stopScan() {
        central.stopScan()
        if (_deviceState.value == DeviceState.SCANNING) {
            _deviceState.value = DeviceState.DISCONNECTED
        }
    }

    fun connect(peripheral: BluetoothPeripheral) {
        connectingPeripheral = peripheral
        _deviceState.value = DeviceState.CONNECTING
        central.connectPeripheral(peripheral, peripheralCallback)
    }

    fun connectByAddress(address: String) {
        val peripheral = central.getPeripheral(address)
        connect(peripheral)
    }

    /** Швидке пряме підключення до останнього відомого MAC (без сканування). */
    fun autoConnect() {
        val mac = lastConnectedMac ?: return
        // Скасовуємо попередню спробу якщо вона ще активна (щоб не було паралельних запитів)
        connectingPeripheral?.let { central.cancelConnection(it) }
        connectByAddress(mac)
    }

    /**
     * Скасовує поточну спробу підключення без виставлення [wasManualDisconnect].
     * Використовується для timeout-скасування з HomeViewModel.
     */
    fun cancelConnection() {
        connectingPeripheral?.let { central.cancelConnection(it) }
        connectedPeripheral?.let { central.cancelConnection(it) }
    }

    fun disconnect() {
        wasManualDisconnect = true
        connectedPeripheral?.let { central.cancelConnection(it) }
    }

    fun sendCommand(json: String) {
        val peripheral = connectedPeripheral ?: run {
            Log.w("BleManager", "sendCommand: no peripheral, json=$json")
            return
        }
        Log.d("BleManager", "sendCommand: json=$json")
        try {
            val ok = peripheral.writeCharacteristic(
                GattConstants.SERVICE_UUID,
                GattConstants.CMD_UUID,
                json.toByteArray(Charsets.UTF_8),
                WriteType.WITHOUT_RESPONSE,
            )
            Log.d("BleManager", "sendCommand writeCharacteristic result=$ok")
        } catch (e: Exception) {
            Log.e("BleManager", "sendCommand exception", e)
        }
    }

    /** Записує JSON у CONFIG-характеристику (WITH_RESPONSE). */
    fun writeConfig(json: String) {
        val peripheral = connectedPeripheral ?: return
        try {
            peripheral.writeCharacteristic(
                GattConstants.SERVICE_UUID,
                GattConstants.CONFIG_UUID,
                json.toByteArray(Charsets.UTF_8),
                WriteType.WITH_RESPONSE,
            )
        } catch (_: Exception) {}
    }
}
