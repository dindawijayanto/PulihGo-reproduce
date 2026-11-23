import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { initials } from '../lib/logic'
import {
  applyListControls,
  buildItems,
  filterOptions,
  formatSessionTime,
  overview,
  page,
  PAGE_SIZE,
  sortOptions,
  type PatientListFilter,
  type PatientListItem,
  type PatientListSort,
  type PatientStatus,
} from '../lib/patientList'
import { useCohort } from '../lib/useCohort'

/** PGF-THR-03 — Therapist Patient List & Search (Figma Screen 03), on top of the INT-01 patient list. */
export function PatientsPage() {
  const { cohort, error, loading, reload, updatedAt } = useCohort()
  const [params, setParams] = useSearchParams()
  const navigate = useNavigate()

  const query = params.get('q') ?? ''
  const filter = (filterOptions.some((f) => f.value === params.get('filter')) ? params.get('filter') : 'ALL') as PatientListFilter
  const sort = (sortOptions.some((s) => s.value === params.get('sort')) ? params.get('sort') : 'MOST_RECENT_SESSION') as PatientListSort
  const requestedPage = Number(params.get('page') ?? '1') - 1

  const items = cohort ? buildItems(cohort) : null
  const stats = items ? overview(items) : null
  const matches = items ? applyListControls(items, query, filter, sort) : []
  const current = page(matches, requestedPage)
  const narrowed = query.trim() !== '' || filter !== 'ALL'
  const now = new Date()

  const update = (next: Record<string, string | null>, resetPage = true) => {
    const p = new URLSearchParams(params)
    for (const [k, v] of Object.entries(next)) {
      if (v == null || v === '') p.delete(k)
      else p.set(k, v)
    }
    if (resetPage) p.delete('page')
    setParams(p, { replace: true })
  }

  return (
    <>
      <div className="page-head">
        <div>
          <div className="crumb">
            THERAPY / <b>PATIENTS</b>
          </div>
          <h1>My Patients</h1>
          <p className="muted" style={{ marginTop: 4 }}>
            Manage and monitor rehabilitation sessions recorded by your assigned patients.
          </p>
        </div>
        <div className="head-actions">
          {stats && (
            <span className="date-chip">
              {stats.totalPatients} patients • {stats.activePatients} active • {stats.needsFollowUp} need follow-up
            </span>
          )}
          <button className="btn-chip" onClick={reload} disabled={loading}>
            {loading ? 'Refreshing…' : 'Refresh'}
          </button>
        </div>
      </div>

      {error && <p className="error-text" style={{ marginBottom: 12 }}>{error}</p>}

      {items && items.length > 0 && (
        <div className="list-controls">
          <label className="input-wrap search">
            <span className="sr-only">Search patients by name or ID</span>
            <input
              type="search"
              value={query}
              placeholder="Search patients by name or ID (e.g. Alya, P001)…"
              onChange={(e) => update({ q: e.target.value })}
            />
            {query && (
              <button className="icon-btn" aria-label="Clear search" onClick={() => update({ q: null })}>
                Clear
              </button>
            )}
          </label>
          <div className="segmented" role="group" aria-label="Filter patients">
            {filterOptions.map((f) => (
              <button
                key={f.value}
                className={filter === f.value ? 'active' : ''}
                aria-pressed={filter === f.value}
                onClick={() => update({ filter: f.value === 'ALL' ? null : f.value })}
              >
                {f.label}
              </button>
            ))}
          </div>
          <label className="sort">
            <span className="label">Sort</span>
            <select
              className="select"
              value={sort}
              onChange={(e) => update({ sort: e.target.value === 'MOST_RECENT_SESSION' ? null : e.target.value })}
            >
              {sortOptions.map((s) => (
                <option key={s.value} value={s.value}>{s.label}</option>
              ))}
            </select>
          </label>
        </div>
      )}

      <div className="table-wrap">
        <table className="table">
          <thead>
            <tr>
              <th className="label">Patient</th>
              <th className="label">Last session telemetry</th>
              <th className="label">Sessions</th>
              <th className="label">Status</th>
              <th className="label" aria-label="Actions" />
            </tr>
          </thead>
          <tbody>
            {items == null && !error && (
              <tr><td colSpan={5} className="empty">Loading…</td></tr>
            )}
            {items?.length === 0 && (
              <tr>
                <td colSpan={5} className="empty">
                  <b>No patients assigned yet</b>
                  <div style={{ marginTop: 6 }}>Patients assigned to you will appear here with their latest session results.</div>
                </td>
              </tr>
            )}
            {items && items.length > 0 && matches.length === 0 && (
              <tr>
                <td colSpan={5} className="empty">
                  <b>{query.trim() ? `No patients match "${query.trim()}"` : 'No patients in this view'}</b>
                  <div style={{ margin: '6px 0 10px' }}>Try another name or ID, or clear the search and filters.</div>
                  <button className="link-btn" onClick={() => update({ q: null, filter: null })}>Clear search &amp; filters</button>
                </td>
              </tr>
            )}
            {current.items.map((p) => (
              <PatientRow key={p.patientId} item={p} now={now} onOpen={() => navigate(`/patients/${p.patientId}`)} />
            ))}
          </tbody>
        </table>
      </div>

      {matches.length > 0 && (
        <div className="row-between pager">
          <span className="faint">
            Showing {current.pageIndex * PAGE_SIZE + 1}–{current.pageIndex * PAGE_SIZE + current.items.length} of {current.totalMatches}
            {narrowed && items ? ` (filtered from ${items.length})` : ''}
            {stats && ` • ${stats.totalRepetitions} total repetitions logged`}
          </span>
          {current.pageCount > 1 && (
            <div className="head-actions">
              <button
                className="btn-chip"
                aria-label="Previous page"
                disabled={current.pageIndex === 0}
                onClick={() => update({ page: String(current.pageIndex) }, false)}
              >
                ←
              </button>
              <span className="faint">{current.pageIndex + 1} / {current.pageCount}</span>
              <button
                className="btn-chip"
                aria-label="Next page"
                disabled={current.pageIndex >= current.pageCount - 1}
                onClick={() => update({ page: String(current.pageIndex + 2) }, false)}
              >
                →
              </button>
            </div>
          )}
        </div>
      )}
      {updatedAt && <p className="faint" style={{ fontSize: 12, marginTop: 8 }}>Last synced {updatedAt.toLocaleTimeString()}</p>}
    </>
  )
}

const statusPill: Record<PatientStatus, [string, string]> = {
  ON_TRACK: ['ok', 'Active'],
  NEEDS_FOLLOW_UP: ['bad', 'Needs Follow-up'],
  NO_SESSIONS: ['idle', 'No Sessions'],
}

function PatientRow({ item: p, now, onOpen }: { item: PatientListItem; now: Date; onOpen: () => void }) {
  const [cls, label] = statusPill[p.status]
  const s = p.lastSession
  return (
    <tr className={`clickable${p.status === 'NEEDS_FOLLOW_UP' ? ' follow-up' : ''}`} onClick={onOpen}>
      <td>
        <div className="patient-cell">
          <span className="mini-avatar">{initials(p.name)}</span>
          <div>
            <b>{p.name}</b>
            <div className="faint" style={{ fontSize: 12 }}>
              #{p.patientId}
              {p.affectedArm && ` • ${p.affectedArm.toLowerCase()} arm`}
              {p.age != null && ` • ${p.age} yrs`}
            </div>
          </div>
        </div>
      </td>
      <td>
        {s ? (
          <>
            <b>{formatSessionTime(s.endedAt, now)}</b>
            <div className="muted" style={{ fontSize: 12 }}>
              {s.repetitions} reps • peak {Math.round(s.peakRomDeg)}°
            </div>
            {s.painLevel !== 'NO_PAIN' && (
              <div className="error-text" style={{ fontSize: 12 }}>
                {s.painLevel === 'MILD_PAIN' ? 'Mild pain reported' : 'Stopped early: Pain reported'}
              </div>
            )}
          </>
        ) : (
          <span className="faint">No sessions recorded yet</span>
        )}
      </td>
      <td>{p.sessionCount}</td>
      <td><span className={`pill ${cls}`}>{label}</span></td>
      <td style={{ textAlign: 'right' }}>
        <Link to={`/patients/${p.patientId}`} className="link-btn" onClick={(e) => e.stopPropagation()}>
          View Patient
        </Link>
      </td>
    </tr>
  )
}
