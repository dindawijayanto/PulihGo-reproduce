import { Link } from 'react-router-dom'

export function PlaceholderPage({ section, title, task }: { section: string; title: string; task: string }) {
  return (
    <>
      <div className="page-head">
        <div>
          <div className="crumb">
            {section} / <b>{title.toUpperCase()}</b>
          </div>
          <h1>{title}</h1>
        </div>
      </div>
      <div className="card empty">
        <h2>Coming soon</h2>
        <p style={{ margin: '8px 0' }}>This page is being built in {task}.</p>
        <Link to="/patients" className="link-btn">Open patient sessions</Link>
      </div>
    </>
  )
}
