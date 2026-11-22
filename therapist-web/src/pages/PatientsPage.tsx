import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { listAssignedPatients, listSessionCounts } from '../lib/api'
import { useAuth } from '../lib/auth'
import type { Patient } from '../lib/types'

type Counts = Record<string, { count: number; latest: string | null }>

export function PatientsPage() {
  const { profile } = useAuth()
  const navigate = useNavigate()
  const [patients, setPatients] = useState<Patient[] | null>(null)
  const [counts, setCounts] = useState<Counts>({})
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!profile) return
    let active = true
    listAssignedPatients(profile.id)
      .then(async (list) => {
        const c = await listSessionCounts(list.map((p) => p.id))
        if (!active) return
        setPatients(list)
        setCounts(c)
      })
      .catch(() => active && setError('Could not load patients from the cloud.'))
    return () => {
      active = false
    }
  }, [profile])

  return (
    <>
      <div className="page-head">
        <div>
          <div className="crumb">
            THERAPY / <b>PATIENT SESSIONS</b>
          </div>
          <h1>Patient Sessions</h1>
          <p className="muted" style={{ marginTop: 4 }}>Exercise sessions synced from each patient's phone.</p>
        </div>
      </div>
      {error && <p className="error-text">{error}</p>}
      <div className="table-wrap">
        <table className="table">
          <thead>
            <tr>
              <th className="label">Patient</th>
              <th className="label">ID</th>
              <th className="label">Arm</th>
              <th className="label">Sessions</th>
              <th className="label">Last session</th>
            </tr>
          </thead>
          <tbody>
            {patients == null && !error && (
              <tr><td colSpan={5} className="empty">Loading…</td></tr>
            )}
            {patients?.length === 0 && (
              <tr><td colSpan={5} className="empty">No patients are assigned to you yet.</td></tr>
            )}
            {patients?.map((p) => (
              <tr key={p.id} className="clickable" onClick={() => navigate(`/patients/${p.id}`)}>
                <td><b>{p.profile?.full_name ?? p.id}</b></td>
                <td className="muted">#{p.id}</td>
                <td className="muted">{p.affected_arm ? p.affected_arm.toLowerCase() : '—'}</td>
                <td>{counts[p.id]?.count ?? 0}</td>
                <td className="muted">{counts[p.id]?.latest ? new Date(counts[p.id].latest!).toLocaleString() : 'No sessions'}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </>
  )
}
