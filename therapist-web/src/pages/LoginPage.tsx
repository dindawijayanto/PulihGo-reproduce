import { useState, type FormEvent } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { Brand, Gateway } from '../components/Gateway'
import { Field, PasswordField } from '../components/Field'
import { Icon } from '../components/Icon'
import { useAuth } from '../lib/auth'
import { isValidEmail } from '../lib/logic'

export function LoginPage() {
  const { signIn } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [failed, setFailed] = useState(false)
  const [busy, setBusy] = useState(false)

  async function submit(e: FormEvent) {
    e.preventDefault()
    let message: string | null = null
    if (!isValidEmail(email)) message = 'Enter a valid email address.'
    else if (!password) message = 'Enter your password.'
    if (message) {
      setError(message)
      setFailed(true)
      return
    }
    setBusy(true)
    const result = await signIn(email, password)
    setBusy(false)
    if (result.ok) {
      const from = (location.state as { from?: string } | null)?.from
      navigate(from && from !== '/login' ? from : '/dashboard', { replace: true })
    } else {
      setError(result.message)
      setFailed(true)
    }
  }

  return (
    <Gateway>
      <form className="card gateway-card" onSubmit={submit} noValidate>
        <Brand />
        <div className="portal-chip">THERAPIST PORTAL</div>
        <h1>Welcome back</h1>
        <p className="sub">Sign in to access your patient monitoring dashboard.</p>
        {failed && (
          <div className="alert" role="alert">
            <Icon name="alert" />
            <span>Unable to sign in. Please check your email and password.</span>
          </div>
        )}
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
          invalid={failed}
        />
        <PasswordField
          label="Password"
          autoComplete="current-password"
          placeholder="Enter your password"
          value={password}
          onValue={(v) => {
            setPassword(v)
            setError(null)
          }}
          invalid={failed}
        />
        <div className="row-between" style={{ marginTop: 10 }}>
          <span className="error-text">{error}</span>
          <Link to="/forgot-password" className="link-btn">
            Forgot password?
          </Link>
        </div>
        <button className="btn-primary" type="submit" disabled={busy}>
          {busy ? 'Signing in…' : failed ? 'Try Again →' : 'Sign In →'}
        </button>
        <hr className="divider" />
        <p className="label center">Authorized therapist access only.</p>
        <p className="faint center" style={{ marginTop: 6, fontSize: 11 }}>
          Demo: therapist@pulihgo.demo / demo123
        </p>
      </form>
    </Gateway>
  )
}
