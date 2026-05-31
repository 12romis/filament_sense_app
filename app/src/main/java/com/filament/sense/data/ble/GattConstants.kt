package com.filament.sense.data.ble

import java.util.UUID

object GattConstants {
    // BLE device name to scan for
    const val DEVICE_NAME = "FilamentSense"

    // Primary GATT service
    val SERVICE_UUID: UUID = UUID.fromString("4fafc201-1fb5-459e-8fcc-c5c9c331914b")

    // Read + Notify: 21 bytes — one slot (remaining, gross, baseline, baselineTs, hasFilament)
    val SPOOL_DATA_UUID: UUID = UUID.fromString("beb5483e-36e1-4688-b7f5-ea07361b26a0")

    // Read + Notify: 12 bytes (temperature: Float, humidity: Float, pressure: Float)
    val ENV_DATA_UUID: UUID = UUID.fromString("beb5483e-36e1-4688-b7f5-ea07361b26b0")

    // Write (no response): JSON command string
    val CMD_UUID: UUID = UUID.fromString("beb5483e-36e1-4688-b7f5-ea07361b26b2")

    // Read + Write: JSON config string (mqtt_host, …)
    val CONFIG_UUID: UUID = UUID.fromString("beb5483e-36e1-4688-b7f5-ea07361b26b3")

    // Read + Notify: JSON printer telemetry from Bambu MQTT (via ESP32)
    val PRINTER_STATUS_UUID: UUID = UUID.fromString("beb5483e-36e1-4688-b7f5-ea07361b26b4")

    // Read + Notify: JSON array of recently printed files ["file_plate_1.gcode", ...]
    val FILES_LIST_UUID: UUID = UUID.fromString("beb5483e-36e1-4688-b7f5-ea07361b26b5")
}
