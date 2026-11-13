package com.pulihgo.app.sensor

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class SensorUiState(
    val source: SensorSource? = null,
    val health: SensorHealth = SensorHealth.WaitingFirstSample,
    val accuracy: SensorAccuracy = SensorAccuracy.UNKNOWN,
    val raw: Angles? = null,
    val calibrated: Angles? = null,
    val calibration: CalibrationState = CalibrationState.NotCalibrated,
)

data class CalibratedSample(val angles: Angles, val elapsedMs: Long) {
    fun angle(axis: Axis): Double = angles[axis]
}

class CalibratedAngleController(
    private val source: SensorSource?,
    private val updates: () -> Flow<SensorUpdate>,
    private val clockMs: () -> Long,
    private val calibrator: AngleCalibrator = AngleCalibrator(),
    private val healthMonitor: SensorHealthMonitor = SensorHealthMonitor(),
    private val healthTickMs: Long = 250,
) {
    private val _state = MutableStateFlow(
        SensorUiState(
            source = source,
            health = if (source == null) SensorHealth.Unavailable else SensorHealth.WaitingFirstSample,
            calibration = calibrator.state,
        ),
    )
    val state: StateFlow<SensorUiState> = _state.asStateFlow()

    private val _samples = MutableSharedFlow<CalibratedSample>(
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    val samples: SharedFlow<CalibratedSample> = _samples.asSharedFlow()

    init {
        if (source == null) healthMonitor.markUnavailable()
    }

    suspend fun run() {
        if (source == null) {
            _state.update { it.copy(health = SensorHealth.Unavailable) }
            return
        }
        healthMonitor.onStarted(clockMs())
        coroutineScope {
            launch {
                while (isActive) {
                    val health = healthMonitor.evaluate(clockMs())
                    _state.update { it.copy(health = health) }
                    delay(healthTickMs)
                }
            }
            updates().collect(::handle)
        }
    }

    internal fun handle(update: SensorUpdate) {
        when (update) {
            is SensorUpdate.Accuracy -> _state.update { it.copy(accuracy = update.accuracy) }
            SensorUpdate.RegistrationFailed -> {
                healthMonitor.markRegistrationFailed()
                _state.update { it.copy(health = SensorHealth.RegistrationFailed) }
            }
            is SensorUpdate.Reading -> {
                val raw = update.reading.angles
                healthMonitor.onSample(update.reading.elapsedMs)
                val calibration = calibrator.onSample(raw)
                val relative = calibrator.relative(raw)
                _state.update { it.copy(raw = raw, calibrated = relative, calibration = calibration) }
                _samples.tryEmit(CalibratedSample(relative, update.reading.elapsedMs))
            }
        }
    }

    fun calibrate() {
        calibrator.start()
        _state.update { it.copy(calibration = calibrator.state) }
    }

    fun resetCalibration() {
        calibrator.reset()
        _state.update { it.copy(calibration = calibrator.state) }
    }

    val offset: Angles? get() = calibrator.offset
}
