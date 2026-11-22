import { useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { Gateway } from '../components/Gateway'
import { PasswordField } from '../components/Field'
import { useAuth } from '../lib/auth'
import { supabase } from '../lib/supabase'

export function ResetPasswordPage() {
  const { session, recovering, finishRecovery, signOut } = useAuth()
  const navigate = useNavigate()
  const [password, setPassword] = useState('')
  const [confirmation, setConfirmation] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)
  const [done, setDone] = useState(false)

  async function submit(e: FormEvent) {
    e.preventDefault()
    if (password.length < 8) return setError('New password must be at least 8 characters.')
    if (!/[^A-Za-z\s]/.test(password)) return setError('New password must contain at least one number or symbol.')
    if (password !== confirmation) return setError('Passwords do not match.')
    setBusy(true)
    const { error: updateError } = await supabase.auth.updateUser({ password })
    setBusy(false)
    if (updateError) return setError(updateError.message)
    finishRecovery()
    await signOut()
    setDone(true)
  }

  const linkValid = session != null || recovering

  return (
    <Gateway>
      <form className="card gateway-card" onSubmit={submit} noValidate>
        <div className="portal-chip">THERAPIST PORTAL</div>
        <h1>{done ? 'Password updated' : 'Set a new password'}</h1>
        {done ? (
          <>
            <p className="sub">Your password has been changed. Sign in again with the new password.</p>
            <button type="button" className="btn-primary" onClick={() => navigate('/login', { replace: true })}>
              Back to Sign In
            </button>
          </>
        ) : !linkValid ? (
          <>
            <p className="sub">This reset link is invalid or has expired. Request a new one.</p>
            <Link to="/forgot-password" className="btn-primary" style={{ display: 'grid', placeItems: 'center' }}>
              Request new link
            </Link>
          </>
        ) : (
          <>
            <p className="sub">Choose a new password for your therapist account.</p>
            <PasswordField label="New password" autoComplete="new-password" placeholder="At least 8 characters" value={password} onValue={(v) => { setPassword(v); setError(null) }} />
            <PasswordField label="Confirm new password" autoComplete="new-password" placeholder="Repeat new password" value={confirmation} onValue={(v) => { setConfirmation(v); setError(null) }} />
            {error && <p className="error-text" style={{ marginTop: 8 }}>{error}</p>}
            <button className="btn-primary" type="submit" disabled={busy}>
              {busy ? 'Saving…' : 'Update Password'}
            </button>
          </>
        )}
      </form>
    </Gateway>
  )
}
