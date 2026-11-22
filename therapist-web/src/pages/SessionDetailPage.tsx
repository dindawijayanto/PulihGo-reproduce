import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { getAssignedPatient, getSession, listExercises } from '../lib/api'
import { useAuth } from '../lib/auth'
import {
  formatDuration,
  outcomeLabel,
  painLabel,
  percent,
  romOutcome,
  sessionDurationMs,
  targetsFor,
  type RomOutcome,
} from '../lib/logic'
import type { ExerciseSession, Patient, PainLevel } from '../lib/types'

type State =
  | { kind: 'loading' }
  | { kind: 'missing' }
  | { kind: 'error' }
  | { kind: 'ready'; patient: Patient; session: ExerciseSession; exercise: ReturnType<typeof targetsFor> }

export function SessionDetailPage() {
  const { patientId = '', sessionId = '' } = useParams()
  const { profile } = useAuth()
  const [state, setState] = useState<State>({ kind: 'loading' })

  useEffect(() => {
    if (!profile) return
    let active = true
    ;(async () => {
      try {
        const patient = await getAssignedPatient(profile.id, patientId)
        const session = patient ? await getSession(patient.id, sessionId) : null
        if (!active) return
        if (!patient || !session) return setState({ kind: 'missing' })
        const exercises = await listExercises()
        if (!active) return
        setState({
          kind: 'ready',
          patient,
          session,
          exercise: targetsFor(session, exercises.find((e) => e.id === session.exercise_id)),
        })
      } catch {
        if (active) setState({ kind: 'error' })
      }
    })()
    return () => {
      active = false
    }
  }, [profile, patientId, sessionId])

  const back = (
    <Link to={`/patients/${patientId}`} className="link-btn">
      ← Back to sessions
    </Link>
  )

  if (state.kind === 'loading') return <div className="card empty">Loading session…</div>
  if (state.kind !== 'ready') {
    return (
      <div className="card empty">
        <h2>{state.kind === 'error' ? 'Could not load the session' : 'Session not available'}</h2>
        <p style={{ margin: '8px 0' }}>
          {state.kind === 'error'
            ? 'Check your connection and try again.'
            : 'This session does not exist or belongs to a patient who is not assigned to you.'}
        </p>
        {back}
      </div>
    )
  }

  const { patient, session: s, exercise: t } = state
  const outcome = romOutcome(s.peak_rom_deg, t.targetRomDeg, t.romCeilingDeg, s.repetitions > 0)
  const smooth = percent(s.average_smoothness)
  const reps = [...s.repetition_details].sort((a, b) => a.repetitionNumber - b.repetitionNumber)
  const started = new Date(s.started_at)

  return (
    <>
      <div className="page-head">
        <div>
          {back}
          <div className="crumb" style={{ marginTop: 8 }}>
            THERAPY / #{patient.id} / <b>SESSION DETAIL</b>
          </div>
          <h1>Session Detail</h1>
          <p className="muted" style={{ marginTop: 4 }}>
            {patient.profile?.full_name ?? patient.id} • {t.exerciseName}
          </p>
        </div>
        <div className="head-actions">
          <span className="date-chip">
            {started.toLocaleDateString('en-US', { weekday: 'short', day: 'numeric', month: 'short', year: 'numeric' })} •{' '}
            {started.toLocaleTimeString(undefined, { hour: '2-digit', minute: '2-digit' })}
          </span>
          <PainPill level={s.pain_level} />
        </div>
      </div>

      <div className="kpis">
        <div className="kpi">
          <span className="label">Repetitions</span>
          <b>{s.repetitions}</b>
          <small>{t.targetReps ? `Target ${t.targetReps} per session` : 'No target set'}</small>
        </div>
        <div className="kpi">
          <span className="label">Peak ROM</span>
          <b>{Math.round(s.peak_rom_deg)}°</b>
          <small>
            {[t.targetRomDeg != null && `target ${Math.round(t.targetRomDeg)}°`, t.romCeilingDeg != null && `ceiling ${Math.round(t.romCeilingDeg)}°`]
              .filter(Boolean)
              .join(' • ') || 'No target set'}
          </small>
          <OutcomePill outcome={outcome} />
        </div>
        <div className="kpi">
          <span className="label">Duration</span>
          <b>{formatDuration(sessionDurationMs(s))}</b>
          <small>Start to finish</small>
        </div>
        <div className="kpi">
          <span className="label">Smoothness</span>
          <b>{smooth == null ? '—' : `${smooth}%`}</b>
          <small>{smooth == null ? 'Not recorded' : 'Average of all reps'}</small>
        </div>
      </div>

      <p className="muted" style={{ fontSize: 12, margin: '16px 0' }}>
        Session ID <code>{s.id}</code> • {s.source === 'SEEDED' ? 'Seeded history' : 'Recorded on the patient’s phone'} • synced{' '}
        {new Date(s.synced_at).toLocaleString()}
      </p>

      <h2 style={{ fontSize: 18, marginBottom: 12 }}>Repetitions ({reps.length})</h2>
      <div className="table-wrap">
        <table className="table">
          <thead>
            <tr>
              <th className="label">#</th>
              <th className="label">Peak ROM</th>
              <th className="label">Duration</th>
              <th className="label">Smoothness</th>
              <th className="label">Result</th>
            </tr>
          </thead>
          <tbody>
            {reps.length === 0 && (
              <tr><td colSpan={5} className="empty">No per-repetition data was recorded for this session.</td></tr>
            )}
            {reps.map((r) => (
              <tr key={r.repetitionNumber}>
                <td><b>{r.repetitionNumber}</b></td>
                <td>{Math.round(r.peakRomDeg)}°</td>
                <td className="muted">{formatDuration(r.durationMs)}</td>
                <td className="muted">{r.smoothnessScore == null ? '—' : `${percent(r.smoothnessScore)}%`}</td>
                <td><OutcomePill outcome={romOutcome(r.peakRomDeg, t.targetRomDeg, t.romCeilingDeg, true)} /></td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </>
  )
}

export function PainPill({ level }: { level: PainLevel }) {
  const cls = level === 'NO_PAIN' ? 'ok' : level === 'MILD_PAIN' ? 'warn' : 'bad'
  return <span className={`pill ${cls}`}>{painLabel[level]}</span>
}

function OutcomePill({ outcome }: { outcome: RomOutcome }) {
  if (outcome === 'UNKNOWN') return null
  const cls = outcome === 'TARGET_HIT' ? 'ok' : outcome === 'CEILING_EXCEEDED' ? 'bad' : 'idle'
  return <span className={`pill ${cls}`} style={{ alignSelf: 'flex-start' }}>{outcomeLabel[outcome]}</span>
}
