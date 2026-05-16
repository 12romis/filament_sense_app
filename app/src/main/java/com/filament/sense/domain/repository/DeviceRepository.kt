package com.filament.sense.domain.repository

import com.filament.sense.domain.model.DeviceState
import kotlinx.coroutines.flow.StateFlow

interface DeviceRepository {
    val deviceState: StateFlow<DeviceState>
    val deviceName: StateFlow<String>
    val lastConnectedMac: String?
    /** true якщо останнє відключення було ініційоване користувачем */
    val wasManualDisconnect: Boolean

    suspend fun startScan()
    fun stopScan()
    suspend fun connectToDevice(address: String)
    suspend fun disconnect()
    /** Пряме підключення до останнього відомого MAC без сканування. */
    fun autoConnect()
    /** Скасовує поточну спробу підключення (timeout-скасування, не ручне відключення). */
    fun cancelConnection()
}
