import type { ReactNode } from 'react'

export function Brand({ tag }: { tag?: string }) {
  return (
    <div className="brand">
      <img src="/logo.png" alt="" />
      <div className="brand-name">
        Pulih<span>Go</span>
      </div>
      {tag && <div className="brand-tag">{tag}</div>}
    </div>
  )
}

export function Gateway({ children }: { children: ReactNode }) {
  return (
    <div className="gateway">
      <header className="gateway-top">
        <Brand tag="THERAPIST PORTAL" />
        <div>
          <span className="status-dot" />
          Clinical Telemetry Secure Gateway
        </div>
      </header>
      <main className="gateway-main">{children}</main>
      <footer className="gateway-foot">
        <div>PulihGo Biomechanical Telemetry © 2026</div>
        <div>Secured by Supabase Auth</div>
      </footer>
    </div>
  )
}
