import { describe, expect, it } from 'vitest'
import {
  formatDuration,
  initials,
  isValidEmail,
  normalizeProfile,
  patientLoadLabel,
  percent,
  profileChanged,
  romOutcome,
  sessionDurationMs,
  sortNewestFirst,
  targetsFor,
  validateNewPassword,
  validateProfile,
} from './logic'
import type { Exercise, ExerciseSession, ProfileForm } from './types'

const profile: ProfileForm = {
  fullName: 'Demo Therapist',
  email: 'therapist@pulihgo.demo',
  specialization: 'Upper Limb Rehabilitation',
  affiliation: 'PulihGo Digital Tele-Rehab Hub',
}

const exercise: Exercise = { id: 'EX001', name: 'Forearm Supination', target_rom_deg: 70, target_reps: 10, rom_ceiling_deg: 80 }

function session(patch: Partial<ExerciseSession> = {}): ExerciseSession {
  return {
    id: 'S001',
    patient_id: 'P001',
    exercise_id: 'EX001',
    started_at: '2026-11-08T09:00:00+07:00',
    ended_at: '2026-11-08T09:03:00+07:00',
    repetitions: 3,
    peak_rom_deg: 56,
    average_smoothness: 0.82,
    pain_level: 'NO_PAIN',
    source: 'SEEDED',
    target_reps: null,
    target_rom_deg: null,
    repetition_details: [],
    synced_at: '2026-11-22T00:00:00Z',
    ...patch,
  }
}

describe('profile', () => {
  it('accepts a valid profile', () => {
    expect(validateProfile(profile)).toBeNull()
  })

  it('returns the first invalid field', () => {
    expect(validateProfile({ ...profile, fullName: ' a ' })).toBe('Enter a full name (at least 3 characters).')
    expect(validateProfile({ ...profile, email: 'carl@clinic' })).toBe('Enter a valid professional email.')
    expect(validateProfile({ ...profile, specialization: '  ' })).toBe('Enter a clinical specialization.')
    expect(validateProfile({ ...profile, affiliation: '' })).toBe('Enter a clinical affiliation.')
  })

  it('normalizes whitespace and email case', () => {
    expect(normalizeProfile({ ...profile, fullName: '  Carl   Manfred ', email: ' Carl@Clinic.ORG ' })).toMatchObject({
      fullName: 'Carl Manfred',
      email: 'carl@clinic.org',
    })
  })

  it('ignores whitespace-only edits', () => {
    expect(profileChanged(profile, { ...profile, fullName: ' Demo Therapist ' })).toBe(false)
    expect(profileChanged(profile, { ...profile, email: 'THERAPIST@pulihgo.demo' })).toBe(false)
    expect(profileChanged(profile, { ...profile, affiliation: 'RS Pulih' })).toBe(true)
  })

  it('builds initials and patient load labels', () => {
    expect(initials('Carl Manfred')).toBe('CM')
    expect(initials('alya')).toBe('AL')
    expect(initials('  ')).toBe('?')
    expect(patientLoadLabel(0)).toBe('No assigned patients')
    expect(patientLoadLabel(1)).toBe('1 Assigned Patient')
    expect(patientLoadLabel(8)).toBe('8 Assigned Patients')
  })

  it('validates emails', () => {
    expect(isValidEmail(' therapist@pulihgo.demo ')).toBe(true)
    expect(isValidEmail('therapist@pulihgo')).toBe(false)
  })
})

describe('password change', () => {
  it('enforces the password rules', () => {
    expect(validateNewPassword('', 'newpass99', 'newpass99')).toBe('Enter your current password.')
    expect(validateNewPassword('demo123', 'a1', 'a1')).toBe('New password must be at least 8 characters.')
    expect(validateNewPassword('demo123', 'onlyletters', 'onlyletters')).toBe('New password must contain at least one number or symbol.')
    expect(validateNewPassword('newpass99', 'newpass99', 'newpass99')).toBe('New password must be different from the current one.')
    expect(validateNewPassword('demo123', 'newpass99', 'newpass98')).toBe('Passwords do not match.')
    expect(validateNewPassword('demo123', 'newpass99', 'newpass99')).toBeNull()
  })
})

describe('sessions', () => {
  it('classifies ROM against target and ceiling', () => {
    expect(romOutcome(70, 70, 80, true)).toBe('TARGET_HIT')
    expect(romOutcome(80, 70, 80, true)).toBe('TARGET_HIT')
    expect(romOutcome(80.1, 70, 80, true)).toBe('CEILING_EXCEEDED')
    expect(romOutcome(69.9, 70, 80, true)).toBe('BELOW_TARGET')
    expect(romOutcome(90, null, null, true)).toBe('UNKNOWN')
    expect(romOutcome(70, 70, 80, false)).toBe('UNKNOWN')
  })

  it('formats durations', () => {
    expect(formatDuration(9_500)).toBe('9s')
    expect(formatDuration(180_000)).toBe('3m 00s')
    expect(formatDuration(3_780_000)).toBe('1h 03m')
    expect(formatDuration(-5)).toBe('0s')
    expect(sessionDurationMs(session())).toBe(180_000)
  })

  it('prefers session targets over exercise defaults', () => {
    expect(targetsFor(session({ target_rom_deg: 60, target_reps: 8 }), exercise)).toEqual({
      targetRomDeg: 60,
      romCeilingDeg: 80,
      targetReps: 8,
      exerciseName: 'Forearm Supination',
    })
    expect(targetsFor(session(), exercise).targetRomDeg).toBe(70)
    expect(targetsFor(session({ exercise_id: 'EX999' }), undefined)).toEqual({
      targetRomDeg: null,
      romCeilingDeg: null,
      targetReps: null,
      exerciseName: 'EX999',
    })
  })

  it('sorts newest first with a stable tie-break', () => {
    const list = [
      session({ id: 'B', started_at: '2026-11-08T09:00:00Z' }),
      session({ id: 'C', started_at: '2026-11-09T09:00:00Z' }),
      session({ id: 'A', started_at: '2026-11-08T09:00:00Z' }),
    ]
    expect(sortNewestFirst(list).map((s) => s.id)).toEqual(['C', 'A', 'B'])
  })

  it('converts smoothness to percent', () => {
    expect(percent(0.756)).toBe(76)
    expect(percent(null)).toBeNull()
  })
})
