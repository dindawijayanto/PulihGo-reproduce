import type { Exercise, ExerciseSession, PainLevel, Patient } from './types'

/**
 * PGF-THR-02 — pure dashboard aggregation over sessions synced to Supabase.
 * Adherence and schedules are not computed because no prescription schedule exists yet.
 */

/** One assigned patient with every synced session, newest first. */
export interface PatientWithSessions {
  patient: Patient
  name: string
  sessions: ExerciseSession[]
}

export interface DailyReps {
  /** Local calendar day, `YYYY-MM-DD`. */
  date: string
  dayLetter: string
  repetitions: number
  isToday: boolean
}

export type ActivityOutcome = 'COMPLETED' | 'MILD_PAIN' | 'STOPPED_FOR_PAIN'

export interface RecentActivityRow {
  patientId: string
  sessionId: string
  name: string
  exerciseName: string
  endedAt: string
  repetitions: number
  peakRomDeg: number
  outcome: ActivityOutcome
  seeded: boolean
}

export type FlagKind = 'PAIN_STOPPED' | 'MILD_PAIN' | 'CEILING_REACHED'

export interface MonitoringFlag {
  patientId: string
  name: string
  kind: FlagKind
  detail: string
  endedAt: string
}

export interface DashboardSummary {
  assignedPatients: number
  patientsWithSessions: number
  totalSessions: number
  weeklyRepetitions: number
  weeklySessions: number
  /** Mean of each patient's latest-session peak ROM; null when no patient has a session. */
  averageLatestPeakRomDeg: number | null
  targetRomDeg: number | null
  week: DailyReps[]
  recentActivity: RecentActivityRow[]
  flags: MonitoringFlag[]
}

export const RECENT_LIMIT = 4
const FLAG_ORDER: FlagKind[] = ['PAIN_STOPPED', 'MILD_PAIN', 'CEILING_REACHED']
const DAY_LETTERS = ['S', 'M', 'T', 'W', 'T', 'F', 'S']

const pad = (n: number) => String(n).padStart(2, '0')

/** Local calendar day of a date, `YYYY-MM-DD`. */
export function dayKey(date: Date): string {
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
}

export function addDays(date: Date, days: number): Date {
  return new Date(date.getFullYear(), date.getMonth(), date.getDate() + days)
}

export function startOfDay(date: Date): Date {
  return new Date(date.getFullYear(), date.getMonth(), date.getDate())
}

/** The day a session counts towards is the day it ended, in the viewer's time zone. */
export function sessionDay(s: Pick<ExerciseSession, 'ended_at'>): string {
  return dayKey(new Date(s.ended_at))
}

export function byEndedDesc(a: ExerciseSession, b: ExerciseSession): number {
  const diff = new Date(b.ended_at).getTime() - new Date(a.ended_at).getTime()
  return diff !== 0 ? diff : a.id.localeCompare(b.id)
}

export function assemble(patients: Patient[], sessions: ExerciseSession[]): PatientWithSessions[] {
  return patients.map((p) => ({
    patient: p,
    name: p.profile?.full_name?.trim() || p.id,
    sessions: sessions.filter((s) => s.patient_id === p.id).sort(byEndedDesc),
  }))
}

export function outcomeOf(pain: PainLevel): ActivityOutcome {
  if (pain === 'STOPPED_DUE_TO_PAIN') return 'STOPPED_FOR_PAIN'
  if (pain === 'MILD_PAIN') return 'MILD_PAIN'
  return 'COMPLETED'
}

/** Rolling 7 days ending today, oldest → newest. */
export function weekOf(sessions: ExerciseSession[], today: Date, letters: 'narrow' | 'short' = 'narrow'): DailyReps[] {
  const start = addDays(today, -6)
  const todayKey = dayKey(today)
  return Array.from({ length: 7 }, (_, i) => {
    const date = addDays(start, i)
    const key = dayKey(date)
    return {
      date: key,
      dayLetter: letters === 'narrow'
        ? DAY_LETTERS[date.getDay()]
        : date.toLocaleDateString('en-US', { weekday: 'short' }),
      repetitions: sessions.filter((s) => sessionDay(s) === key).reduce((sum, s) => sum + s.repetitions, 0),
      isToday: key === todayKey,
    }
  })
}

export function summarize(
  patients: PatientWithSessions[],
  exercises: Record<string, Exercise>,
  today: Date,
): DashboardSummary {
  const all = patients.flatMap((p) => p.sessions)
  const weekStart = dayKey(addDays(today, -6))
  const todayKey = dayKey(today)
  const inWeek = all.filter((s) => {
    const d = sessionDay(s)
    return d >= weekStart && d <= todayKey
  })
  const latest = patients.flatMap((p) => (p.sessions[0] ? [{ p, s: p.sessions[0] }] : []))
  const recent = [...latest]
    .sort((a, b) => byEndedDesc(a.s, b.s))
    .slice(0, RECENT_LIMIT)
    .map(({ p, s }) => ({
      patientId: p.patient.id,
      sessionId: s.id,
      name: p.name,
      exerciseName: exercises[s.exercise_id]?.name ?? s.exercise_id,
      endedAt: s.ended_at,
      repetitions: s.repetitions,
      peakRomDeg: s.peak_rom_deg,
      outcome: outcomeOf(s.pain_level),
      seeded: s.source === 'SEEDED',
    }))
  const targets = [
    ...new Set(
      latest
        .map(({ s }) => s.target_rom_deg ?? exercises[s.exercise_id]?.target_rom_deg)
        .filter((t): t is number => t != null),
    ),
  ]
  return {
    assignedPatients: patients.length,
    patientsWithSessions: latest.length,
    totalSessions: all.length,
    weeklyRepetitions: inWeek.reduce((sum, s) => sum + s.repetitions, 0),
    weeklySessions: inWeek.length,
    averageLatestPeakRomDeg: latest.length
      ? latest.reduce((sum, { s }) => sum + s.peak_rom_deg, 0) / latest.length
      : null,
    targetRomDeg: targets.length === 1 ? targets[0] : null,
    week: weekOf(inWeek, today),
    recentActivity: recent,
    flags: flags(latest, exercises),
  }
}

/** Flags look only at each patient's latest session, ordered by severity then recency. */
function flags(
  latest: { p: PatientWithSessions; s: ExerciseSession }[],
  exercises: Record<string, Exercise>,
): MonitoringFlag[] {
  const result: MonitoringFlag[] = []
  for (const { p, s } of latest) {
    const rom = Math.trunc(s.peak_rom_deg)
    const ceiling = exercises[s.exercise_id]?.rom_ceiling_deg
    const base = { patientId: p.patient.id, name: p.name, endedAt: s.ended_at }
    if (s.pain_level === 'STOPPED_DUE_TO_PAIN') {
      const reps = `${s.repetitions} ${s.repetitions === 1 ? 'rep' : 'reps'}`
      result.push({ ...base, kind: 'PAIN_STOPPED', detail: `Stopped the session due to pain after ${reps} (peak ${rom}°).` })
    } else if (s.pain_level === 'MILD_PAIN') {
      result.push({ ...base, kind: 'MILD_PAIN', detail: `Reported mild pain after the last session (peak ${rom}°).` })
    } else if (ceiling != null && ceiling > 0 && s.peak_rom_deg >= ceiling) {
      result.push({ ...base, kind: 'CEILING_REACHED', detail: `Peak ROM reached ${rom}° against a ${Math.trunc(ceiling)}° ceiling.` })
    }
  }
  return result.sort(
    (a, b) =>
      FLAG_ORDER.indexOf(a.kind) - FLAG_ORDER.indexOf(b.kind) ||
      new Date(b.endedAt).getTime() - new Date(a.endedAt).getTime(),
  )
}

/** "Monday, November 23, 2026" as in the Figma date pill. */
export function formatLongDate(date: Date): string {
  return date.toLocaleDateString('en-US', { weekday: 'long', month: 'long', day: 'numeric', year: 'numeric' })
}

/** "18h ago" / "3d ago" / "just now" for flag and activity recency. */
export function formatAgo(iso: string, now: Date): string {
  const minutes = Math.max(0, Math.floor((now.getTime() - new Date(iso).getTime()) / 60000))
  if (minutes < 1) return 'just now'
  if (minutes < 60) return `${minutes}m ago`
  if (minutes < 60 * 24) return `${Math.floor(minutes / 60)}h ago`
  return `${Math.floor(minutes / (60 * 24))}d ago`
}
