package com.filament.sense.domain.model

/** Конфігурація, яка зчитується з ESP32 через CONFIG-характеристику. */
data class ConfigData(val mqttHost: String)
