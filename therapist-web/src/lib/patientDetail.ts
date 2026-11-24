import type { ExerciseSession, PainLevel } from './types'

/** PGF-THR-04: a read-only, patient-scoped view of recorded activity and comfort. */
export interface PatientDetailSummary {
  sessions: ExerciseSession[]
  totalSessions: number
  totalRepetitions: number
  bestPeakRomDeg: number | null
  averageSmoothness: number | null
  latestSession: ExerciseSession | null
  noPainSessions: number
  mildPainSessions: number
  stoppedForPainSessions: number
  painReportedSessions: number
}

/** Never aggregate another patient's sessions, even if a caller mixes data. */
export function summarizePatientDetail(patientId: string, input: ExerciseSession[]): PatientDetailSummary {
  const sessions = input.filter((session) => session.patient_id === patientId).sort((a, b) => {
    const delta = new Date(b.ended_at).getTime() - new Date(a.ended_at).getTime()
    return delta !== 0 ? delta : a.id.localeCompare(b.id)
  })
  const smooth = sessions
    .map((session) => session.average_smoothness)
    .filter((score): score is number => score != null && Number.isFinite(score) && score >= 0 && score <= 1)
  const peaks = sessions
    .map((session) => session.peak_rom_deg)
    .filter((peak) => Number.isFinite(peak) && peak > 0)
  const count = (pain: PainLevel) => sessions.filter((session) => session.pain_level === pain).length
  const mildPainSessions = count('MILD_PAIN')
  const stoppedForPainSessions = count('STOPPED_DUE_TO_PAIN')
  return {
    sessions,
    totalSessions: sessions.length,
    totalRepetitions: sessions.reduce((total, session) => total + session.repetitions, 0),
    bestPeakRomDeg: peaks.length ? peaks.reduce((best, peak) => Math.max(best, peak), 0) : null,
    averageSmoothness: smooth.length ? smooth.reduce((sum, value) => sum + value, 0) / smooth.length : null,
    latestSession: sessions[0] ?? null,
    noPainSessions: count('NO_PAIN'),
    mildPainSessions,
    stoppedForPainSessions,
    painReportedSessions: mildPainSessions + stoppedForPainSessions,
  }
}

export type ComfortTone = 'ok' | 'warn' | 'bad' | 'idle'

export function comfortTone(level: PainLevel | null): ComfortTone {
  switch (level) {
    case 'NO_PAIN': return 'ok'
    case 'MILD_PAIN': return 'warn'
    case 'STOPPED_DUE_TO_PAIN': return 'bad'
    default: return 'idle'
  }
}

/** This is a report of the user's choice, not an inferred clinical diagnosis. */
export function comfortDescription(level: PainLevel | null): string {
  switch (level) {
    case 'NO_PAIN': return 'No pain was reported in the latest session.'
    case 'MILD_PAIN': return 'The patient reported mild pain in the latest session. Review their feedback.'
    case 'STOPPED_DUE_TO_PAIN': return 'The patient stopped their latest session due to pain. Review their feedback before adjusting the plan.'
    default: return 'No comfort feedback is available yet.'
  }
}
