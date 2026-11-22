import { useEffect, useRef, useState } from 'react'
import { Link, NavLink, Outlet, useNavigate } from 'react-router-dom'
import { useAuth } from '../lib/auth'
import { initials } from '../lib/logic'
import { Brand } from './Gateway'

const tabs = [
  { to: '/dashboard', label: 'Dashboard' },
  { to: '/patients', label: 'Therapy' },
  { to: '/reports', label: 'Reports' },
  { to: '/settings', label: 'Settings' },
]

export function PortalLayout() {
  const { profile, signOut } = useAuth()
  const navigate = useNavigate()
  const [menuOpen, setMenuOpen] = useState(false)
  const menuRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    if (!menuOpen) return
    const close = (e: MouseEvent) => {
      if (menuRef.current && !menuRef.current.contains(e.target as Node)) setMenuOpen(false)
    }
    document.addEventListener('mousedown', close)
    return () => document.removeEventListener('mousedown', close)
  }, [menuOpen])

  const name = profile?.full_name ?? 'Therapist'

  return (
    <div className="portal">
      <header className="portal-header">
        <div className="portal-brand">
          <Brand />
          <div className="brand-sub">CLINICAL HUB</div>
        </div>
        <nav className="nav" aria-label="Portal">
          {tabs.map((t) => (
            <NavLink key={t.to} to={t.to} className={({ isActive }) => (isActive ? 'active' : '')}>
              {t.label}
            </NavLink>
          ))}
        </nav>
        <div className="who">
          <div className="who-text">
            <b>{name}</b>
            <small>PHYSIOTHERAPIST</small>
          </div>
          <div className="menu" ref={menuRef}>
            <button className="avatar" aria-label="Account menu" onClick={() => setMenuOpen((v) => !v)}>
              {initials(name)}
            </button>
            {menuOpen && (
              <div className="menu-pop" role="menu">
                <Link to="/settings" role="menuitem" onClick={() => setMenuOpen(false)}>
                  Settings
                </Link>
                <button
                  role="menuitem"
                  onClick={async () => {
                    setMenuOpen(false)
                    await signOut()
                    navigate('/login', { replace: true })
                  }}
                >
                  Log out
                </button>
              </div>
            )}
          </div>
        </div>
      </header>
      <main className="portal-main">
        <Outlet />
      </main>
      <footer className="portal-foot">
        <div>PulihGo Digital Rehabilitation Platform • Therapist Portal</div>
        <div>
          <span className="status-dot" />
          Cloud synced
        </div>
      </footer>
    </div>
  )
}
