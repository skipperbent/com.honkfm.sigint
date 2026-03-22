package com.honkfm.sensordump

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object AppState {
    private val _isLogging = MutableStateFlow(false);
    private val _totalEmf = MutableStateFlow(0.0f)
    private val _pendingNote = MutableStateFlow("")

    val isLogging = _isLogging.asStateFlow()
    val totalEmf = _totalEmf.asStateFlow()
    val pendingNote = _pendingNote.asStateFlow()

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