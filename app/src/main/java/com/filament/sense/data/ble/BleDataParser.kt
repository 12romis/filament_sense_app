package com.filament.sense.data.ble

import com.filament.sense.domain.model.EnvData
import com.filament.sense.domain.model.SpoolSlot
import java.nio.ByteBuffer
import java.nio.ByteOrder

object BleDataParser {

    /**
     * Парсить 12 байт слоту котушки.
     * Формат: remaining (Float LE) | gross (Float LE) | hasFilament (byte, 0=false)
     */
    fun parseSpoolData(bytes: ByteArray, index: Int): SpoolSlot {
        if (bytes.size < 9) return SpoolSlot(index = index)
        val buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        val remaining = buf.float
        val gross = buf.float
        val hasFilament = buf.get() != 0.toByte()
        return SpoolSlot(
            index = index,
            remainingGrams = remaining,
            grossWeightGrams = gross,
            hasFilament = hasFilament,
        )
    }

    /**
     * Парсить 12 байт env-даних.
     * Формат: temperature (Float LE) | humidity (Float LE) | pressure (Float LE)
     */
    fun parseEnvData(bytes: ByteArray): EnvData? {
        if (bytes.size < 12) return null
        val buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        return EnvData(
            temperature = buf.float,
            humidity = buf.float,
            pressure = buf.float,
        )
    }

    /**
     * Формує JSON-команду для CMD_UUID.
     */
    fun buildSaveBaselineCmd(slot: Int) =
        """{"cmd":"save_baseline","slot":$slot}"""

    fun buildSetNameCmd(slot: Int, value: String) =
        """{"cmd":"set_name","slot":$slot,"value":"$value"}"""

    fun buildSetTareCmd(slot: Int, value: Float) =
        """{"cmd":"set_tare","slot":$slot,"value":$value}"""

    fun buildSetThresholdCmd(warning: Int, critical: Int, empty: Int) =
        """{"cmd":"set_threshold","warning":$warning,"critical":$critical,"empty":$empty}"""
}