import { useCallback, useEffect, useState } from 'react'
import { listAssignedPatients, listExercises, listSessionsForPatients } from './api'
import { useAuth } from './auth'
import { assemble, type PatientWithSessions } from './dashboard'
import type { Exercise } from './types'

const REFRESH_MS = 15000

/** Assigned patients with their synced sessions; re-polls while the therapist has Auto-sync on. */
export function useCohort() {
  const { profile } = useAuth()
  const [cohort, setCohort] = useState<PatientWithSessions[] | null>(null)
  const [exercises, setExercises] = useState<Record<string, Exercise>>({})
  const [error, setError] = useState<string | null>(null)
  const [updatedAt, setUpdatedAt] = useState<Date | null>(null)
  const [loading, setLoading] = useState(false)

  const load = useCallback(async () => {
    if (!profile) return
    setLoading(true)
    try {
      const patients = await listAssignedPatients(profile.id)
      const [sessions, ex] = await Promise.all([
        listSessionsForPatients(patients.map((p) => p.id)),
        listExercises(),
      ])
      setCohort(assemble(patients, sessions))
      setExercises(Object.fromEntries(ex.map((e) => [e.id, e])))
      setUpdatedAt(new Date())
      setError(null)
    } catch {
      setError('Could not load patient sessions from the cloud.')
    } finally {
      setLoading(false)
    }
  }, [profile])

  useEffect(() => {
    load()
    if (!profile?.auto_sync) return
    const timer = window.setInterval(load, REFRESH_MS)
    return () => window.clearInterval(timer)
  }, [load, profile?.auto_sync])

  return { cohort, exercises, error, updatedAt, loading, reload: load, therapistName: profile?.full_name ?? 'Therapist' }
}
