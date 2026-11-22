export type Role = 'PATIENT' | 'THERAPIST'
export type PainLevel = 'NO_PAIN' | 'MILD_PAIN' | 'STOPPED_DUE_TO_PAIN'
export type SessionSource = 'SEEDED' | 'DEVICE'
export type Language = 'ENGLISH' | 'INDONESIAN'

export interface Profile {
  id: string
  role: Role
  username: string
  full_name: string
  email: string
  legacy_user_id: string | null
  specialization: string | null
  affiliation: string | null
  audio_alerts: boolean
  auto_sync: boolean
  language: Language
  updated_at: string
}

export interface Patient {
  id: string
  profile_id: string
  therapist_id: string | null
  age: number | null
  affected_arm: string | null
  rehabilitation_goal: string | null
  notes: string | null
  profile?: Pick<Profile, 'full_name' | 'username'> | null
}

export interface Exercise {
  id: string
  name: string
  target_rom_deg: number
  target_reps: number
  rom_ceiling_deg: number
}

export interface RepetitionDetail {
  repetitionNumber: number
  peakRomDeg: number
  durationMs: number
  smoothnessScore: number | null
}

export interface ExerciseSession {
  id: string
  patient_id: string
  exercise_id: string
  started_at: string
  ended_at: string
  repetitions: number
  peak_rom_deg: number
  average_smoothness: number | null
  pain_level: PainLevel
  source: SessionSource
  target_reps: number | null
  target_rom_deg: number | null
  repetition_details: RepetitionDetail[]
  synced_at: string
}

export interface ProfileForm {
  fullName: string
  email: string
  specialization: string
  affiliation: string
}
