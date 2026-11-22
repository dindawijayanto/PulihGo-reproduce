import { useState, type FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { Gateway } from '../components/Gateway'
import { Field } from '../components/Field'
import { Icon } from '../components/Icon'
import { isValidEmail } from '../lib/logic'
import { supabase } from '../lib/supabase'

export function ForgotPasswordPage() {
  const [email, setEmail] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [sentTo, setSentTo] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  async function submit(e: FormEvent) {
    e.preventDefault()
    if (!isValidEmail(email)) {
      setError('Enter a valid email address.')
      return
    }
    setBusy(true)
    const { error: sendError } = await supabase.auth.resetPasswordForEmail(email.trim(), {
      redirectTo: `${window.location.origin}/reset-password`,
    })
    setBusy(false)
    if (sendError && sendError.status === 429) {
      setError('Too many reset requests. Please wait a few minutes and try again.')
      return
    }
    setSentTo(email.trim())
  }

  return (
    <Gateway>
      <form className="card gateway-card" onSubmit={submit} noValidate>
        <Link to="/login" className="btn-chip" aria-label="Back to sign in" style={{ display: 'inline-flex' }}>
          <Icon name="back" size={16} />
        </Link>
        <div className="center" style={{ marginTop: 16 }}>
          <div className="avatar" style={{ margin: '0 auto', width: 48, height: 48, background: 'var(--chip)', color: 'var(--accent)', cursor: 'default' }}>
            <Icon name={sentTo ? 'send' : 'refresh'} size={22} />
          </div>
          {sentTo ? (
            <>
              <h1 style={{ fontSize: 22, marginTop: 16 }}>Check your inbox</h1>
              <p className="sub">
                If a therapist account exists for <b>{sentTo}</b>, a link to reset the password has been sent there.
              </p>
            </>
          ) : (
            <>
              <h1 style={{ fontSize: 22, marginTop: 16 }}>Forgot your password?</h1>
              <p className="sub">Enter your registered email address to request password reset instructions.</p>
            </>
          )}
        </div>
        {sentTo ? (
          <>
            <Link to="/login" className="btn-primary" style={{ display: 'grid', placeItems: 'center' }}>
              Back to Sign In
            </Link>
            <button type="button" className="link-btn plain" style={{ width: '100%', marginTop: 12 }} onClick={() => setSentTo(null)}>
              Use a different email
            </button>
          </>
        ) : (
          <>
            <Field
              label="Email Address"
              icon="mail"
              type="email"
              autoComplete="email"
              placeholder="therapist@example.com"
              value={email}
              onValue={(v) => {
                setEmail(v)
                setError(null)
              }}
              invalid={error != null}
            />
            {error && <p className="error-text" style={{ marginTop: 8 }}>{error}</p>}
            <button className="btn-primary" type="submit" disabled={busy}>
              {busy ? 'Sending…' : 'Send Reset Link ➤'}
            </button>
            <Link to="/login" className="link-btn plain" style={{ display: 'block', textAlign: 'center', marginTop: 12 }}>
              Back to Sign In
            </Link>
          </>
        )}
        <hr className="divider" />
        <p className="label center">
          <Icon name="shield" size={12} /> Authorized therapist access only.
        </p>
      </form>
    </Gateway>
  )
}
