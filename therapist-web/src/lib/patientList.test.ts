import { describe, expect, it } from 'vitest'
import { assemble } from './dashboard'
import {
  applyListControls,
  buildItems,
  formatSessionTime,
  matchesQuery,
  overview,
  page,
  type PatientListItem,
} from './patientList'
import type { ExerciseSession, PainLevel, Patient } from './types'

const at = (dayOffset: number, hour = 9, minute = 0) => new Date(2026, 10, 23 + dayOffset, hour, minute)

function patient(id: string, name: string, username: string): Patient {
  return {
    id, profile_id: `uuid-${id}`, therapist_id: 'T1', age: 30, affected_arm: 'RIGHT',
    rehabilitation_goal: 'Forearm', notes: null, profile: { full_name: name, username },
  }
}

let seq = 0
function session(patientId: string, ended: Date, reps = 5, pain: PainLevel = 'NO_PAIN'): ExerciseSession {
  return {
    id: `S${++seq}`, patient_id: patientId, exercise_id: 'EX001',
    started_at: new Date(ended.getTime() - 120_000).toISOString(), ended_at: ended.toISOString(),
    repetitions: reps, peak_rom_deg: 55, average_smoothness: 0.8, pain_level: pain,
    source: 'DEVICE', target_reps: 10, target_rom_deg: 70, repetition_details: [], synced_at: ended.toISOString(),
  }
}

const items = buildItems(
  assemble(
    [patient('P001', 'Alya Putri', 'patient01'), patient('P002', 'Bima Pratama', 'patient02'), patient('P003', 'citra Dewi', 'patient03')],
    [
      session('P001', at(-3), 4),
      session('P001', at(-1), 6),
      session('P002', at(-2), 2, 'STOPPED_DUE_TO_PAIN'),
    ],
  ),
)
const byId = (id: string) => items.find((i) => i.patientId === id)!

describe('buildItems', () => {
  it('summarises sessions and derives the status from the latest one', () => {
    expect(byId('P001')).toMatchObject({ sessionCount: 2, totalRepetitions: 10, status: 'ON_TRACK' })
    expect(byId('P001').lastSession?.repetitions).toBe(6)
    expect(byId('P002').status).toBe('NEEDS_FOLLOW_UP')
    expect(byId('P003')).toMatchObject({ sessionCount: 0, lastSession: null, status: 'NO_SESSIONS' })
  })
})

describe('search, filter and sort', () => {
  it('matches name, id and username case-insensitively', () => {
    expect(matchesQuery(byId('P001'), '  alya ')).toBe(true)
    expect(matchesQuery(byId('P001'), 'p001')).toBe(true)
    expect(matchesQuery(byId('P002'), 'PATIENT02')).toBe(true)
    expect(matchesQuery(byId('P002'), 'zzz')).toBe(false)
  })

  it('filters active and follow-up patients', () => {
    const ids = (list: PatientListItem[]) => list.map((i) => i.patientId)
    expect(ids(applyListControls(items, '', 'ACTIVE', 'NAME_ASC'))).toEqual(['P001', 'P002'])
    expect(ids(applyListControls(items, '', 'NEEDS_FOLLOW_UP', 'NAME_ASC'))).toEqual(['P002'])
    expect(ids(applyListControls(items, 'alya', 'NEEDS_FOLLOW_UP', 'NAME_ASC'))).toEqual([])
  })

  it('sorts by most recent session with no-session patients last, or by name ignoring case', () => {
    const ids = (list: PatientListItem[]) => list.map((i) => i.patientId)
    expect(ids(applyListControls(items, '', 'ALL', 'MOST_RECENT_SESSION'))).toEqual(['P001', 'P002', 'P003'])
    expect(ids(applyListControls(items, '', 'ALL', 'NAME_ASC'))).toEqual(['P001', 'P002', 'P003'])
  })
})

describe('page', () => {
  it('splits into pages and clamps out-of-range requests', () => {
    expect(page(items, 0, 2)).toMatchObject({ pageIndex: 0, pageCount: 2, totalMatches: 3 })
    expect(page(items, 1, 2).items).toHaveLength(1)
    expect(page(items, 9, 2).pageIndex).toBe(1)
    expect(page([], 3, 2)).toMatchObject({ pageIndex: 0, pageCount: 1, items: [] })
  })
})

describe('overview and time', () => {
  it('counts the cohort', () => {
    expect(overview(items)).toEqual({ totalPatients: 3, activePatients: 2, needsFollowUp: 1, totalRepetitions: 12 })
  })

  it('formats the last session time like Figma', () => {
    const now = at(0, 15)
    expect(formatSessionTime(at(0, 14, 20).toISOString(), now)).toBe('Today, 14:20')
    expect(formatSessionTime(at(-1, 16, 45).toISOString(), now)).toBe('Yesterday, 16:45')
    expect(formatSessionTime(new Date(2026, 9, 13, 10, 15).toISOString(), now)).toBe('Oct 13, 10:15')
  })
})
