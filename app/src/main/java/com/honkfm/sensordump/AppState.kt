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

    val isLogging = _isLogging.asStateFlow()
    val totalEmf = _totalEmf.asStateFlow()
    val pendingNote = _pendingNote.asStateFlow()
    val wifiCount = _wifiCount.asStateFlow()
    val callNeighborCell = _callNeighborCell.asStateFlow()
    val emfAnomalyDelta = _emf_anomaly_delta.asStateFlow()
    val batteryTemperature = _batteryTemperature.asStateFlow()

    fun setBatteryTemperature(temp: Double) {
        _batteryTemperature.value = temp;
    }

    fun setCellNeighborCount(count: Int) {
        _callNeighborCell.value = count;
    }

    fun setWifiCount(count: Int) {
        _wifiCount.value = count;
    }

    fun setEmfAnomalyDelta(value: Float) {
        _emf_anomaly_delta.value = value
    }

    fun setIsLogging(running: Boolean) {
        _isLogging.value = running
    }

    fun getIsLogging(): Boolean {
        return _isLogging.value;
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