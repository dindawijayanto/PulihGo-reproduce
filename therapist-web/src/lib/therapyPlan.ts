import type { Exercise } from './types'

/** PGF-THR-05: patient/exercise-specific prescription, persisted in Supabase. */
export interface TherapyPlan {
  patient_id: string
  exercise_id: string
  target_rom_deg: number
  rom_ceiling_deg: number
  target_reps: number
  sets_per_session: number
  sessions_per_week: number
  notes: string
  is_active: boolean
  created_at: string
  updated_at: string
  updated_by: string
}

/** Keep numeric inputs as strings until validation; an empty input is not a zero prescription. */
export interface TherapyPlanDraft {
  targetRomDeg: string
  romCeilingDeg: string
  targetReps: string
  setsPerSession: string
  sessionsPerWeek: string
  notes: string
  isActive: boolean
}

export interface TherapyPlanValues {
  target_rom_deg: number
  rom_ceiling_deg: number
  target_reps: number
  sets_per_session: number
  sessions_per_week: number
  notes: string
  is_active: boolean
}

export const THERAPY_PLAN_LIMITS = {
  maxAngle: 180,
  maxReps: 100,
  maxSets: 10,
  maxSessionsPerWeek: 7,
  maxNotes: 500,
} as const

/** Catalog defaults are display-only until the therapist explicitly saves them. */
export function draftForExercise(exercise: Exercise, saved?: TherapyPlan | null): TherapyPlanDraft {
  return {
    targetRomDeg: String(saved?.target_rom_deg ?? exercise.target_rom_deg),
    romCeilingDeg: String(saved?.rom_ceiling_deg ?? exercise.rom_ceiling_deg),
    targetReps: String(saved?.target_reps ?? exercise.target_reps),
    setsPerSession: String(saved?.sets_per_session ?? 1),
    sessionsPerWeek: String(saved?.sessions_per_week ?? 3),
    notes: saved?.notes ?? '',
    isActive: saved?.is_active ?? true,
  }
}

/** Do not attribute plans for a different patient to this page. */
export function plansForPatient(patientId: string, input: TherapyPlan[]): TherapyPlan[] {
  return input.filter((plan) => plan.patient_id === patientId).sort((a, b) => a.exercise_id.localeCompare(b.exercise_id))
}

/** A saved plan is never silently replaced by catalog defaults. */
export function selectedPlan(plans: TherapyPlan[], exerciseId: string): TherapyPlan | null {
  return plans.find((p) => p.exercise_id === exerciseId) ?? null
}

function positiveNumber(raw: string): number | null {
  if (!raw.trim()) return null
  const n = Number(raw)
  return Number.isFinite(n) && n > 0 ? n : null
}

function positiveInteger(raw: string, max: number): number | null {
  const n = positiveNumber(raw)
  return n != null && Number.isInteger(n) && n <= max ? n : null
}

export function validateTherapyPlan(draft: TherapyPlanDraft): string | null {
  const target = positiveNumber(draft.targetRomDeg)
  const ceiling = positiveNumber(draft.romCeilingDeg)
  if (target == null || target >= THERAPY_PLAN_LIMITS.maxAngle) {
    return 'Target ROM must be greater than 0° and below 180°.'
  }
  if (ceiling == null || ceiling > THERAPY_PLAN_LIMITS.maxAngle || ceiling <= target) {
    return 'Safe ROM ceiling must be greater than target ROM and no more than 180°.'
  }
  if (positiveInteger(draft.targetReps, THERAPY_PLAN_LIMITS.maxReps) == null) {
    return 'Target repetitions must be a whole number from 1 to 100.'
  }
  if (positiveInteger(draft.setsPerSession, THERAPY_PLAN_LIMITS.maxSets) == null) {
    return 'Sets per session must be a whole number from 1 to 10.'
  }
  if (positiveInteger(draft.sessionsPerWeek, THERAPY_PLAN_LIMITS.maxSessionsPerWeek) == null) {
    return 'Sessions per week must be a whole number from 1 to 7.'
  }
  if (draft.notes.trim().length > THERAPY_PLAN_LIMITS.maxNotes) {
    return `Notes cannot exceed ${THERAPY_PLAN_LIMITS.maxNotes} characters.`
  }
  return null
}

/** Call ONLY after validateTherapyPlan returns null; keep storage types explicit. */
export function planValues(draft: TherapyPlanDraft): TherapyPlanValues {
  const error = validateTherapyPlan(draft)
  if (error) throw new Error(error)
  return {
    target_rom_deg: Number(draft.targetRomDeg),
    rom_ceiling_deg: Number(draft.romCeilingDeg),
    target_reps: Number(draft.targetReps),
    sets_per_session: Number(draft.setsPerSession),
    sessions_per_week: Number(draft.sessionsPerWeek),
    notes: draft.notes.trim(),
    is_active: draft.isActive,
  }
}

/** Compare normalized values, not differences in input-string formatting. */
export function planDraftChanged(current: TherapyPlanDraft, initial: TherapyPlanDraft): boolean {
  const normalize = (draft: TherapyPlanDraft) => ({
    targetRomDeg: draft.targetRomDeg.trim(),
    romCeilingDeg: draft.romCeilingDeg.trim(),
    targetReps: draft.targetReps.trim(),
    setsPerSession: draft.setsPerSession.trim(),
    sessionsPerWeek: draft.sessionsPerWeek.trim(),
    notes: draft.notes.trim(),
    isActive: draft.isActive,
  })
  return JSON.stringify(normalize(current)) !== JSON.stringify(normalize(initial))
}
