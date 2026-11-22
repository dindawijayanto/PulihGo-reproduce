import { useCallback, useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { getAssignedPatient, listExercises, listSessions } from '../lib/api'
import { useAuth } from '../lib/auth'
import { formatDuration, painLabel, sessionDurationMs } from '../lib/logic'
import type { Exercise, ExerciseSession, Patient } from '../lib/types'
import { PainPill } from './SessionDetailPage'

const REFRESH_MS = 15000

export function PatientSessionsPage() {
  const { patientId = '' } = useParams()
  const { profile } = useAuth()
  const navigate = useNavigate()
  const [patient, setPatient] = useState<Patient | null | undefined>(undefined)
  const [sessions, setSessions] = useState<ExerciseSession[]>([])
  const [exercises, setExercises] = useState<Record<string, Exercise>>({})
  const [error, setError] = useState<string | null>(null)
  const [updatedAt, setUpdatedAt] = useState<Date | null>(null)

  const load = useCallback(async () => {
    if (!profile) return
    try {
      const owned = await getAssignedPatient(profile.id, patientId)
      setPatient(owned)
      if (!owned) return
      const [list, ex] = await Promise.all([listSessions(owned.id), listExercises()])
      setSessions(list)
      setExercises(Object.fromEntries(ex.map((e) => [e.id, e])))
      setUpdatedAt(new Date())
      setError(null)
    } catch {
      setError('Could not load sessions from the cloud.')
    }
  }, [profile, patientId])

  useEffect(() => {
    load()
    if (!profile?.auto_sync) return
    const timer = window.setInterval(load, REFRESH_MS)
    return () => window.clearInterval(timer)
  }, [load, profile?.auto_sync])

  if (patient === null) {
    return (
      <div className="card empty">
        <h2>Patient not available</h2>
        <p style={{ marginTop: 8 }}>Patient #{patientId} does not exist or is not assigned to you.</p>
        <Link to="/patients" className="link-btn">Back to patients</Link>
      </div>
    )
  }

  return (
    <>
      <div className="page-head">
        <div>
          <Link to="/patients" className="link-btn">← All patients</Link>
          <div className="crumb" style={{ marginTop: 8 }}>
            THERAPY / <b>#{patientId}</b>
          </div>
          <h1>{patient?.profile?.full_name ?? 'Loading…'}</h1>
          {patient && (
            <p className="muted" style={{ marginTop: 4 }}>
              {patient.age ? `${patient.age} yrs • ` : ''}{patient.affected_arm?.toLowerCase() ?? ''} arm • {patient.rehabilitation_goal}
            </p>
          )}
        </div>
        <div className="head-actions">
          <span className="date-chip">{updatedAt ? `Synced ${updatedAt.toLocaleTimeString()}` : 'Syncing…'}</span>
          <button className="btn-chip" onClick={load}>Refresh</button>
        </div>
      </div>
      {error && <p className="error-text" style={{ marginBottom: 12 }}>{error}</p>}
      <div className="table-wrap">
        <table className="table">
          <thead>
            <tr>
              <th className="label">Date</th>
              <th className="label">Exercise</th>
              <th className="label">Reps</th>
              <th className="label">Peak ROM</th>
              <th className="label">Duration</th>
              <th className="label">Outcome</th>
            </tr>
          </thead>
          <tbody>
            {patient && sessions.length === 0 && (
              <tr><td colSpan={6} className="empty">No sessions recorded yet.</td></tr>
            )}
            {sessions.map((s) => (
              <tr
                key={s.id}
                className="clickable"
                onClick={() => navigate(`/patients/${patientId}/sessions/${encodeURIComponent(s.id)}`)}
                aria-label={`${new Date(s.started_at).toLocaleString()} ${painLabel[s.pain_level]}`}
              >
                <td>
                  <b>{new Date(s.started_at).toLocaleDateString(undefined, { day: 'numeric', month: 'short', year: 'numeric' })}</b>
                  <span className="muted"> • {new Date(s.started_at).toLocaleTimeString(undefined, { hour: '2-digit', minute: '2-digit' })}</span>
                  {s.source === 'SEEDED' && <span className="tag">Seeded</span>}
                </td>
                <td className="muted">{exercises[s.exercise_id]?.name ?? s.exercise_id}</td>
                <td>{s.repetitions}</td>
                <td>{Math.round(s.peak_rom_deg)}°</td>
                <td className="muted">{formatDuration(sessionDurationMs(s))}</td>
                <td><PainPill level={s.pain_level} /></td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </>
  )
}
