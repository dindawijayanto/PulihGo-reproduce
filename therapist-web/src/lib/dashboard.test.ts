import { describe, expect, it } from 'vitest'
import { assemble, dayKey, formatAgo, formatLongDate, RECENT_LIMIT, summarize } from './dashboard'
import type { Exercise, ExerciseSession, PainLevel, Patient } from './types'

// All times are built in the local zone so the tests pass in any time zone.
const today = new Date(2026, 10, 23, 15, 0)
const at = (dayOffset: number, hour = 9) => new Date(2026, 10, 23 + dayOffset, hour, 0)

const exercises: Record<string, Exercise> = {
  EX001: { id: 'EX001', name: 'Forearm Supination and Pronation', target_rom_deg: 70, target_reps: 10, rom_ceiling_deg: 80 },
}

function patient(id: string, name: string | null = `Name ${id}`): Patient {
  return {
    id, profile_id: `uuid-${id}`, therapist_id: 'T1', age: 30, affected_arm: 'RIGHT',
    rehabilitation_goal: 'Forearm', notes: null, profile: name == null ? null : { full_name: name, username: id },
  }
}

let seq = 0
function session(patientId: string, ended: Date, opts: Partial<ExerciseSession> & { pain?: PainLevel } = {}): ExerciseSession {
  const { pain, ...rest } = opts
  return {
    id: `S${++seq}`, patient_id: patientId, exercise_id: 'EX001',
    started_at: new Date(ended.getTime() - 180_000).toISOString(), ended_at: ended.toISOString(),
    repetitions: 5, peak_rom_deg: 60, average_smoothness: 0.8, pain_level: pain ?? 'NO_PAIN',
    source: 'DEVICE', target_reps: 10, target_rom_deg: 70, repetition_details: [], synced_at: ended.toISOString(),
    ...rest,
  }
}

describe('assemble', () => {
  it('groups sessions per patient newest first and falls back to the id for a missing name', () => {
    const a = session('P001', at(-2))
    const b = session('P001', at(0))
    const other = session('P999', at(0))
    const cohort = assemble([patient('P001'), patient('P002', null)], [a, b, other])
    expect(cohort[0].sessions.map((s) => s.id)).toEqual([b.id, a.id])
    expect(cohort[1]).toMatchObject({ name: 'P002', sessions: [] })
  })
})

describe('summarize', () => {
  it('is empty when no patient has a session', () => {
    const s = summarize(assemble([patient('P001')], []), exercises, today)
    expect(s.totalSessions).toBe(0)
    expect(s.averageLatestPeakRomDeg).toBeNull()
    expect(s.week).toHaveLength(7)
    expect(s.week.every((d) => d.repetitions === 0)).toBe(true)
  })

  it('counts only the rolling 7 days ending today in the weekly dose and timeline', () => {
    const cohort = assemble(
      [patient('P001'), patient('P002')],
      [
        session('P001', at(0), { repetitions: 4 }),
        session('P001', at(-6), { repetitions: 3 }),
        session('P002', at(-7), { repetitions: 9 }), // just outside the window
        session('P002', at(1), { repetitions: 8 }), // future-dated, ignored
      ],
    )
    const s = summarize(cohort, exercises, today)
    expect(s.totalSessions).toBe(4)
    expect(s.weeklySessions).toBe(2)
    expect(s.weeklyRepetitions).toBe(7)
    expect(s.week[0]).toMatchObject({ date: dayKey(at(-6)), repetitions: 3, isToday: false })
    expect(s.week[6]).toMatchObject({ date: dayKey(today), repetitions: 4, isToday: true })
  })

  it('averages the latest session peak ROM of each patient and reports one shared target', () => {
    const cohort = assemble(
      [patient('P001'), patient('P002'), patient('P003')],
      [
        session('P001', at(-3), { peak_rom_deg: 20 }),
        session('P001', at(-1), { peak_rom_deg: 50 }),
        session('P002', at(-2), { peak_rom_deg: 70 }),
      ],
    )
    const s = summarize(cohort, exercises, today)
    expect(s.patientsWithSessions).toBe(2)
    expect(s.assignedPatients).toBe(3)
    expect(s.averageLatestPeakRomDeg).toBe(60)
    expect(s.targetRomDeg).toBe(70)
  })

  it('drops the target when patients have different targets', () => {
    const cohort = assemble(
      [patient('P001'), patient('P002')],
      [session('P001', at(-1), { target_rom_deg: 60 }), session('P002', at(-1), { target_rom_deg: 70 })],
    )
    expect(summarize(cohort, exercises, today).targetRomDeg).toBeNull()
  })

  it('lists the latest session per patient, newest first, capped at the limit', () => {
    const patients = ['P1', 'P2', 'P3', 'P4', 'P5'].map((id) => patient(id))
    const sessions = patients.map((p, i) => session(p.id, at(-i)))
    const s = summarize(assemble(patients, sessions), exercises, today)
    expect(s.recentActivity).toHaveLength(RECENT_LIMIT)
    expect(s.recentActivity.map((r) => r.patientId)).toEqual(['P1', 'P2', 'P3', 'P4'])
    expect(s.recentActivity[0]).toMatchObject({ exerciseName: 'Forearm Supination and Pronation', outcome: 'COMPLETED' })
  })

  it('flags pain before ceiling and only from each latest session', () => {
    const cohort = assemble(
      [patient('P001', 'Alya'), patient('P002', 'Bima'), patient('P003', 'Citra'), patient('P004', 'Dewi')],
      [
        session('P001', at(-1), { peak_rom_deg: 82 }), // ceiling reached
        session('P002', at(-2), { pain: 'STOPPED_DUE_TO_PAIN', repetitions: 1, peak_rom_deg: 42.7 }),
        session('P003', at(-3), { pain: 'MILD_PAIN' }),
        session('P004', at(-5), { pain: 'STOPPED_DUE_TO_PAIN' }),
        session('P004', at(-1)), // newer pain-free session clears the flag
      ],
    )
    const flags = summarize(cohort, exercises, today).flags
    expect(flags.map((f) => [f.name, f.kind])).toEqual([
      ['Bima', 'PAIN_STOPPED'],
      ['Citra', 'MILD_PAIN'],
      ['Alya', 'CEILING_REACHED'],
    ])
    expect(flags[0].detail).toBe('Stopped the session due to pain after 1 rep (peak 42°).')
    expect(flags[2].detail).toBe('Peak ROM reached 82° against a 80° ceiling.')
  })
})

describe('formatting', () => {
  it('formats the long date and relative time', () => {
    expect(formatLongDate(new Date(2026, 10, 23))).toBe('Monday, November 23, 2026')
    const now = new Date(2026, 10, 23, 12, 0)
    expect(formatAgo(new Date(2026, 10, 23, 12, 0, 30).toISOString(), now)).toBe('just now')
    expect(formatAgo(new Date(2026, 10, 23, 11, 15).toISOString(), now)).toBe('45m ago')
    expect(formatAgo(new Date(2026, 10, 22, 18, 0).toISOString(), now)).toBe('18h ago')
    expect(formatAgo(new Date(2026, 10, 20, 12, 0).toISOString(), now)).toBe('3d ago')
  })
})
