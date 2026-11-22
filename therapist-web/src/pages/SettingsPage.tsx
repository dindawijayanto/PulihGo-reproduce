import { useEffect, useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { Field, PasswordField } from '../components/Field'
import { Icon } from '../components/Icon'
import { listAssignedPatients, updatePreferences, updateProfile } from '../lib/api'
import { useAuth } from '../lib/auth'
import { initials, normalizeProfile, patientLoadLabel, profileChanged, validateNewPassword, validateProfile } from '../lib/logic'
import { supabase } from '../lib/supabase'
import { useTheme } from '../lib/theme'
import type { Language, Profile, ProfileForm } from '../lib/types'

function formOf(profile: Profile): ProfileForm {
  return {
    fullName: profile.full_name,
    email: profile.email,
    specialization: profile.specialization ?? '',
    affiliation: profile.affiliation ?? '',
  }
}

const todayLabel = new Date().toLocaleDateString('en-US', { weekday: 'long', month: 'long', day: 'numeric', year: 'numeric' })

export function SettingsPage() {
  const { profile, setProfile, signOut, session } = useAuth()
  const { theme, setTheme } = useTheme()
  const navigate = useNavigate()
  const [form, setForm] = useState<ProfileForm>(() => formOf(profile!))
  const [patientCount, setPatientCount] = useState<number | null>(null)
  const [message, setMessage] = useState<{ kind: 'ok' | 'error'; text: string } | null>(null)
  const [prefError, setPrefError] = useState<string | null>(null)
  const [saving, setSaving] = useState(false)
  const [showPassword, setShowPassword] = useState(false)
  const [passwordUpdated, setPasswordUpdated] = useState(false)
  const [confirmLogout, setConfirmLogout] = useState(false)

  useEffect(() => {
    if (!profile) return
    listAssignedPatients(profile.id)
      .then((p) => setPatientCount(p.length))
      .catch(() => setPatientCount(null))
  }, [profile])

  if (!profile) return null
  const saved = formOf(profile)
  const dirty = profileChanged(saved, form)

  function edit(patch: Partial<ProfileForm>) {
    setForm((f) => ({ ...f, ...patch }))
    setMessage(null)
  }

  async function save(e?: FormEvent) {
    e?.preventDefault()
    if (!dirty || !profile) return
    const error = validateProfile(form)
    if (error) return setMessage({ kind: 'error', text: error })
    setSaving(true)
    try {
      const updated = await updateProfile(profile.id, normalizeProfile(form))
      setProfile(updated)
      setForm(formOf(updated))
      setMessage({ kind: 'ok', text: 'Profile saved to the cloud.' })
    } catch {
      setMessage({ kind: 'error', text: 'Could not save the profile. Check your connection and try again.' })
    } finally {
      setSaving(false)
    }
  }

  async function changePref(patch: { audio_alerts?: boolean; auto_sync?: boolean; language?: Language }) {
    if (!profile) return
    setPrefError(null)
    const previous = profile
    setProfile({ ...profile, ...patch })
    try {
      setProfile(await updatePreferences(profile.id, patch))
    } catch {
      setProfile(previous)
      setPrefError('Could not save the preference. Try again.')
    }
  }

  async function logout() {
    await signOut()
    navigate('/login', { replace: true })
  }

  return (
    <>
      <div className="page-head">
        <div>
          <div className="crumb">
            SETTINGS / <b>GENERAL</b>
          </div>
          <h1>Settings &amp; Account</h1>
        </div>
        <div className="head-actions">
          <span className="date-chip">{todayLabel}</span>
          <button className="btn-chip" onClick={() => save()} disabled={!dirty || saving}>
            <Icon name="save" size={14} /> Save Changes
          </button>
        </div>
      </div>

      <div className="grid-2">
        <form className="card" onSubmit={save} noValidate>
          <div className="card-title">
            <span style={{ color: 'var(--accent)' }}><Icon name="id" /></span>
            <span className="label">Therapist Profile</span>
            <span className="pill ok">Verified Account</span>
          </div>
          <div className="identity">
            <div className="tile">{initials(profile.full_name)}</div>
            <div>
              <h2>{profile.full_name}</h2>
              <p className="muted" style={{ fontSize: 13 }}>Physiotherapist • {profile.specialization || 'Specialization not set'}</p>
              <span className="id-chip">@{profile.username}{profile.legacy_user_id ? ` • ${profile.legacy_user_id}` : ''}</span>
            </div>
          </div>
          <Field labelClass="label" label="Full legal name" icon="user" value={form.fullName} onValue={(v) => edit({ fullName: v })} />
          <Field labelClass="label" label="Professional email" icon="at" type="email" value={form.email} onValue={(v) => edit({ email: v })} />
          <div className="two-col">
            <Field labelClass="label" label="Clinical specialization" icon="stethoscope" value={form.specialization} onValue={(v) => edit({ specialization: v })} />
            <div className="field">
              <span className="label">Active patient load</span>
              <div className="input-wrap">
                <Icon name="users" />
                <span className="readonly-value">{patientCount == null ? '…' : patientLoadLabel(patientCount)}</span>
              </div>
            </div>
          </div>
          <Field labelClass="label" label="Primary clinical affiliation" icon="building" value={form.affiliation} onValue={(v) => edit({ affiliation: v })} />
          {message && <p className={message.kind === 'ok' ? 'success-text' : 'error-text'} style={{ marginTop: 12 }}>{message.text}</p>}
          <button className="btn-primary" type="submit" disabled={!dirty || saving} style={{ width: 'auto', padding: '0 24px' }}>
            {saving ? 'Saving…' : 'Update Profile Information'}
          </button>
          <hr className="divider" />
          <div className="row-between">
            <span className="muted" style={{ fontSize: 12, fontWeight: 600 }}>Login email: {session?.user.email}</span>
            <Link to="/patients" className="link-btn">View Caseload</Link>
          </div>
        </form>

        <div className="stack">
          <section className="card">
            <div className="card-title">
              <span style={{ color: 'var(--accent)' }}><Icon name="sliders" /></span>
              <span className="label">System Preferences</span>
            </div>
            <Pref title="Dark Mode" caption="Dark clinical interface for low-light reading. Saved in this browser." checked={theme === 'dark'} onChange={(v) => setTheme(v ? 'dark' : 'light')} />
            <Pref title="Telemetry Audio Alerts" caption="Audible warning when a patient exceeds the range-of-motion ceiling." checked={profile.audio_alerts} onChange={(v) => changePref({ audio_alerts: v })} />
            <div className="pref">
              <div>
                <h3>Clinical Terminology &amp; Language</h3>
                <p>Preferred language for clinical notes.</p>
              </div>
              <select className="select" value={profile.language} onChange={(e) => changePref({ language: e.target.value as Language })} aria-label="Language">
                <option value="ENGLISH">English (Clinical)</option>
                <option value="INDONESIAN">Bahasa Indonesia</option>
              </select>
            </div>
            <Pref title="Auto-Sync Patient Sessions" caption="Refresh patient sessions from the cloud automatically." checked={profile.auto_sync} onChange={(v) => changePref({ auto_sync: v })} />
            {prefError && <p className="error-text">{prefError}</p>}
          </section>

          <section className="card">
            <div className="card-title">
              <span style={{ color: 'var(--accent)' }}><Icon name="shield" /></span>
              <span className="label">Security &amp; Active Session</span>
            </div>
            <div className="session-box">
              <span style={{ color: 'var(--accent)' }}><Icon name="monitor" size={22} /></span>
              <div>
                <h3 style={{ fontSize: 17 }}>This browser</h3>
                <p className="muted" style={{ fontSize: 12 }}>
                  Last sign in {session?.user.last_sign_in_at ? new Date(session.user.last_sign_in_at).toLocaleString() : 'unknown'}
                </p>
              </div>
              <span className="pill ok">Active Session</span>
            </div>
            <div className="pref">
              <div>
                <h3>Account Password</h3>
                <p>{passwordUpdated ? 'Updated just now.' : 'Change the password used to sign in to the portal.'}</p>
              </div>
              <button className="btn-chip" onClick={() => setShowPassword(true)}>Update Password</button>
            </div>
            <div className="danger-box">
              <div>
                <h3><Icon name="warning" size={16} /> Session Sign Out</h3>
                <p className="muted" style={{ fontSize: 12 }}>End this session and return to the therapist sign in.</p>
              </div>
              <button className="btn-danger" onClick={() => setConfirmLogout(true)}>Log Out of Account</button>
            </div>
          </section>
        </div>
      </div>

      {showPassword && (
        <PasswordModal
          email={session?.user.email ?? ''}
          onClose={() => setShowPassword(false)}
          onDone={() => {
            setShowPassword(false)
            setPasswordUpdated(true)
          }}
        />
      )}

      {confirmLogout && (
        <div className="modal-back" role="dialog" aria-modal="true">
          <div className="card modal">
            <h2>Log out of the portal?</h2>
            <p className="muted" style={{ marginTop: 8 }}>You will return to the therapist sign in.</p>
            <div className="modal-actions">
              <button className="btn-chip" onClick={() => setConfirmLogout(false)}>Cancel</button>
              <button className="btn-danger" onClick={logout}>Log Out</button>
            </div>
          </div>
        </div>
      )}
    </>
  )
}

function Pref({ title, caption, checked, onChange }: { title: string; caption: string; checked: boolean; onChange: (v: boolean) => void }) {
  return (
    <div className="pref">
      <div>
        <h3>{title}</h3>
        <p>{caption}</p>
      </div>
      <label className="switch">
        <input type="checkbox" checked={checked} onChange={(e) => onChange(e.target.checked)} aria-label={title} />
        <span />
      </label>
    </div>
  )
}

function PasswordModal({ email, onClose, onDone }: { email: string; onClose: () => void; onDone: () => void }) {
  const [current, setCurrent] = useState('')
  const [next, setNext] = useState('')
  const [confirmation, setConfirmation] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  async function submit(e: FormEvent) {
    e.preventDefault()
    const invalid = validateNewPassword(current, next, confirmation)
    if (invalid) return setError(invalid)
    setBusy(true)
    const check = await supabase.auth.signInWithPassword({ email, password: current })
    if (check.error) {
      setBusy(false)
      return setError('Current password is incorrect.')
    }
    const { error: updateError } = await supabase.auth.updateUser({ password: next })
    setBusy(false)
    if (updateError) return setError(updateError.message)
    onDone()
  }

  return (
    <div className="modal-back" role="dialog" aria-modal="true">
      <form className="card modal" onSubmit={submit} noValidate>
        <h2>Update Password</h2>
        <PasswordField label="Current password" autoComplete="current-password" value={current} onValue={(v) => { setCurrent(v); setError(null) }} />
        <PasswordField label="New password" autoComplete="new-password" placeholder="At least 8 characters" value={next} onValue={(v) => { setNext(v); setError(null) }} />
        <PasswordField label="Confirm new password" autoComplete="new-password" value={confirmation} onValue={(v) => { setConfirmation(v); setError(null) }} />
        {error && <p className="error-text" style={{ marginTop: 10 }}>{error}</p>}
        <div className="modal-actions">
          <button type="button" className="btn-chip" onClick={onClose}>Cancel</button>
          <button type="submit" className="btn-chip" style={{ background: 'var(--accent-strong)', color: 'var(--on-accent)' }} disabled={busy}>
            {busy ? 'Updating…' : 'Update'}
          </button>
        </div>
      </form>
    </div>
  )
}
