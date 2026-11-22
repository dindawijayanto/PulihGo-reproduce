import { supabase } from './supabase'
import type { Exercise, ExerciseSession, Language, Patient, Profile, ProfileForm } from './types'

export async function fetchProfile(userId: string): Promise<Profile | null> {
  const { data, error } = await supabase.from('profiles').select('*').eq('id', userId).maybeSingle()
  if (error) throw error
  return data as Profile | null
}

export async function updateProfile(userId: string, form: ProfileForm): Promise<Profile> {
  const { data, error } = await supabase
    .from('profiles')
    .update({
      full_name: form.fullName,
      email: form.email,
      specialization: form.specialization,
      affiliation: form.affiliation,
      updated_at: new Date().toISOString(),
    })
    .eq('id', userId)
    .select('*')
    .single()
  if (error) throw error
  return data as Profile
}

export async function updatePreferences(
  userId: string,
  prefs: { audio_alerts?: boolean; auto_sync?: boolean; language?: Language },
): Promise<Profile> {
  const { data, error } = await supabase
    .from('profiles')
    .update({ ...prefs, updated_at: new Date().toISOString() })
    .eq('id', userId)
    .select('*')
    .single()
  if (error) throw error
  return data as Profile
}

export async function listAssignedPatients(therapistId: string): Promise<Patient[]> {
  const { data, error } = await supabase
    .from('patients')
    .select('*, profile:profiles!patients_profile_id_fkey(full_name, username)')
    .eq('therapist_id', therapistId)
    .order('id')
  if (error) throw error
  return (data ?? []) as Patient[]
}

export async function getAssignedPatient(therapistId: string, patientId: string): Promise<Patient | null> {
  const { data, error } = await supabase
    .from('patients')
    .select('*, profile:profiles!patients_profile_id_fkey(full_name, username)')
    .eq('therapist_id', therapistId)
    .eq('id', patientId)
    .maybeSingle()
  if (error) throw error
  return data as Patient | null
}

export async function listSessions(patientId: string): Promise<ExerciseSession[]> {
  const { data, error } = await supabase
    .from('exercise_sessions')
    .select('*')
    .eq('patient_id', patientId)
    .order('started_at', { ascending: false })
  if (error) throw error
  return (data ?? []) as ExerciseSession[]
}

export async function listSessionCounts(patientIds: string[]): Promise<Record<string, { count: number; latest: string | null }>> {
  if (patientIds.length === 0) return {}
  const { data, error } = await supabase
    .from('exercise_sessions')
    .select('patient_id, started_at')
    .in('patient_id', patientIds)
  if (error) throw error
  const result: Record<string, { count: number; latest: string | null }> = {}
  for (const id of patientIds) result[id] = { count: 0, latest: null }
  for (const row of data ?? []) {
    const entry = result[row.patient_id]
    entry.count += 1
    if (!entry.latest || row.started_at > entry.latest) entry.latest = row.started_at
  }
  return result
}

export async function getSession(patientId: string, sessionId: string): Promise<ExerciseSession | null> {
  const { data, error } = await supabase
    .from('exercise_sessions')
    .select('*')
    .eq('patient_id', patientId)
    .eq('id', sessionId)
    .maybeSingle()
  if (error) throw error
  return data as ExerciseSession | null
}

export async function listExercises(): Promise<Exercise[]> {
  const { data, error } = await supabase.from('exercises').select('*')
  if (error) throw error
  return (data ?? []) as Exercise[]
}
