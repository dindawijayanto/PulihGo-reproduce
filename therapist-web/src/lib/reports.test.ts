import { describe, expect, it } from 'vitest'
import { assemble } from './dashboard'
import { buildReport, rangeLabel, rangeStart, statusOf } from './reports'
import type { Exercise, ExerciseSession, PainLevel, Patient } from './types'

const today = new Date(2026, 10, 23, 15, 0)
const at = (dayOffset: number, hour = 9) => new Date(2026, 10, 23 + dayOffset, hour, 0)

const exercises: Record<string, Exercise> = {
  EX001: { id: 'EX001', name: 'Forearm Supination and Pronation', target_rom_deg: 70, target_reps: 10, rom_ceiling_deg: 80 },
}

const alya: Patient = {
  id: 'P001', profile_id: 'uuid-1', therapist_id: 'T1', age: 24, affected_arm: 'RIGHT',
  rehabilitation_goal: 'Forearm supination and pronation', notes: null, profile: { full_name: 'Alya Putri', username: 'patient01' },
}

let seq = 0
function session(ended: Date, opts: Partial<ExerciseSession> & { pain?: PainLevel } = {}): ExerciseSession {
  const { pain, ...rest } = opts
  return {
    id: `S${++seq}`, patient_id: 'P001', exercise_id: 'EX001',
    started_at: new Date(ended.getTime() - 150_000).toISOString(), ended_at: ended.toISOString(),
    repetitions: 5, peak_rom_deg: 60, average_smoothness: null, pain_level: pain ?? 'NO_PAIN',
    source: 'DEVICE', target_reps: null, target_rom_deg: null, repetition_details: [], synced_at: ended.toISOString(),
    ...rest,
  }
}

const report = (sessions: ExerciseSession[], range: Parameters<typeof buildReport>[1] = 'LAST_7_DAYS') =>
  buildReport(assemble([alya], sessions)[0], range, exercises, today)

describe('ranges', () => {
  it('starts the range so it includes today', () => {
    expect(rangeStart('LAST_7_DAYS', today)?.getDate()).toBe(17)
    expect(rangeStart('LAST_30_DAYS', today)?.getMonth()).toBe(9) // 25 Oct
    expect(rangeStart('ALL_TIME', today)).toBeNull()
    expect(rangeLabel('LAST_7_DAYS', today)).toBe('Last 7 Days (17 Nov – 23 Nov)')
    expect(rangeLabel('ALL_TIME', today)).toBe('All Time')
  })
})

describe('statusOf', () => {
  it('ranks pain above a zero-rep session', () => {
    expect(statusOf(session(at(0), { repetitions: 0, pain: 'STOPPED_DUE_TO_PAIN' }))).toBe('STOPPED_FOR_PAIN')
    expect(statusOf(session(at(0), { repetitions: 0, pain: 'MILD_PAIN' }))).toBe('MILD_PAIN')
    expect(statusOf(session(at(0), { repetitions: 0 }))).toBe('NO_REPETITIONS')
    expect(statusOf(session(at(0)))).toBe('COMPLETED')
  })
})

describe('buildReport', () => {
  it('aggregates sessions inside the range, includes seeded data, and computes advanced KPIs', () => {
    const r = report([
      session(at(0), { repetitions: 4, peak_rom_deg: 58, average_smoothness: 0.8 }),
      session(at(-3), { repetitions: 6, peak_rom_deg: 66, pain: 'MILD_PAIN', average_smoothness: 0.9 }),
      session(at(-4), { repetitions: 0, peak_rom_deg: 0, average_smoothness: 0.6 }),
      session(at(-5), { repetitions: 5, peak_rom_deg: 60, source: 'SEEDED' }), // should be included in KPIs
      session(at(-10), { repetitions: 9, peak_rom_deg: 75 }), // out of range
    ])
    // Total reps = 4 + 6 + 0 + 5 = 15
    // Weighted smoothness = (0.8 * 4 + 0.9 * 6 + 5 * null(skip)) / 10 = (3.2 + 5.4) / 10 = 0.86
    // target reps = 10. Actual sessions = 4 (day 0, -3, -4, -5). Total expected reps = 4 * 10 = 40.
    // adherence = 15 / 40 = 37.5%
    // ROM Delta = day 0 ROM (58) - day -5 ROM (60) = -2
    expect(r.kpis).toEqual({
      sessions: 4,
      totalRepetitions: 15,
      bestPeakRomDeg: 66,
      averagePeakRomDeg: 61.333333333333336, // (58 + 66 + 60) / 3 = 184 / 3 = 61.33...
      averageSmoothness: 0.86,
      painReportedSessions: 1,
      adherenceRate: 100 * 15 / 40,
      romProgressionDelta: -2,
      painRate: (1 / 4) * 100,
      mildPainSessions: 1,
      stoppedPainSessions: 0,
    })
    expect(r.sessionsOutsideRange).toBe(1)
    expect(r.rows.map((row) => row.status)).toEqual(['COMPLETED', 'NO_REPETITIONS', 'MILD_PAIN', 'COMPLETED'])
    expect(r.romTrend.map((p) => p.peakRomDeg)).toEqual([60, 66, 58]) // Seeded data is in romTrend
  })

  it('includes everything for All Time and leaves KPIs empty when nothing has reps', () => {
    const all = report([session(at(-40), { repetitions: 3 })], 'ALL_TIME')
    expect(all.kpis.sessions).toBe(1)
    expect(all.sessionsOutsideRange).toBe(0)

    const none = report([session(at(-1), { repetitions: 0 })])
    expect(none.kpis.bestPeakRomDeg).toBeNull()
    expect(none.kpis.averagePeakRomDeg).toBeNull()
    expect(none.kpis.averageSmoothness).toBeNull()
    expect(none.romTrend).toEqual([])
  })

  it('takes targets from the session first, then the prescribed exercise', () => {
    expect(report([session(at(0))])).toMatchObject({ targetRomDeg: 70, romCeilingDeg: 80, targetRepsPerSession: 10 })
    expect(report([session(at(0), { target_rom_deg: 65, target_reps: 8 })])).toMatchObject({
      targetRomDeg: 65,
      targetRepsPerSession: 8,
    })
  })

  it('always shows the rolling 7-day rep distribution, whatever the range', () => {
    const r = report([session(at(0), { repetitions: 4 }), session(at(-20), { repetitions: 7 })], 'ALL_TIME')
    expect(r.week).toHaveLength(7)
    expect(r.week[6]).toMatchObject({ repetitions: 4, isToday: true, dayLetter: 'Mon' })
    expect(r.week.reduce((sum, d) => sum + d.repetitions, 0)).toBe(4)
  })
})
