import type { Exercise, ExerciseSession, ProfileForm } from './types'

export type RomOutcome = 'BELOW_TARGET' | 'TARGET_HIT' | 'CEILING_EXCEEDED' | 'UNKNOWN'

const EMAIL = /^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/

export function isValidEmail(value: string): boolean {
  return EMAIL.test(value.trim())
}

export function normalizeProfile(form: ProfileForm): ProfileForm {
  return {
    fullName: form.fullName.trim().replace(/\s+/g, ' '),
    email: form.email.trim().toLowerCase(),
    specialization: form.specialization.trim(),
    affiliation: form.affiliation.trim(),
  }
}

export function validateProfile(form: ProfileForm): string | null {
  const p = normalizeProfile(form)
  if (p.fullName.length < 3) return 'Enter a full name (at least 3 characters).'
  if (!isValidEmail(p.email)) return 'Enter a valid professional email.'
  if (!p.specialization) return 'Enter a clinical specialization.'
  if (!p.affiliation) return 'Enter a clinical affiliation.'
  return null
}

export function profileChanged(saved: ProfileForm, edited: ProfileForm): boolean {
  const a = normalizeProfile(saved)
  const b = normalizeProfile(edited)
  return a.fullName !== b.fullName || a.email !== b.email || a.specialization !== b.specialization || a.affiliation !== b.affiliation
}

export function validateNewPassword(current: string, next: string, confirmation: string): string | null {
  if (!current) return 'Enter your current password.'
  if (next.length < 8) return 'New password must be at least 8 characters.'
  if (!/[^A-Za-z\s]/.test(next)) return 'New password must contain at least one number or symbol.'
  if (next === current) return 'New password must be different from the current one.'
  if (next !== confirmation) return 'Passwords do not match.'
  return null
}

export function initials(name: string): string {
  const words = name.trim().split(/\s+/).filter(Boolean)
  if (words.length === 0) return '?'
  if (words.length === 1) return words[0].slice(0, 2).toUpperCase()
  return (words[0][0] + words[1][0]).toUpperCase()
}

export function patientLoadLabel(count: number): string {
  if (count === 0) return 'No assigned patients'
  if (count === 1) return '1 Assigned Patient'
  return `${count} Assigned Patients`
}

export function romOutcome(peak: number, target: number | null, ceiling: number | null, measured: boolean): RomOutcome {
  if (!measured) return 'UNKNOWN'
  if (ceiling != null && peak > ceiling) return 'CEILING_EXCEEDED'
  if (target == null) return 'UNKNOWN'
  return peak >= target ? 'TARGET_HIT' : 'BELOW_TARGET'
}

export function formatDuration(ms: number): string {
  const total = Math.max(0, Math.floor(ms / 1000))
  const h = Math.floor(total / 3600)
  const m = Math.floor((total % 3600) / 60)
  const s = total % 60
  if (h > 0) return `${h}h ${String(m).padStart(2, '0')}m`
  if (m > 0) return `${m}m ${String(s).padStart(2, '0')}s`
  return `${s}s`
}

export function sessionDurationMs(session: Pick<ExerciseSession, 'started_at' | 'ended_at'>): number {
  return new Date(session.ended_at).getTime() - new Date(session.started_at).getTime()
}

export function sortNewestFirst<T extends Pick<ExerciseSession, 'started_at' | 'id'>>(sessions: T[]): T[] {
  return [...sessions].sort((a, b) => {
    const diff = new Date(b.started_at).getTime() - new Date(a.started_at).getTime()
    return diff !== 0 ? diff : a.id.localeCompare(b.id)
  })
}

export interface SessionTargets {
  targetRomDeg: number | null
  romCeilingDeg: number | null
  targetReps: number | null
  exerciseName: string
}

export function targetsFor(session: ExerciseSession, exercise: Exercise | undefined): SessionTargets {
  return {
    targetRomDeg: session.target_rom_deg ?? exercise?.target_rom_deg ?? null,
    romCeilingDeg: exercise?.rom_ceiling_deg ?? null,
    targetReps: session.target_reps ?? (exercise && exercise.target_reps > 0 ? exercise.target_reps : null),
    exerciseName: exercise?.name ?? session.exercise_id,
  }
}

export function percent(score: number | null): number | null {
  return score == null ? null : Math.round(score * 100)
}

export const painLabel: Record<ExerciseSession['pain_level'], string> = {
  NO_PAIN: 'No pain',
  MILD_PAIN: 'Mild pain',
  STOPPED_DUE_TO_PAIN: 'Stopped: pain',
}

export const outcomeLabel: Record<RomOutcome, string> = {
  TARGET_HIT: 'Target hit',
  BELOW_TARGET: 'Below target',
  CEILING_EXCEEDED: 'Over ceiling',
  UNKNOWN: '',
}
