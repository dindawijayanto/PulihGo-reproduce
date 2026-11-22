import type { ReactNode } from 'react'
import { BrowserRouter, Navigate, Route, Routes, useLocation } from 'react-router-dom'
import { PortalLayout } from './components/PortalLayout'
import { AuthProvider, useAuth } from './lib/auth'
import { supabaseConfigured } from './lib/supabase'
import { ThemeProvider } from './lib/theme'
import { ForgotPasswordPage } from './pages/ForgotPasswordPage'
import { LoginPage } from './pages/LoginPage'
import { PatientSessionsPage } from './pages/PatientSessionsPage'
import { PatientsPage } from './pages/PatientsPage'
import { PlaceholderPage } from './pages/PlaceholderPage'
import { ResetPasswordPage } from './pages/ResetPasswordPage'
import { SessionDetailPage } from './pages/SessionDetailPage'
import { SettingsPage } from './pages/SettingsPage'

function RequireTherapist({ children }: { children: ReactNode }) {
  const { loading, profile } = useAuth()
  const location = useLocation()
  if (loading) return <div className="empty">Loading…</div>
  if (!profile) return <Navigate to="/login" replace state={{ from: location.pathname }} />
  return <>{children}</>
}

function GuestOnly({ children }: { children: ReactNode }) {
  const { loading, profile } = useAuth()
  if (loading) return <div className="empty">Loading…</div>
  if (profile) return <Navigate to="/dashboard" replace />
  return <>{children}</>
}

export function App() {
  if (!supabaseConfigured) {
    return (
      <div className="empty">
        Supabase is not configured. Copy <code>.env.example</code> to <code>.env.local</code> and fill in the project URL and publishable key.
      </div>
    )
  }
  return (
    <ThemeProvider>
      <AuthProvider>
        <BrowserRouter>
          <Routes>
            <Route path="/login" element={<GuestOnly><LoginPage /></GuestOnly>} />
            <Route path="/forgot-password" element={<GuestOnly><ForgotPasswordPage /></GuestOnly>} />
            <Route path="/reset-password" element={<ResetPasswordPage />} />
            <Route element={<RequireTherapist><PortalLayout /></RequireTherapist>}>
              <Route path="/dashboard" element={<PlaceholderPage section="DASHBOARD" title="Dashboard" task="PGF-THR-02" />} />
              <Route path="/patients" element={<PatientsPage />} />
              <Route path="/patients/:patientId" element={<PatientSessionsPage />} />
              <Route path="/patients/:patientId/sessions/:sessionId" element={<SessionDetailPage />} />
              <Route path="/reports" element={<PlaceholderPage section="REPORTS" title="Weekly Reports" task="PGF-THR-06" />} />
              <Route path="/settings" element={<SettingsPage />} />
            </Route>
            <Route path="*" element={<Navigate to="/dashboard" replace />} />
          </Routes>
        </BrowserRouter>
      </AuthProvider>
    </ThemeProvider>
  )
}
