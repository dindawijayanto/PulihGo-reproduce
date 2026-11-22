const paths: Record<string, string> = {
  mail: 'M3 6h18v12H3z M3 7l9 6 9-6',
  lock: 'M6 11h12v9H6z M8.5 11V8a3.5 3.5 0 0 1 7 0v3',
  eye: 'M2 12s3.5-6 10-6 10 6 10 6-3.5 6-10 6S2 12 2 12z M12 9.5a2.5 2.5 0 1 0 0 5 2.5 2.5 0 0 0 0-5z',
  eyeOff: 'M3 3l18 18 M10.6 6.1A9.8 9.8 0 0 1 12 6c6.5 0 10 6 10 6a17 17 0 0 1-3.2 3.8 M6.6 6.6C3.7 8.4 2 12 2 12s3.5 6 10 6c1.6 0 3-.4 4.2-1',
  alert: 'M12 3a9 9 0 1 0 0 18 9 9 0 0 0 0-18z M12 7.5v5.5 M12 16.2v.3',
  back: 'M19 12H5 M11 6l-6 6 6 6',
  arrow: 'M5 12h14 M13 6l6 6-6 6',
  user: 'M12 4a4 4 0 1 0 0 8 4 4 0 0 0 0-8z M4 20c1.5-3.5 4.5-5 8-5s6.5 1.5 8 5',
  at: 'M16 12a4 4 0 1 1-8 0 4 4 0 0 1 8 0z M16 12v1.5a2.5 2.5 0 0 0 5 0V12a9 9 0 1 0-3.5 7.1',
  shield: 'M12 3l8 3v6c0 4.5-3.4 8.2-8 9-4.6-.8-8-4.5-8-9V6z',
  sliders: 'M4 6h10 M18 6h2 M16 4v4 M4 12h4 M12 12h8 M10 10v4 M4 18h12 M20 18h0 M18 16v4',
  id: 'M4 5h16v14H4z M8 10a2 2 0 1 0 4 0 2 2 0 0 0-4 0 M7 16c.7-1.5 2-2 3-2s2.3.5 3 2 M15 9h3 M15 13h3',
  monitor: 'M3 5h18v11H3z M8 20h8 M12 16v4',
  warning: 'M12 3l10 18H2z M12 10v5 M12 18v.3',
  refresh: 'M20 11a8 8 0 0 0-14.9-3.5 M4 4v4h4 M4 13a8 8 0 0 0 14.9 3.5 M20 20v-4h-4',
  send: 'M4 12l16-8-6 16-2.5-6.5z',
  building: 'M5 21V5l7-2v18 M12 9h7v12 M8 8h1 M8 12h1 M8 16h1 M15 13h1 M15 17h1 M3 21h18',
  users: 'M9 5a3.5 3.5 0 1 0 0 7 3.5 3.5 0 0 0 0-7z M2 20c1-3 3.8-4.5 7-4.5s6 1.5 7 4.5 M16 5.2a3.3 3.3 0 0 1 0 6.4 M18.5 15.8c1.6.7 2.8 2 3.5 4.2',
  stethoscope: 'M6 3v6a4 4 0 0 0 8 0V3 M10 13v2a5 5 0 0 0 10 0v-2 M20 11a2 2 0 1 0 0 .1',
  calendar: 'M4 6h16v14H4z M4 10h16 M8 3v4 M16 3v4',
  save: 'M5 4h11l3 3v13H5z M8 4v5h7V4 M8 20v-6h8v6',
}

export function Icon({ name, size = 18 }: { name: keyof typeof paths; size?: number }) {
  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth={1.8}
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
    >
      {paths[name].split(' M').map((d, i) => (
        <path key={i} d={i === 0 ? d : 'M' + d} />
      ))}
    </svg>
  )
}
