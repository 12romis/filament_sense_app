package com.filament.sense.data.ble

import java.util.UUID

object GattConstants {
    // BLE device name to scan for
    const val DEVICE_NAME = "FilamentSense"

    // Primary GATT service
    val SERVICE_UUID: UUID = UUID.fromString("4fafc201-1fb5-459e-8fcc-c5c9c331914b")

    // Read + Notify: 12 bytes (remaining: Float, gross: Float, hasFilament: byte)
    // One characteristic per spool slot (slots 0–4)
    val SPOOL_DATA_UUIDS: List<UUID> = listOf(
        UUID.fromString("beb5483e-36e1-4688-b7f5-ea07361b26a0"),
        UUID.fromString("beb5483e-36e1-4688-b7f5-ea07361b26a1"),
        UUID.fromString("beb5483e-36e1-4688-b7f5-ea07361b26a2"),
        UUID.fromString("beb5483e-36e1-4688-b7f5-ea07361b26a3"),
        UUID.fromString("beb5483e-36e1-4688-b7f5-ea07361b26a4"),
    )

    // Read + Notify: 12 bytes (temperature: Float, humidity: Float, pressure: Float)
    val ENV_DATA_UUID: UUID = UUID.fromString("beb5483e-36e1-4688-b7f5-ea07361b26b0")

    // Read: 1 byte — number of active spool slots (UInt8)
    val SPOOL_COUNT_UUID: UUID = UUID.fromString("beb5483e-36e1-4688-b7f5-ea07361b26b1")

    // Write (no response): JSON command string
    val CMD_UUID: UUID = UUID.fromString("beb5483e-36e1-4688-b7f5-ea07361b26b2")

    // Read + Write: JSON config string
    val CONFIG_UUID: UUID = UUID.fromString("beb5483e-36e1-4688-b7f5-ea07361b26b3")
}