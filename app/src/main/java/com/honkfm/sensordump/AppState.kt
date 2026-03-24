package com.honkfm.sensordump

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object AppState {
    private val _isLogging = MutableStateFlow(false)
    private val _totalEmf = MutableStateFlow(0.0f)
    private val _pendingNote = MutableStateFlow("")
    private val _wifiCount = MutableStateFlow(0)
    private val _callNeighborCell = MutableStateFlow(0)
    private val _emf_anomaly_delta = MutableStateFlow(0f)
    private val _batteryTemperature = MutableStateFlow(0.0)
    private val _scanRunTime = MutableStateFlow(0L)
    private val _scanLines = MutableStateFlow(0)
    private val _scanSize = MutableStateFlow(0L)
    private val _barometerPa = MutableStateFlow(0F)

    val isLogging = _isLogging.asStateFlow()
    val totalEmf = _totalEmf.asStateFlow()
    val pendingNote = _pendingNote.asStateFlow()
    val wifiCount = _wifiCount.asStateFlow()
    val callNeighborCell = _callNeighborCell.asStateFlow()
    val emfAnomalyDelta = _emf_anomaly_delta.asStateFlow()
    val batteryTemperature = _batteryTemperature.asStateFlow()
    val scanLines = _scanLines.asStateFlow()
    var scanStartTime = 0L
    val scanRunTime = _scanRunTime.asStateFlow()
    val scanSize =  _scanSize.asStateFlow()
    val barometerPa = _barometerPa.asStateFlow()

    fun addLine(sizeBytes: Long = 0) {
        _scanLines.value++
        _scanRunTime.value = (System.nanoTime() - scanStartTime)
        _scanSize.value += sizeBytes
    }

    fun setBatteryTemperature(temp: Double) {
        _batteryTemperature.value = temp
    }

    fun setCellNeighborCount(count: Int) {
        _callNeighborCell.value = count
    }

    fun setWifiCount(count: Int) {
        _wifiCount.value = count
    }

    fun setEmfAnomalyDelta(value: Float) {
        _emf_anomaly_delta.value = value
    }

    fun setBarometerPa(pa: Float) {
        _barometerPa.value = pa
    }

    fun setIsLogging(running: Boolean) {
        _isLogging.value = running

        // Reset
        if (running) {
            scanStartTime = System.nanoTime()
            _scanRunTime.value = 0
            _scanLines.value = 0
            _scanSize.value = 0
            _barometerPa.value = 0F
        }
    }

    fun getIsLogging(): Boolean {
        return _isLogging.value
    }

    fun getTotalEmf(): Float {
        return _totalEmf.value
    }

    fun setTotalEmf(value: Float) {
        _totalEmf.value = value
    }

    fun setPendingNote(note: String) {
        _pendingNote.value = note
    }
}