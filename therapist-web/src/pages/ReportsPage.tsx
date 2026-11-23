import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { formatDuration, percent } from '../lib/logic'
import { buildReport, reportRanges, type ReportRange, type ReportSessionStatus, type RomTrendPoint } from '../lib/reports'
import { useCohort } from '../lib/useCohort'
import { RepBars } from './DashboardPage'

/** PGF-THR-06 — Therapist Reports (Figma Screen 06 empty / with data). */
export function ReportsPage() {
  const { cohort, exercises, error, loading, reload } = useCohort()
  const [params, setParams] = useSearchParams()
  const navigate = useNavigate()
  const patientId = params.get('patient') ?? ''
  const rangeParam = params.get('range') as ReportRange | null
  const range: ReportRange = reportRanges.some((r) => r.value === rangeParam) ? rangeParam! : 'LAST_7_DAYS'
  const selected = cohort?.find((p) => p.patient.id === patientId)
  const report = selected ? buildReport(selected, range, exercises, new Date()) : null

  const update = (next: { patient?: string; range?: ReportRange }) => {
    const p = new URLSearchParams(params)
    if (next.patient !== undefined) {
      if (next.patient) p.set('patient', next.patient)
      else p.delete('patient')
    }
    if (next.range) p.set('range', next.range)
    setParams(p, { replace: true })
  }

  return (
    <>
      <div className="page-head">
        <div>
          <div className="crumb">
            REPORTS / <b>WEEKLY REPORTS</b>
          </div>
          <h1>Weekly Reports</h1>
          <p className="muted" style={{ marginTop: 4 }}>Per-patient summary of sessions synced from the PulihGo mobile app.</p>
        </div>
        <div className="head-actions">
          <button className="btn-chip" onClick={reload} disabled={loading}>
            {loading ? 'Refreshing…' : 'Refresh'}
          </button>
        </div>
      </div>

      {error && <p className="error-text" style={{ marginBottom: 12 }}>{error}</p>}
      {!cohort && !error && <div className="card empty">Loading patients…</div>}

      {cohort?.length === 0 && (
        <div className="card empty">
          <h2>No patients assigned</h2>
          <p style={{ margin: '8px 0' }}>Reports appear once a patient is assigned to you.</p>
          <Link to="/patients" className="link-btn">Go to Patient Directory</Link>
        </div>
      )}

      {cohort && cohort.length > 0 && (
        <div className="stack">
          <div className="card report-controls">
            <label className="field" style={{ marginTop: 0 }}>
              <span className="label">Selected patient</span>
              <select className="select" value={selected ? patientId : ''} onChange={(e) => update({ patient: e.target.value })}>
                <option value="">Select an assigned patient…</option>
                {cohort.map((p) => (
                  <option key={p.patient.id} value={p.patient.id}>
                    {p.name} (#{p.patient.id})
                  </option>
                ))}
              </select>
            </label>
            <label className="field" style={{ marginTop: 0 }}>
              <span className="label">Range</span>
              <select className="select" value={range} onChange={(e) => update({ range: e.target.value as ReportRange })}>
                {reportRanges.map((r) => (
                  <option key={r.value} value={r.value}>{r.label}</option>
                ))}
              </select>
            </label>
          </div>

          {!selected && (
            <div className="card empty">
              <h2>Select a patient to view reports</h2>
              <p style={{ margin: '8px 0 14px' }}>
                {patientId ? `Patient #${patientId} is not assigned to you.` : 'Choose one of your assigned patients.'}
              </p>
              <div className="head-actions" style={{ justifyContent: 'center' }}>
                {cohort.map((p) => (
                  <button key={p.patient.id} className="btn-chip" onClick={() => update({ patient: p.patient.id })}>
                    Load {p.name}
                  </button>
                ))}
              </div>
            </div>
          )}

          {report && selected!.sessions.length === 0 && (
            <div className="card empty">
              <h2>{report.name}</h2>
              <p style={{ marginTop: 8 }}>This patient has not recorded any sessions yet.</p>
            </div>
          )}

          {report && selected!.sessions.length > 0 && (
            <>
              <div className="identity">
                <div className="tile">{report.name.slice(0, 1).toUpperCase()}</div>
                <div style={{ flex: 1 }}>
                  <h2>{report.name}</h2>
                  <span className="id-chip">#{report.patientId}</span>
                  {report.goal && <p className="muted" style={{ marginTop: 6 }}>{report.goal}</p>}
                </div>
                <span className="date-chip">{report.rangeLabel}</span>
              </div>

              <div className="kpis">
                <div className="kpi">
                  <span className="label">Sessions</span>
                  <b>{report.kpis.sessions}</b>
                  <small>
                    {report.kpis.painReportedSessions === 0
                      ? 'No pain reported'
                      : `${report.kpis.painReportedSessions} with pain reported`}
                  </small>
                </div>
                <div className="kpi">
                  <span className="label">Total reps</span>
                  <b>{report.kpis.totalRepetitions}</b>
                  <small>Completed repetitions</small>
                </div>
                <div className="kpi">
                  <span className="label">Best peak ROM</span>
                  <b>{report.kpis.bestPeakRomDeg == null ? '—' : `${Math.round(report.kpis.bestPeakRomDeg)}°`}</b>
                  <small>
                    {report.kpis.averagePeakRomDeg == null
                      ? 'No repetitions in range'
                      : `Average ${Math.round(report.kpis.averagePeakRomDeg)}°`}
                  </small>
                </div>
                <div className="kpi">
                  <span className="label">Avg smoothness</span>
                  <b>{report.kpis.averageSmoothness == null ? '—' : `${percent(report.kpis.averageSmoothness)}%`}</b>
                  <small>{report.kpis.averageSmoothness == null ? 'Not measured in range' : 'Sessions that measured it'}</small>
                </div>
              </div>

              <div className="grid-2">
                <div className="card">
                  <div className="card-title">
                    <span className="label">ROM Recovery Trend</span>
                  </div>
                  {report.romTrend.length === 0 ? (
                    <p className="muted empty">No sessions with repetitions in this range.</p>
                  ) : (
                    <RomChart points={report.romTrend} target={report.targetRomDeg} ceiling={report.romCeilingDeg} />
                  )}
                  <p className="faint" style={{ fontSize: 12, marginTop: 12 }}>Peak ROM across sessions in the selected range.</p>
                </div>
                <div className="card">
                  <div className="card-title">
                    <span className="label">Weekly Rep Distribution</span>
                    {report.targetRepsPerSession != null && (
                      <span className="faint" style={{ fontSize: 12 }}>Target {report.targetRepsPerSession} reps / session</span>
                    )}
                  </div>
                  <RepBars week={report.week} target={report.targetRepsPerSession} />
                  <p className="faint" style={{ fontSize: 12, marginTop: 12 }}>
                    Rolling 7-day. Adherence score needs a prescribed schedule and is not calculated yet.
                  </p>
                </div>
              </div>

              <div>
                <div className="row-between" style={{ marginBottom: 12 }}>
                  <span className="label">Session History</span>
                  <span className="faint" style={{ fontSize: 12 }}>Repetitions, peak angle and duration of each session</span>
                </div>
                <div className="table-wrap">
                  <table className="table">
                    <thead>
                      <tr>
                        <th className="label">Date</th>
                        <th className="label">Exercise</th>
                        <th className="label">Reps</th>
                        <th className="label">Peak ROM</th>
                        <th className="label">Duration</th>
                        <th className="label">Status</th>
                      </tr>
                    </thead>
                    <tbody>
                      {report.rows.length === 0 && (
                        <tr><td colSpan={6} className="empty">No sessions in this range.</td></tr>
                      )}
                      {report.rows.map((r) => {
                        const started = new Date(r.startedAt)
                        return (
                          <tr
                            key={r.id}
                            className="clickable"
                            onClick={() => navigate(`/patients/${report.patientId}/sessions/${encodeURIComponent(r.id)}`)}
                          >
                            <td>
                              <b>{started.toLocaleDateString('en-GB', { day: 'numeric', month: 'short', year: 'numeric' })}</b>
                              <span className="muted"> • {started.toLocaleTimeString('en-GB', { hour: '2-digit', minute: '2-digit' })}</span>
                              {r.seeded && <span className="tag">Seeded</span>}
                            </td>
                            <td className="muted">{r.exerciseName}</td>
                            <td>{r.repetitions}</td>
                            <td>{Math.round(r.peakRomDeg)}°</td>
                            <td className="muted">{formatDuration(r.durationMs)}</td>
                            <td><StatusPill status={r.status} /></td>
                          </tr>
                        )
                      })}
                    </tbody>
                  </table>
                </div>
                {report.sessionsOutsideRange > 0 && (
                  <p className="faint" style={{ fontSize: 12, marginTop: 10 }}>
                    {report.sessionsOutsideRange} older {report.sessionsOutsideRange === 1 ? 'session is' : 'sessions are'} outside
                    this range.{' '}
                    {range !== 'ALL_TIME' && (
                      <button className="link-btn" onClick={() => update({ range: 'ALL_TIME' })}>Show All Time</button>
                    )}
                  </p>
                )}
              </div>
            </>
          )}
        </div>
      )}
    </>
  )
}

const statusText: Record<ReportSessionStatus, [string, string]> = {
  COMPLETED: ['ok', 'Completed'],
  MILD_PAIN: ['warn', 'Mild Pain'],
  STOPPED_FOR_PAIN: ['bad', 'Stopped: Pain'],
  NO_REPETITIONS: ['idle', 'No Reps'],
}

function StatusPill({ status }: { status: ReportSessionStatus }) {
  const [cls, text] = statusText[status]
  return <span className={`pill ${cls}`}>{text}</span>
}

function RomChart({ points, target, ceiling }: { points: RomTrendPoint[]; target: number | null; ceiling: number | null }) {
  const W = 400
  const H = 180
  const pad = { l: 34, r: 12, t: 12, b: 26 }
  const max = Math.ceil((Math.max(...points.map((p) => p.peakRomDeg), target ?? 0, ceiling ?? 0) + 10) / 10) * 10
  const x = (i: number) => (points.length === 1 ? (pad.l + W - pad.r) / 2 : pad.l + (i * (W - pad.l - pad.r)) / (points.length - 1))
  const y = (v: number) => pad.t + (1 - v / max) * (H - pad.t - pad.b)
  const ticks = [0, max / 2, max]
  const labelEvery = Math.ceil(points.length / 6)
  return (
    <svg
      viewBox={`0 0 ${W} ${H}`}
      className="rom-chart"
      role="img"
      aria-label={`Peak ROM trend: ${points.map((p) => `${p.label} ${Math.round(p.peakRomDeg)}°`).join(', ')}`}
    >
      {ticks.map((t) => (
        <g key={t}>
          <line x1={pad.l} x2={W - pad.r} y1={y(t)} y2={y(t)} className="grid" />
          <text x={pad.l - 6} y={y(t) + 4} textAnchor="end">{Math.round(t)}°</text>
        </g>
      ))}
      {ceiling != null && <line x1={pad.l} x2={W - pad.r} y1={y(ceiling)} y2={y(ceiling)} className="ceiling" />}
      {target != null && <line x1={pad.l} x2={W - pad.r} y1={y(target)} y2={y(target)} className="target" />}
      <polyline points={points.map((p, i) => `${x(i)},${y(p.peakRomDeg)}`).join(' ')} className="line" />
      {points.map((p, i) => (
        <g key={i}>
          <circle cx={x(i)} cy={y(p.peakRomDeg)} r={4} className="dot">
            <title>{`${p.label}: ${Math.round(p.peakRomDeg)}°`}</title>
          </circle>
          {i % labelEvery === 0 && (
            <text
              x={x(i)}
              y={H - 6}
              textAnchor={points.length > 1 && i === 0 ? 'start' : points.length > 1 && i === points.length - 1 ? 'end' : 'middle'}
            >
              {p.label}
            </text>
          )}
        </g>
      ))}
      {target != null && <text x={W - pad.r} y={y(target) - 4} textAnchor="end" className="target-label">Target {Math.round(target)}°</text>}
      {ceiling != null && <text x={W - pad.r} y={y(ceiling) - 4} textAnchor="end" className="ceiling-label">Ceiling {Math.round(ceiling)}°</text>}
    </svg>
  )
}
