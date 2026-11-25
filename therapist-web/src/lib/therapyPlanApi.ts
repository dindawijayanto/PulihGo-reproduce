import { getAssignedPatient } from './api'
import { supabase } from './supabase'
import type { TherapyPlan, TherapyPlanValues } from './therapyPlan'

/** Only a therapist assigned to this patient can read or modify their plan (also enforced by RLS). */
export async function listTherapyPlans(therapistId: string, patientId: string): Promise<TherapyPlan[]> {
  const owned = await getAssignedPatient(therapistId, patientId)
  if (!owned) throw new Error('Patient is not assigned to your therapist account.')
  const { data, error } = await supabase.from('therapy_plans')
    .select('*').eq('patient_id', owned.id).order('exercise_id')
  if (error) throw error
  return (data ?? []) as TherapyPlan[]
}

/**
 * PGF-THR-05 save hotfix:
 * The SQL migration grants UPDATE on prescription fields only; it intentionally does
 * NOT grant UPDATE on patient_id/exercise_id (the immutable identity columns).
 * PostgREST upsert can include those keys in ON CONFLICT DO UPDATE, which conflicts
 * with the restricted column grants. Use a scoped lookup, then INSERT or UPDATE.
 * RLS, the audit trigger and the (patient_id, exercise_id) primary key stay intact.
 * Concurrent first-time creation may produce a duplicate-key error: report that
 * error rather than silently overwriting another therapist's changes.
 */
export async function saveTherapyPlan(
  therapistId: string,
  patientId: string,
  exerciseId: string,
  values: TherapyPlanValues,
): Promise<TherapyPlan> {
  const owned = await getAssignedPatient(therapistId, patientId)
  if (!owned) throw new Error('Patient is not assigned to your therapist account.')

  const { data: existing, error: lookupError } = await supabase.from('therapy_plans')
    .select('patient_id,exercise_id')
    .eq('patient_id', owned.id)
    .eq('exercise_id', exerciseId)
    .maybeSingle()
  if (lookupError) throw lookupError

  // Never place either identity key in an UPDATE payload. The database enforces
  // ownership/assignment in RLS even if the browser request is manipulated.
  const write = existing
    ? supabase.from('therapy_plans').update(values)
      .eq('patient_id', owned.id).eq('exercise_id', exerciseId)
    : supabase.from('therapy_plans').insert({
      patient_id: owned.id,
      exercise_id: exerciseId,
      ...values,
    })
  const { data, error } = await write.select('*').single()
  if (error) throw error
  if (!data || data.patient_id !== owned.id || data.exercise_id !== exerciseId) {
    throw new Error('The saved Therapy Plan could not be verified.')
  }
  return data as TherapyPlan
}
