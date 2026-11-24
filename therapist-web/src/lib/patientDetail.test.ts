import { describe, expect, it } from 'vitest'
import { comfortDescription, comfortTone, summarizePatientDetail } from './patientDetail'
import type { ExerciseSession, PainLevel } from './types'

function record(id: string, patientId: string, endedAt: string, pain: PainLevel, options: Partial<ExerciseSession> = {}): ExerciseSession {
  return {
    id,
    patient_id: patientId,
    exercise_id: 'EX001',
    started_at: endedAt,
    ended_at: endedAt,
    repetitions: 3,
    peak_rom_deg: 70,
    average_smoothness: 0.8,
    pain_level: pain,
    source: 'DEVICE',
    target_reps: 10,
    target_rom_deg: 70,
    repetition_details: [],
    synced_at: endedAt,
    ...options,
  }
}

describe('PGF-THR-04 patient detail', () => {
  it('keeps the empty state distinct from zero scores', () => {
    const s = summarizePatientDetail('P001', [])
    expect(s.totalSessions).toBe(0)
    expect(s.bestPeakRomDeg).toBeNull()
    expect(s.averageSmoothness).toBeNull()
    expect(s.latestSession).toBeNull()
  })

  it('does not display another patient’s data', () => {
    const s = summarizePatientDetail('P001', [
      record('S1', 'P001', '2026-11-12T10:00:00Z', 'NO_PAIN'),
      record('S2', 'P002', '2026-11-13T10:00:00Z', 'STOPPED_DUE_TO_PAIN', { repetitions: 100 }),
    ])
    expect(s.totalSessions).toBe(1)
    expect(s.totalRepetitions).toBe(3)
    expect(s.painReportedSessions).toBe(0)
  })

  it('sorts newest first and counts patient-reported comfort separately', () => {
    const s = summarizePatientDetail('P001', [
      record('older', 'P001', '2026-11-10T10:00:00Z', 'MILD_PAIN', { repetitions: 4, peak_rom_deg: 62 }),
      record('newer', 'P001', '2026-11-12T10:00:00Z', 'STOPPED_DUE_TO_PAIN', { repetitions: 0, peak_rom_deg: 38 }),
      record('oldest', 'P001', '2026-11-08T10:00:00Z', 'NO_PAIN', { repetitions: 2, peak_rom_deg: 73 }),
    ])
    expect(s.sessions.map((item) => item.id)).toEqual(['newer', 'older', 'oldest'])
    expect(s.latestSession?.id).toBe('newer')
    expect(s.totalRepetitions).toBe(6)
    expect(s.bestPeakRomDeg).toBe(73)
    expect(s.noPainSessions).toBe(1)
    expect(s.mildPainSessions).toBe(1)
    expect(s.stoppedForPainSessions).toBe(1)
    expect(s.painReportedSessions).toBe(2)
  })

  it('averages only recorded valid smoothness and retains 0 as a real score', () => {
    const s = summarizePatientDetail('P001', [
      record('s1', 'P001', '2026-11-08T10:00:00Z', 'NO_PAIN', { average_smoothness: null }),
      record('s2', 'P001', '2026-11-09T10:00:00Z', 'NO_PAIN', { average_smoothness: 0 }),
      record('s3', 'P001', '2026-11-10T10:00:00Z', 'NO_PAIN', { average_smoothness: 1 }),
    ])
    expect(s.averageSmoothness).toBe(0.5)
    expect(summarizePatientDetail('P001', [record('s1', 'P001', '2026-11-08T10:00:00Z', 'NO_PAIN', { average_smoothness: null })]).averageSmoothness).toBeNull()
  })

  it('does not fabricate a measured peak for zero-ROM records', () => {
    const s = summarizePatientDetail('P001', [record('s1', 'P001', '2026-11-08T10:00:00Z', 'NO_PAIN', { peak_rom_deg: 0, repetitions: 0 })])
    expect(s.bestPeakRomDeg).toBeNull()
  })

  it('retains seeded and synced device sessions as separate rows', () => {
    const s = summarizePatientDetail('P001', [
      record('S001', 'P001', '2026-11-08T10:00:00Z', 'NO_PAIN', { source: 'SEEDED' }),
      record('D-1', 'P001', '2026-11-12T10:00:00Z', 'MILD_PAIN'),
    ])
    expect(s.totalSessions).toBe(2)
    expect(s.sessions.map((item) => item.source)).toEqual(['DEVICE', 'SEEDED'])
  })

  it('uses only reported feedback, not ROM or smoothness, for comfort labels', () => {
    expect(comfortTone('NO_PAIN')).toBe('ok')
    expect(comfortTone('MILD_PAIN')).toBe('warn')
    expect(comfortTone('STOPPED_DUE_TO_PAIN')).toBe('bad')
    expect(comfortTone(null)).toBe('idle')
    expect(comfortDescription('STOPPED_DUE_TO_PAIN')).toContain('stopped')
    expect(comfortDescription(null)).toContain('No comfort')
  })
})
