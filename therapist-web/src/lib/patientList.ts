import { addDays, dayKey, type PatientWithSessions } from './dashboard'
import type { PainLevel } from './types'

/**
 * PGF-THR-03 — Patient List & Search rules, ported from the Android TherapistPatientListLogic.
 *
 * - Only patients assigned to the therapist are listed (RLS + the assigned-patients query).
 * - "Needs follow-up" only means the latest synced session carried a pain report.
 *   It is a review prompt, not a clinical diagnosis.
 * - Adherence and scheduled reviews are not computed: there is no schedule data yet.
 */

export type PatientStatus = 'NO_SESSIONS' | 'ON_TRACK' | 'NEEDS_FOLLOW_UP'
export type PatientListFilter = 'ALL' | 'ACTIVE' | 'NEEDS_FOLLOW_UP'
export type PatientListSort = 'MOST_RECENT_SESSION' | 'NAME_ASC'

export const filterOptions: { value: PatientListFilter; label: string }[] = [
  { value: 'ALL', label: 'All' },
  { value: 'ACTIVE', label: 'Active' },
  { value: 'NEEDS_FOLLOW_UP', label: 'Needs Follow-up' },
]

export const sortOptions: { value: PatientListSort; label: string }[] = [
  { value: 'MOST_RECENT_SESSION', label: 'Most Recent Session' },
  { value: 'NAME_ASC', label: 'Name (A–Z)' },
]

export const PAGE_SIZE = 8

export interface LastSessionSummary {
  id: string
  endedAt: string
  repetitions: number
  peakRomDeg: number
  averageSmoothness: number | null
  painLevel: PainLevel
}

export interface PatientListItem {
  patientId: string
  name: string
  username: string
  age: number | null
  affectedArm: string | null
  rehabilitationGoal: string | null
  sessionCount: number
  totalRepetitions: number
  lastSession: LastSessionSummary | null
  status: PatientStatus
}

export interface CohortOverview {
  totalPatients: number
  activePatients: number
  needsFollowUp: number
  totalRepetitions: number
}

export interface PatientPage {
  items: PatientListItem[]
  pageIndex: number
  pageCount: number
  totalMatches: number
}

export function buildItems(cohort: PatientWithSessions[]): PatientListItem[] {
  return cohort.map(({ patient, name, sessions }) => {
    // Sessions arrive newest first from assemble().
    const latest = sessions[0]
    const lastSession = latest
      ? {
          id: latest.id,
          endedAt: latest.ended_at,
          repetitions: latest.repetitions,
          peakRomDeg: latest.peak_rom_deg,
          averageSmoothness: latest.average_smoothness,
          painLevel: latest.pain_level,
        }
      : null
    return {
      patientId: patient.id,
      name,
      username: patient.profile?.username ?? '',
      age: patient.age,
      affectedArm: patient.affected_arm,
      rehabilitationGoal: patient.rehabilitation_goal,
      sessionCount: sessions.length,
      totalRepetitions: sessions.reduce((sum, s) => sum + s.repetitions, 0),
      lastSession,
      status: !lastSession ? 'NO_SESSIONS' : lastSession.painLevel !== 'NO_PAIN' ? 'NEEDS_FOLLOW_UP' : 'ON_TRACK',
    }
  })
}

export function matchesQuery(item: PatientListItem, query: string): boolean {
  const q = query.trim().toLowerCase()
  if (!q) return true
  return (
    item.name.toLowerCase().includes(q) ||
    item.patientId.toLowerCase().includes(q) ||
    (item.username !== '' && item.username.toLowerCase().includes(q))
  )
}

export function matchesFilter(item: PatientListItem, filter: PatientListFilter): boolean {
  if (filter === 'ACTIVE') return item.sessionCount > 0
  if (filter === 'NEEDS_FOLLOW_UP') return item.status === 'NEEDS_FOLLOW_UP'
  return true
}

const byName = (a: PatientListItem, b: PatientListItem) =>
  a.name.localeCompare(b.name, 'en', { sensitivity: 'base' }) || a.patientId.localeCompare(b.patientId)

export function sortItems(items: PatientListItem[], sort: PatientListSort): PatientListItem[] {
  if (sort === 'NAME_ASC') return [...items].sort(byName)
  // Patients without sessions go last; ties fall back to name then id for stability.
  const time = (i: PatientListItem) => (i.lastSession ? new Date(i.lastSession.endedAt).getTime() : -Infinity)
  return [...items].sort((a, b) => time(b) - time(a) || byName(a, b))
}

export function applyListControls(
  items: PatientListItem[],
  query: string,
  filter: PatientListFilter,
  sort: PatientListSort,
): PatientListItem[] {
  return sortItems(items.filter((i) => matchesQuery(i, query) && matchesFilter(i, filter)), sort)
}

/** Clamps the requested page into range so a narrowed search never shows an empty page. */
export function page(items: PatientListItem[], requestedPage: number, pageSize = PAGE_SIZE): PatientPage {
  const pageCount = Math.max(1, Math.ceil(items.length / pageSize))
  const pageIndex = Math.min(Math.max(0, Math.trunc(requestedPage) || 0), pageCount - 1)
  return {
    items: items.slice(pageIndex * pageSize, (pageIndex + 1) * pageSize),
    pageIndex,
    pageCount,
    totalMatches: items.length,
  }
}

export function overview(items: PatientListItem[]): CohortOverview {
  return {
    totalPatients: items.length,
    activePatients: items.filter((i) => i.sessionCount > 0).length,
    needsFollowUp: items.filter((i) => i.status === 'NEEDS_FOLLOW_UP').length,
    totalRepetitions: items.reduce((sum, i) => sum + i.totalRepetitions, 0),
  }
}

/** Figma "Today, 14:20" / "Yesterday, 16:45" / "Oct 13, 10:15" in the viewer's zone. */
export function formatSessionTime(iso: string, now: Date): string {
  const t = new Date(iso)
  const clock = t.toLocaleTimeString('en-GB', { hour: '2-digit', minute: '2-digit' })
  const key = dayKey(t)
  if (key === dayKey(now)) return `Today, ${clock}`
  if (key === dayKey(addDays(now, -1))) return `Yesterday, ${clock}`
  return `${t.toLocaleDateString('en-US', { month: 'short', day: 'numeric' })}, ${clock}`
}
