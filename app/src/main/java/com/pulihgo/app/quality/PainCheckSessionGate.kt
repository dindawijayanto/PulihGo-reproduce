package com.pulihgo.app.quality

import com.pulihgo.app.data.session.ExerciseSessionRecord
import com.pulihgo.app.data.session.SessionPainLevel
import com.pulihgo.app.sensor.LiveExerciseSnapshot

/**
 * PGF-QS-03: freezes one actual sensor snapshot before Pain Check / emergency stop.
 * Pure Kotlin. Does not own sensor processing, persistence, or Supabase synchronisation.
 *
 * The exact same session data and ID are sent on every retry after a failed save.
 * A successful save makes this gate terminal until the screen is recreated.
 */
class PainCheckSessionGate(
    private val patientId: String,
    private val exerciseId: String,
) {
    init {
        require(patientId.isNotBlank())
        require(exerciseId.isNotBlank())
    }

    private enum class Mode { FINISH, PAIN_STOP }

    private data class Frozen(
        val startEpochMs: Long,
        val endEpochMs: Long,
        val snapshot: LiveExerciseSnapshot,
        val mode: Mode,
    )

    private var frozen: Frozen? = null
    private var saving = false
    private var saved = false

    val isFrozen: Boolean get() = frozen != null
    val isSaved: Boolean get() = saved
    val isStopPending: Boolean get() = frozen?.mode == Mode.PAIN_STOP
    val isReviewPending: Boolean get() = frozen?.mode == Mode.FINISH

    fun beginReview(snapshot: LiveExerciseSnapshot, startedAtEpochMs: Long, nowEpochMs: Long): Boolean =
        freeze(snapshot, startedAtEpochMs, nowEpochMs, Mode.FINISH)

    fun beginPainStop(snapshot: LiveExerciseSnapshot, startedAtEpochMs: Long, nowEpochMs: Long): Boolean =
        freeze(snapshot, startedAtEpochMs, nowEpochMs, Mode.PAIN_STOP)

    private fun freeze(
        snapshot: LiveExerciseSnapshot,
        startedAtEpochMs: Long,
        nowEpochMs: Long,
        mode: Mode,
    ): Boolean {
        if (frozen != null || saving || saved || nowEpochMs <= 0L) return false
        val started = startedAtEpochMs.takeIf { it > 0L && it <= nowEpochMs } ?: nowEpochMs
        frozen = Frozen(
            startEpochMs = started,
            endEpochMs = nowEpochMs,
            // A defensive copy keeps the recorded per-repetition data immutable.
            snapshot = snapshot.copy(completedRepetitions = snapshot.completedRepetitions.toList()),
            mode = mode,
        )
        return true
    }

    /** Return to practice without writing a session. Only a normal Finish review is cancellable. */
    fun cancelReview(): Boolean {
        if (saving || saved || frozen?.mode != Mode.FINISH) return false
        frozen = null
        return true
    }

    /** Synchronous local commit callback; cloud sync is handled by the existing navigation layer. */
    fun submit(
        pain: SessionPainLevel,
        save: (ExerciseSessionRecord) -> Boolean,
    ): Boolean {
        val current = frozen ?: return false
        if (saving || saved) return false
        if (current.mode == Mode.PAIN_STOP && pain != SessionPainLevel.STOPPED_DUE_TO_PAIN) return false
        if (current.mode == Mode.FINISH && pain == SessionPainLevel.STOPPED_DUE_TO_PAIN) return false

        val s = current.snapshot
        val record = ExerciseSessionRecord(
            patientId = patientId,
            exerciseId = exerciseId,
            startedAtEpochMs = current.startEpochMs,
            endedAtEpochMs = current.endEpochMs,
            repetitions = s.repetitions,
            peakRomDeg = s.peakRomDeg,
            averageSmoothness = s.averageSmoothness,
            painLevel = pain,
            repetitionDetails = s.completedRepetitions,
        )
        saving = true
        return try {
            val committed = runCatching { save(record) }.getOrDefault(false)
            if (committed) saved = true
            committed
        } finally {
            saving = false
        }
    }
}
