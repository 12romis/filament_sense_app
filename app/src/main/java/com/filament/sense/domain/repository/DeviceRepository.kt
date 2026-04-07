package com.filament.sense.domain.repository

import com.filament.sense.domain.model.DeviceState
import kotlinx.coroutines.flow.StateFlow

interface DeviceRepository {
    val deviceState: StateFlow<DeviceState>
    val deviceName: StateFlow<String>
    suspend fun startScan()
    suspend fun stopScan()
    suspend fun connectToDevice(address: String)
    suspend fun disconnect()
}