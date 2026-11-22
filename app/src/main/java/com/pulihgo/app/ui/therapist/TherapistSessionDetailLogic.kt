package com.pulihgo.app.ui.therapist

import com.pulihgo.app.data.session.SessionPainLevel
import com.pulihgo.app.data.session.SessionSource
import com.pulihgo.app.data.session.TherapistSessionLookup

enum class RomOutcome { BELOW_TARGET, TARGET_HIT, CEILING_EXCEEDED, UNKNOWN }

data class SessionRepRow(
    val number: Int,
    val peakRomDeg: Double,
    val durationMs: Long,
    val smoothnessPercent: Int?,
    val outcome: RomOutcome,
)

data class TherapistSessionDetail(
    val patientId: String,
    val patientName: String,
    val sessionId: String,
    val exerciseName: String,
    val startedAtEpochMs: Long,
    val durationMs: Long,
    val seeded: Boolean,
    val repetitions: Int,
    val targetReps: Int?,
    val peakRomDeg: Double,
    val targetRomDeg: Double?,
    val romCeilingDeg: Double?,
    val romOutcome: RomOutcome,
    val smoothnessPercent: Int?,
    val painLevel: SessionPainLevel,
    val reps: List<SessionRepRow>,
)

object TherapistSessionDetailLogic {
    fun build(
        found: TherapistSessionLookup.Found,
        patientName: String,
        exercises: Map<String, ExerciseTarget>,
    ): TherapistSessionDetail {
        val session = found.session
        val record = session.record
        val exercise = exercises[record.exerciseId]
        val targetRom = session.targetRomDeg ?: exercise?.targetRomDeg
        val ceiling = exercise?.romCeilingDeg
        return TherapistSessionDetail(
            patientId = found.patient.id,
            patientName = patientName,
            sessionId = session.id,
            exerciseName = exercise?.name ?: record.exerciseId,
            startedAtEpochMs = record.startedAtEpochMs,
            durationMs = session.durationMs,
            seeded = session.source == SessionSource.SEEDED,
            repetitions = record.repetitions,
            targetReps = session.targetReps ?: exercise?.targetReps?.takeIf { it > 0 },
            peakRomDeg = record.peakRomDeg,
            targetRomDeg = targetRom,
            romCeilingDeg = ceiling,
            romOutcome = romOutcome(record.peakRomDeg, targetRom, ceiling, record.repetitions > 0),
            smoothnessPercent = record.averageSmoothness?.let(::percent),
            painLevel = record.painLevel,
            reps = record.repetitionDetails.sortedBy { it.repetitionNumber }.map {
                SessionRepRow(
                    number = it.repetitionNumber,
                    peakRomDeg = it.peakRomDeg,
                    durationMs = it.durationMs,
                    smoothnessPercent = it.smoothnessScore?.let(::percent),
                    outcome = romOutcome(it.peakRomDeg, targetRom, ceiling, true),
                )
            },
        )
    }

    fun romOutcome(peakDeg: Double, targetDeg: Double?, ceilingDeg: Double?, measured: Boolean): RomOutcome = when {
        !measured -> RomOutcome.UNKNOWN
        ceilingDeg != null && peakDeg > ceilingDeg -> RomOutcome.CEILING_EXCEEDED
        targetDeg == null -> RomOutcome.UNKNOWN
        peakDeg >= targetDeg -> RomOutcome.TARGET_HIT
        else -> RomOutcome.BELOW_TARGET
    }

    private fun percent(score: Double): Int = Math.round(score * 100).toInt()
}
