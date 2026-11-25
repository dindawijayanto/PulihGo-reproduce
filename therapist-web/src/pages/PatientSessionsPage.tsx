import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { getAssignedPatient, listExercises, listSessions } from '../lib/api'
import { useAuth } from '../lib/auth'
import { initials, formatDuration, painLabel, percent, sessionDurationMs } from '../lib/logic'
import { comfortDescription, comfortTone, summarizePatientDetail } from '../lib/patientDetail'
import type { Exercise, ExerciseSession, Patient } from '../lib/types'
import { PainPill } from './SessionDetailPage'
import './patientDetail.css'

const REFRESH_MS = 15_000

type LoadState =
  | { kind: 'loading'; scope: string }
  | { kind: 'missing'; scope: string }
  | { kind: 'error'; scope: string; message: string }
  | { kind: 'ready'; scope: string; patient: Patient; sessions: ExerciseSession[]; exercises: Record<string, Exercise>; updatedAt: Date; refreshError: string | null }

/** PGF-THR-04: Patient Detail & Comfort Feedback; existing /patients/:patientId route. */
export function PatientSessionsPage() {
  const { patientId = '' } = useParams()
  const { profile } = useAuth()
  const therapistId = profile?.id ?? ''
  const autoSync = profile?.auto_sync ?? false
  const scope = `${therapistId}:${patientId}`
  const [state, setState] = useState<LoadState>({ kind: 'loading', scope: '' })
  const [refreshNonce, setRefreshNonce] = useState(0)

  useEffect(() => {
    let active = true
    if (!therapistId || !patientId) {
      setState({ kind: 'missing', scope })
      return () => { active = false }
    }

    // Do not keep the previous patient's profile or metrics visible during navigation.
    setState({ kind: 'loading', scope })
    const load = async () => {
      try {
        // Verify ownership before requesting any of the patient's sessions.
        const owned = await getAssignedPatient(therapistId, patientId)
        if (!active) return
        if (!owned) {
          setState({ kind: 'missing', scope })
          return
        }
        const [records, exerciseList] = await Promise.all([listSessions(owned.id), listExercises()])
        if (!active) return
        setState({
          kind: 'ready', scope, patient: owned, sessions: records,
          exercises: Object.fromEntries(exerciseList.map((exercise) => [exercise.id, exercise])),
          updatedAt: new Date(), refreshError: null,
        })
      } catch {
        if (!active) return
        setState((previous) => previous.kind === 'ready' && previous.scope === scope
          ? { ...previous, refreshError: 'Could not refresh cloud data. Previously loaded results are shown.' }
          : { kind: 'error', scope, message: 'Could not load patient details from the cloud.' })
      }
    }
    void load()
    const timer = autoSync ? window.setInterval(() => { void load() }, REFRESH_MS) : null
    return () => {
      active = false
      if (timer != null) window.clearInterval(timer)
    }
  }, [therapistId, autoSync, patientId, scope, refreshNonce])

  const reload = () => setRefreshNonce((value) => value + 1)
  const back = <Link className="link-btn" to="/patients">← All patients</Link>
  const shown = state.scope === scope ? state : { kind: 'loading' as const, scope }

  if (shown.kind === 'loading') return <div className="card empty" role="status">Loading patient details…</div>
  if (shown.kind !== 'ready') {
    return (
      <div className="card empty" role="alert">
        <h2>{shown.kind === 'missing' ? 'Patient not available' : 'Could not load patient'}</h2>
        <p style={{ margin: '10px auto 16px', maxWidth: 520 }}>
          {shown.kind === 'missing'
            ? 'This patient does not exist or is not assigned to your therapist account.'
            : shown.message}
        </p>
        <div className="pd-empty-actions">
          {back}
          {shown.kind === 'error' && <button className="btn-chip" onClick={reload}>Retry</button>}
        </div>
      </div>
    )
  }

  const { patient, sessions, exercises, updatedAt, refreshError } = shown
  const summary = summarizePatientDetail(patient.id, sessions)
  const latest = summary.latestSession
  const name = patient.profile?.full_name || patient.id
  const latestLevel = latest?.pain_level ?? null
  const arm = patient.affected_arm?.toLowerCase()
  const best = summary.bestPeakRomDeg == null ? '—' : `${Math.round(summary.bestPeakRomDeg)}°`
  const smooth = percent(summary.averageSmoothness)

  return (
    <>
      <div className="page-head">
        <div>
          {back}
          <div className="crumb" style={{ marginTop: 8 }}>THERAPY / PATIENTS / <b>DETAIL</b></div>
          <h1>Patient Detail</h1>
          <p className="muted" style={{ marginTop: 4 }}>Clinical overview and patient-reported comfort feedback.</p>
        </div>
        <div className="head-actions">
          <span className="date-chip">Synced {updatedAt.toLocaleTimeString()}</span>
          <Link className="btn-chip" to={`/patients/${encodeURIComponent(patient.id)}/therapy-plan`}>Therapy Plan</Link>
          <button className="btn-chip" onClick={reload}>Refresh</button>
        </div>
      </div>

      {refreshError && <p className="error-text" role="status" style={{ marginBottom: 16 }}>{refreshError}</p>}

      <div className="pd-overview">
        <section className="card pd-profile" aria-labelledby="pd-profile-title">
          <div className="card-title"><span className="label" id="pd-profile-title">Patient profile</span></div>
          <div className="pd-identity">
            <div className="pd-avatar" aria-hidden="true">{initials(name)}</div>
            <div>
              <h2>{name}</h2>
              <div className="muted">Patient #{patient.id}</div>
              <span className={`pill ${latest ? comfortTone(latestLevel) : 'idle'}`} style={{ marginTop: 10 }}>
                {latest ? (latestLevel === 'NO_PAIN' ? 'Active' : 'Needs follow-up') : 'No sessions yet'}
              </span>
            </div>
          </div>
          <dl className="pd-facts">
            <div><dt>Age</dt><dd>{patient.age == null ? 'Not specified' : `${patient.age} years`}</dd></div>
            <div><dt>Affected arm</dt><dd>{arm ? `${arm.charAt(0).toUpperCase()}${arm.slice(1)}` : 'Not specified'}</dd></div>
            <div className="pd-fact-full"><dt>Rehabilitation goal</dt><dd>{patient.rehabilitation_goal || 'Not specified'}</dd></div>
            {patient.notes && <div className="pd-fact-full"><dt>Patient notes</dt><dd>{patient.notes}</dd></div>}
          </dl>
        </section>

        <section className="card pd-comfort" aria-labelledby="pd-comfort-title">
          <div className="card-title"><span className="label" id="pd-comfort-title">Comfort feedback</span></div>
          <div className="pd-comfort-head">
            <span className={`pill ${comfortTone(latestLevel)}`}>
              {latestLevel ? painLabel[latestLevel] : 'No feedback yet'}
            </span>
            <span className="muted" style={{ fontSize: 12 }}>
              {latest ? `Latest session • ${new Date(latest.ended_at).toLocaleDateString()}` : 'No sessions'}
            </span>
          </div>
          <p className="pd-comfort-message">{comfortDescription(latestLevel)}</p>
          <div className="pd-comfort-counts" aria-label="Reported comfort by session">
            <div><b>{summary.noPainSessions}</b><span>No pain</span></div>
            <div><b>{summary.mildPainSessions}</b><span>Mild pain</span></div>
            <div><b>{summary.stoppedForPainSessions}</b><span>Stopped due to pain</span></div>
          </div>
          <p className="faint pd-footnote">These labels come from patient feedback, not an automatic diagnosis. Historical/demo sessions are marked in the activity table.</p>
        </section>
      </div>

      <section aria-labelledby="pd-summary-title" className="pd-section">
        <div className="pd-section-heading">
          <h2 id="pd-summary-title">Training Summary</h2>
          <span className="muted" style={{ fontSize: 12 }}>All synced sessions for this patient</span>
        </div>
        <div className="kpis pd-kpis">
          <div className="kpi"><span className="label">Total sessions</span><b>{summary.totalSessions}</b><small>Recorded activity</small></div>
          <div className="kpi"><span className="label">Total repetitions</span><b>{summary.totalRepetitions}</b><small>Across all sessions</small></div>
          <div className="kpi"><span className="label">Best peak ROM</span><b>{best}</b><small>{summary.bestPeakRomDeg == null ? 'Not measured' : 'Highest recorded angle'}</small></div>
          <div className="kpi"><span className="label">Avg smoothness</span><b>{smooth == null ? '—' : `${smooth}%`}</b><small>{smooth == null ? 'Not recorded' : 'Sessions with a score'}</small></div>
        </div>
      </section>

      <section aria-labelledby="pd-history-title" className="pd-section">
        <div className="pd-section-heading">
          <h2 id="pd-history-title">Activity History</h2>
          <span className="date-chip">{summary.totalSessions} {summary.totalSessions === 1 ? 'session' : 'sessions'}</span>
        </div>
        <div className="table-wrap">
          <table className="table pd-history">
            <thead>
              <tr>
                <th className="label">Date &amp; time</th>
                <th className="label">Exercise</th>
                <th className="label">Reps</th>
                <th className="label">Peak ROM</th>
                <th className="label">Duration</th>
                <th className="label">Smoothness</th>
                <th className="label">Comfort</th>
                <th className="label">Details</th>
              </tr>
            </thead>
            <tbody>
              {summary.sessions.length === 0 && (
                <tr><td colSpan={8} className="empty">No activity recorded yet. Sessions saved and synced by the patient will appear here.</td></tr>
              )}
              {summary.sessions.map((session) => {
                const started = new Date(session.started_at)
                const sessionSmooth = percent(session.average_smoothness)
                const path = `/patients/${encodeURIComponent(patient.id)}/sessions/${encodeURIComponent(session.id)}`
                return (
                  <tr key={session.id}>
                    <td>
                      <b>{started.toLocaleDateString(undefined, { day: 'numeric', month: 'short', year: 'numeric' })}</b>
                      <div className="muted">{started.toLocaleTimeString(undefined, { hour: '2-digit', minute: '2-digit' })}</div>
                      {session.source === 'SEEDED' && <span className="tag pd-seed-tag">Demo history</span>}
                    </td>
                    <td>{exercises[session.exercise_id]?.name ?? session.exercise_id}</td>
                    <td>{session.repetitions}</td>
                    <td>{Math.round(session.peak_rom_deg)}°</td>
                    <td className="muted">{formatDuration(sessionDurationMs(session))}</td>
                    <td>{sessionSmooth == null ? '—' : `${sessionSmooth}%`}</td>
                    <td><PainPill level={session.pain_level} /></td>
                    <td><Link className="link-btn pd-session-link" to={path} aria-label={`View session ${session.id}`}>View session</Link></td>
                  </tr>
                )
              })}
            </tbody>
          </table>
        </div>
      </section>
    </>
  )
}
