import { addDays, byEndedDesc, dayKey, sessionDay, weekOf, type DailyReps, type PatientWithSessions } from './dashboard'
import { sessionDurationMs } from './logic'
import type { Exercise, ExerciseSession } from './types'

/**
 * PGF-THR-06 — per-patient report data for the Reports page. KPI definitions here are simple
 * aggregates of synced sessions; PGF-THR-07 (Erza) owns the final KPI calculations.
 */

export type ReportRange = 'LAST_7_DAYS' | 'LAST_30_DAYS' | 'ALL_TIME'

export const reportRanges: { value: ReportRange; label: string; days: number | null }[] = [
  { value: 'LAST_7_DAYS', label: 'Last 7 Days', days: 7 },
  { value: 'LAST_30_DAYS', label: 'Last 30 Days', days: 30 },
  { value: 'ALL_TIME', label: 'All Time', days: null },
]

export interface ReportKpis {
  sessions: number
  totalRepetitions: number
  bestPeakRomDeg: number | null
  averagePeakRomDeg: number | null
  /** Mean of the sessions that measured smoothness; null when none did. */
  averageSmoothness: number | null
  painReportedSessions: number
}

export interface RomTrendPoint {
  label: string
  peakRomDeg: number
}

export type ReportSessionStatus = 'COMPLETED' | 'MILD_PAIN' | 'STOPPED_FOR_PAIN' | 'NO_REPETITIONS'

export interface ReportSessionRow {
  id: string
  startedAt: string
  exerciseName: string
  repetitions: number
  peakRomDeg: number
  durationMs: number
  status: ReportSessionStatus
  seeded: boolean
}

export interface PatientReport {
  patientId: string
  name: string
  goal: string
  range: ReportRange
  rangeLabel: string
  kpis: ReportKpis
  /** Sessions with repetitions in the range, oldest → newest. */
  romTrend: RomTrendPoint[]
  targetRomDeg: number | null
  romCeilingDeg: number | null
  /** Always the rolling 7 days ending today, as in Figma's "Weekly Rep Distribution". */
  week: DailyReps[]
  targetRepsPerSession: number | null
  /** Newest first. */
  rows: ReportSessionRow[]
  /** Sessions recorded for this patient outside the selected range. */
  sessionsOutsideRange: number
}

const shortDay = (d: Date) => d.toLocaleDateString('en-GB', { day: 'numeric', month: 'short' })

export function rangeStart(range: ReportRange, today: Date): Date | null {
  const days = reportRanges.find((r) => r.value === range)!.days
  return days == null ? null : addDays(today, -(days - 1))
}

export function rangeLabel(range: ReportRange, today: Date): string {
  const label = reportRanges.find((r) => r.value === range)!.label
  const start = rangeStart(range, today)
  return start ? `${label} (${shortDay(start)} – ${shortDay(today)})` : label
}

export function statusOf(s: ExerciseSession): ReportSessionStatus {
  // Pain feedback outranks the repetition count so the therapist never loses it.
  if (s.pain_level === 'STOPPED_DUE_TO_PAIN') return 'STOPPED_FOR_PAIN'
  if (s.pain_level === 'MILD_PAIN') return 'MILD_PAIN'
  if (s.repetitions === 0) return 'NO_REPETITIONS'
  return 'COMPLETED'
}

const average = (values: number[]) => (values.length ? values.reduce((a, b) => a + b, 0) / values.length : null)

export function buildReport(
  patient: PatientWithSessions,
  range: ReportRange,
  exercises: Record<string, Exercise>,
  today: Date,
): PatientReport {
  const start = rangeStart(range, today)
  const startKey = start ? dayKey(start) : null
  const todayKey = dayKey(today)
  const all = [...patient.sessions].sort(byEndedDesc)
  const inRange = all.filter((s) => {
    const d = sessionDay(s)
    return (startKey == null || d >= startKey) && d <= todayKey
  })
  const withReps = inRange.filter((s) => s.repetitions > 0)
  const exerciseIds = [...new Set(all.map((s) => s.exercise_id))]
  const exercise = exerciseIds.length === 1 ? exercises[exerciseIds[0]] : undefined
  const sessionTargetRom = all.find((s) => s.target_rom_deg != null)?.target_rom_deg ?? null
  const sessionTargetReps = all.find((s) => s.target_reps != null)?.target_reps ?? null

  return {
    patientId: patient.patient.id,
    name: patient.name,
    goal: patient.patient.rehabilitation_goal ?? '',
    range,
    rangeLabel: rangeLabel(range, today),
    kpis: {
      sessions: inRange.length,
      totalRepetitions: inRange.reduce((sum, s) => sum + s.repetitions, 0),
      bestPeakRomDeg: withReps.length ? Math.max(...withReps.map((s) => s.peak_rom_deg)) : null,
      averagePeakRomDeg: average(withReps.map((s) => s.peak_rom_deg)),
      averageSmoothness: average(inRange.flatMap((s) => (s.average_smoothness == null ? [] : [s.average_smoothness]))),
      painReportedSessions: inRange.filter((s) => s.pain_level !== 'NO_PAIN').length,
    },
    romTrend: [...withReps].reverse().map((s) => ({ label: shortDay(new Date(s.ended_at)), peakRomDeg: s.peak_rom_deg })),
    targetRomDeg: sessionTargetRom ?? exercise?.target_rom_deg ?? null,
    romCeilingDeg: exercise?.rom_ceiling_deg ?? null,
    week: weekOf(all, today, 'short'),
    targetRepsPerSession: sessionTargetReps ?? (exercise && exercise.target_reps > 0 ? exercise.target_reps : null),
    rows: inRange.map((s) => ({
      id: s.id,
      startedAt: s.started_at,
      exerciseName: exercises[s.exercise_id]?.name ?? s.exercise_id,
      repetitions: s.repetitions,
      peakRomDeg: s.peak_rom_deg,
      durationMs: sessionDurationMs(s),
      status: statusOf(s),
      seeded: s.source === 'SEEDED',
    })),
    sessionsOutsideRange: all.length - inRange.length,
  }
}
