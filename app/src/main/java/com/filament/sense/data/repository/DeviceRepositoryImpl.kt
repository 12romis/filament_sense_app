package com.filament.sense.data.repository

import com.filament.sense.data.ble.BleManager
import com.filament.sense.domain.model.DeviceState
import com.filament.sense.domain.repository.DeviceRepository
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceRepositoryImpl @Inject constructor(
    private val bleManager: BleManager,
) : DeviceRepository {

    override val deviceState: StateFlow<DeviceState> = bleManager.deviceState
    override val deviceName: StateFlow<String> = bleManager.deviceName
    override val lastConnectedMac: String? get() = bleManager.lastConnectedMac
    override val wasManualDisconnect: Boolean get() = bleManager.wasManualDisconnect

    override suspend fun startScan() = bleManager.startScan()
    override fun stopScan() = bleManager.stopScan()
    override suspend fun connectToDevice(address: String) = bleManager.connectByAddress(address)
    override suspend fun disconnect() = bleManager.disconnect()
    override fun autoConnect() = bleManager.autoConnect()
    override fun cancelConnection() = bleManager.cancelConnection()
}
