package com.filament.sense.data.ble

import com.filament.sense.domain.model.ConfigData
import com.filament.sense.domain.model.EnvData
import com.filament.sense.domain.model.SpoolSlot
import java.nio.ByteBuffer
import java.nio.ByteOrder

object BleDataParser {

    /**
     * Парсить 21 байт слоту котушки (little-endian):
     *   offset  0: Float  remainingGrams
     *   offset  4: Float  grossWeightGrams
     *   offset  8: Float  baselineWeight
     *   offset 12: Int64  baselineTimestamp (Unix sec; 0 = не встановлено)
     *   offset 20: UInt8  hasFilament (0=false)
     */
    fun parseSpoolData(bytes: ByteArray): SpoolSlot {
        if (bytes.size < 21) return SpoolSlot()
        val buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        val remaining = buf.float
        val gross = buf.float
        val baseline = buf.float
        val tsSec = buf.long
        val hasFil = buf.get() != 0.toByte()
        return SpoolSlot(
            remainingGrams = if (remaining.isNaN()) 0f else remaining,
            grossWeightGrams = if (gross.isNaN()) 0f else gross,
            baselineWeight = if (baseline.isNaN()) 0f else baseline,
            baselineTimestamp = if (tsSec == 0L) null else tsSec * 1000L,
            hasFilament = hasFil,
        )
    }

    /**
     * Парсить 12 байт env-даних (little-endian):
     *   temperature (Float) | humidity (Float) | pressure (Float)
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
     * Парсить JSON конфігурацію з CONFIG-характеристики.
     * Підтримуємо поле "mqtt_host" без зовнішніх залежностей.
     */
    fun parseConfig(bytes: ByteArray): ConfigData? {
        val json = bytes.toString(Charsets.UTF_8).trim()
        val regex = """"mqtt_host"\s*:\s*"([^"]*)"""".toRegex()
        val match = regex.find(json) ?: return null
        return ConfigData(mqttHost = match.groupValues[1])
    }

    // ── Команди (Android → ESP32, CMD_UUID) ─────────────────────────────────

    fun buildSaveBaselineCmd(slot: Int = 0) =
        """{"cmd":"save_baseline","slot":$slot}"""

    fun buildSetMqttHostJson(host: String) =
        """{"mqtt_host":"$host"}"""

    fun buildSetNameCmd(slot: Int, value: String) =
        """{"cmd":"set_name","slot":$slot,"value":"$value"}"""

    fun buildSetTareCmd(slot: Int, tare: Float, nominal: Int) =
        """{"cmd":"set_tare","slot":$slot,"value":$tare,"nominal":$nominal}"""

    fun buildSetThresholdCmd(warning: Int, critical: Int, empty: Int) =
        """{"cmd":"set_threshold","warning":$warning,"critical":$critical,"empty":$empty}"""

    fun buildManualReportCmd() =
        """{"cmd":"manual_report"}"""
}
