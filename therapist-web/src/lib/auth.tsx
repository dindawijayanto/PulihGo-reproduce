import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react'
import type { Session } from '@supabase/supabase-js'
import { supabase } from './supabase'
import { fetchProfile } from './api'
import type { Profile } from './types'

export type SignInResult = { ok: true } | { ok: false; message: string }

interface AuthState {
  loading: boolean
  session: Session | null
  profile: Profile | null
  recovering: boolean
  signIn: (email: string, password: string) => Promise<SignInResult>
  signOut: () => Promise<void>
  setProfile: (profile: Profile) => void
  finishRecovery: () => void
}

const AuthContext = createContext<AuthState | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [loading, setLoading] = useState(true)
  const [session, setSession] = useState<Session | null>(null)
  const [profile, setProfile] = useState<Profile | null>(null)
  const [recovering, setRecovering] = useState(false)

  const loadProfile = useCallback(async (next: Session | null) => {
    if (!next) {
      setProfile(null)
      return
    }
    try {
      const loaded = await fetchProfile(next.user.id)
      setProfile(loaded?.role === 'THERAPIST' ? loaded : null)
    } catch {
      setProfile(null)
    }
  }, [])

  useEffect(() => {
    let active = true
    supabase.auth.getSession().then(async ({ data }) => {
      if (!active) return
      setSession(data.session)
      await loadProfile(data.session)
      if (active) setLoading(false)
    })
    const { data: sub } = supabase.auth.onAuthStateChange((event, next) => {
      if (event === 'PASSWORD_RECOVERY') setRecovering(true)
      setSession(next)
      if (event === 'SIGNED_OUT') setProfile(null)
    })
    return () => {
      active = false
      sub.subscription.unsubscribe()
    }
  }, [loadProfile])

  const signIn = useCallback(async (email: string, password: string): Promise<SignInResult> => {
    const { data, error } = await supabase.auth.signInWithPassword({ email: email.trim(), password })
    if (error || !data.session) return { ok: false, message: 'Invalid credentials entered.' }
    const loaded = await fetchProfile(data.session.user.id).catch(() => null)
    if (loaded?.role !== 'THERAPIST') {
      await supabase.auth.signOut()
      return { ok: false, message: 'This portal is for therapist accounts only.' }
    }
    setSession(data.session)
    setProfile(loaded)
    return { ok: true }
  }, [])

  const signOut = useCallback(async () => {
    await supabase.auth.signOut()
    setSession(null)
    setProfile(null)
  }, [])

  const value = useMemo<AuthState>(
    () => ({
      loading,
      session,
      profile,
      recovering,
      signIn,
      signOut,
      setProfile,
      finishRecovery: () => setRecovering(false),
    }),
    [loading, session, profile, recovering, signIn, signOut],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth(): AuthState {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used inside AuthProvider')
  return ctx
}
