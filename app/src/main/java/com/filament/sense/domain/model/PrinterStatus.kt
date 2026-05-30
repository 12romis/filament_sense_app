package com.filament.sense.domain.model

data class PrinterStatus(
    val gcodeState: String = "",      // "RUNNING", "FINISH", "IDLE", "FAILED", …
    val fileName: String = "",        // current or last printed file
    val nozzleTemp: Float? = null,    // current nozzle temperature, °C
    val nozzleTarget: Int? = null,    // target nozzle temperature, °C
    val bedTemp: Float? = null,       // current bed temperature, °C
    val bedTarget: Int? = null,       // target bed temperature, °C
    val progress: Int = 0,            // print progress 0-100 %
    val remainingMinutes: Int = 0,    // estimated remaining time, minutes
    val layerNum: Int = 0,            // current layer
    val totalLayers: Int = 0,         // total layers
)
