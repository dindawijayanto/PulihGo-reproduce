import { Link, useNavigate } from 'react-router-dom'
import { Icon } from '../components/Icon'
import {
  formatAgo,
  formatLongDate,
  summarize,
  type ActivityOutcome,
  type DailyReps,
  type FlagKind,
  type MonitoringFlag,
} from '../lib/dashboard'
import { useCohort } from '../lib/useCohort'

/** PGF-THR-02 — Therapist Dashboard (Figma Screen 01 empty / Screen 02 with data). */
export function DashboardPage() {
  const { cohort, exercises, error, updatedAt, loading, reload } = useCohort()
  const navigate = useNavigate()
  const now = new Date()
  const summary = cohort ? summarize(cohort, exercises, now) : null

  return (
    <>
      <div className="page-head">
        <div>
          <div className="crumb">
            DASHBOARD / <b>OVERVIEW</b>
          </div>
          <h1>Overall Statistics</h1>
          <p className="muted" style={{ marginTop: 4 }}>
            Rolling 7-day view of sessions synced from your patients' phones.
          </p>
        </div>
        <div className="head-actions">
          <span className="date-chip">
            <Icon name="calendar" size={14} /> {formatLongDate(now)}
          </span>
          <button className="btn-chip" onClick={reload} disabled={loading}>
            {loading ? 'Refreshing…' : 'Refresh'}
          </button>
        </div>
      </div>

      {error && <p className="error-text" style={{ marginBottom: 12 }}>{error}</p>}
      {!summary && !error && <div className="card empty">Loading dashboard…</div>}

      {summary && summary.totalSessions === 0 && (
        <div className="card empty">
          <h2>No patient data yet</h2>
          <p style={{ margin: '8px 0 4px' }}>
            {summary.assignedPatients === 0
              ? 'No patients are assigned to you yet.'
              : 'No calibrated sessions recorded yet.'}
          </p>
          <p className="faint" style={{ marginBottom: 12 }}>
            Patients record sessions with the motion sensor in the PulihGo mobile app.
          </p>
          <Link to="/patients" className="link-btn">View Patients</Link>
        </div>
      )}

      {summary && summary.totalSessions > 0 && (
        <div className="stack">
          <div className="kpis">
            <div className="kpi">
              <span className="label">Active patients</span>
              <b>
                {summary.patientsWithSessions}
                <span className="kpi-unit"> / {summary.assignedPatients}</span>
              </b>
              <small>Assigned patients with at least one session</small>
            </div>
            <div className="kpi">
              <span className="label">Total exercise dose</span>
              <b>
                {summary.weeklyRepetitions}
                <span className="kpi-unit"> reps</span>
              </b>
              <small>
                {summary.weeklySessions} {summary.weeklySessions === 1 ? 'session' : 'sessions'} • Rolling 7-Day
              </small>
            </div>
            <div className="kpi">
              <span className="label">Average peak ROM</span>
              <b>{summary.averageLatestPeakRomDeg == null ? '—' : `${Math.round(summary.averageLatestPeakRomDeg)}°`}</b>
              <small>
                Latest session per patient
                {summary.targetRomDeg != null && ` • target ${Math.round(summary.targetRomDeg)}°`}
              </small>
            </div>
            <div className="kpi">
              <span className="label">Weekly adherence</span>
              <b className="faint">—</b>
              <small>Needs a prescribed exercise schedule</small>
            </div>
          </div>

          <div className="grid-2">
            <div className="card">
              <div className="card-title">
                <span className="label">Cohort Repetition Timeline</span>
                <span className="faint" style={{ fontSize: 12 }}>Rolling 7-Day</span>
              </div>
              <RepBars week={summary.week} />
              <p className="faint" style={{ fontSize: 12, marginTop: 12 }}>Daily repetitions from saved sessions.</p>
            </div>
            <div className="card">
              <div className="card-title">
                <Icon name="warning" size={16} />
                <span className="label">Monitoring Flags</span>
                {summary.flags.length > 0 && <span className="pill bad">{summary.flags.length}</span>}
              </div>
              {summary.flags.length === 0 ? (
                <p className="muted">No flags in your patients' latest sessions.</p>
              ) : (
                <div className="flag-list">
                  {summary.flags.map((f) => (
                    <FlagRow key={f.patientId} flag={f} now={now} onReview={() => navigate(`/patients/${f.patientId}`)} />
                  ))}
                </div>
              )}
            </div>
          </div>

          <div>
            <div className="row-between" style={{ marginBottom: 12 }}>
              <span className="label">Recent Patient Activity</span>
              <span className="faint" style={{ fontSize: 12 }}>Latest session per patient</span>
            </div>
            <div className="table-wrap">
              <table className="table">
                <thead>
                  <tr>
                    <th className="label">Patient</th>
                    <th className="label">Latest session</th>
                    <th className="label">Reps</th>
                    <th className="label">Peak ROM</th>
                    <th className="label">Status</th>
                    <th className="label" aria-label="Actions" />
                  </tr>
                </thead>
                <tbody>
                  {summary.recentActivity.map((r) => (
                    <tr key={r.patientId}>
                      <td>
                        <b>{r.name}</b>
                        <div className="faint" style={{ fontSize: 12 }}>#{r.patientId}</div>
                      </td>
                      <td>
                        {r.exerciseName}
                        <div className="faint" style={{ fontSize: 12 }}>
                          {formatAgo(r.endedAt, now)}
                          {r.seeded && <span className="tag">Seeded</span>}
                        </div>
                      </td>
                      <td>{r.repetitions}</td>
                      <td>{Math.round(r.peakRomDeg)}°</td>
                      <td><OutcomePill outcome={r.outcome} /></td>
                      <td style={{ textAlign: 'right' }}>
                        <Link
                          className="link-btn"
                          to={`/patients/${r.patientId}/sessions/${encodeURIComponent(r.sessionId)}`}
                        >
                          View Details
                        </Link>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
          {updatedAt && <p className="faint" style={{ fontSize: 12 }}>Last synced {updatedAt.toLocaleTimeString()}</p>}
        </div>
      )}
    </>
  )
}

export function RepBars({ week, target }: { week: DailyReps[]; target?: number | null }) {
  const max = Math.max(1, target ?? 0, ...week.map((d) => d.repetitions))
  return (
    <div className="bars" role="img" aria-label={week.map((d) => `${d.date}: ${d.repetitions} reps`).join(', ')}>
      {week.map((d) => (
        <div key={d.date} className="bar-col" title={`Daily repetitions: ${d.repetitions}`}>
          <span className="bar-value">{d.repetitions > 0 ? d.repetitions : ''}</span>
          <div className="bar-track">
            {target != null && target > 0 && (
              <div className="bar-target" style={{ bottom: `${(target / max) * 100}%` }} />
            )}
            <div
              className={`bar${d.isToday ? ' today' : ''}`}
              style={{ height: `${(d.repetitions / max) * 100}%` }}
            />
          </div>
          <span className={`bar-day${d.isToday ? ' today' : ''}`}>{d.dayLetter}</span>
        </div>
      ))}
    </div>
  )
}

const outcomeText: Record<ActivityOutcome, [string, string]> = {
  COMPLETED: ['ok', 'Completed'],
  MILD_PAIN: ['warn', 'Mild Pain'],
  STOPPED_FOR_PAIN: ['bad', 'Stopped: Pain'],
}

function OutcomePill({ outcome }: { outcome: ActivityOutcome }) {
  const [cls, text] = outcomeText[outcome]
  return <span className={`pill ${cls}`}>{text}</span>
}

const flagText: Record<FlagKind, [string, string]> = {
  PAIN_STOPPED: ['bad', 'Pain Alert'],
  MILD_PAIN: ['warn', 'Mild Pain'],
  CEILING_REACHED: ['warn', 'Ceiling Reached'],
}

function FlagRow({ flag, now, onReview }: { flag: MonitoringFlag; now: Date; onReview: () => void }) {
  const [cls, text] = flagText[flag.kind]
  return (
    <div className={`flag ${cls}`}>
      <div className="row-between">
        <span className={`pill ${cls}`}>{text}</span>
        <span className="faint" style={{ fontSize: 12 }}>{formatAgo(flag.endedAt, now)}</span>
      </div>
      <b>{flag.name}</b>
      <p className="muted">{flag.detail}</p>
      <button className="link-btn" onClick={onReview}>Review Patient →</button>
    </div>
  )
}
