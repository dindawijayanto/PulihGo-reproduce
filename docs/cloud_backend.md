# Cloud backend & Therapist web (PGF-THR-01, PGF-INT-01)

PulihGo uses **local + cloud** storage:

- **Patient app (Android)** saves every exercise session on the phone first (`ExerciseSessionStore`, works offline), then uploads it to Supabase.
- **Therapist portal (web, `therapist-web/`)** reads patients and sessions from Supabase.

```
Android (patient) ── local SharedPreferences ──► SessionCloudSync ──► Supabase (Postgres + Auth) ◄── therapist-web (React)
```

## Supabase project

- URL and publishable key go in local files only (never commit them):
  - Android: `local.properties`
    ```
    supabase.url=https://<project>.supabase.co
    supabase.publishableKey=sb_publishable_...
    ```
  - Web: `therapist-web/.env.local` (copy `.env.example`).
- Never put the `service_role` / secret key in the app or the web.
- Ask Dinda for the project URL and publishable key.

### Setting up a new project

1. Authentication → Users → Add user (Auto Confirm): `therapist@pulihgo.demo`, `alya@pulihgo.demo`, `bima@pulihgo.demo`, password `demo123`.
2. SQL Editor: run `supabase/schema.sql`, then `supabase/seed.sql` (both can be re-run safely).
3. Authentication → URL Configuration → Site URL `http://localhost:5173`.

## Schema

| Table | Key | Notes |
|---|---|---|
| `profiles` | `id` = `auth.users.id` | role, name, therapist profile fields and preferences; `legacy_user_id` = U001… |
| `patients` | `id` = P001… | `profile_id` (patient account), `therapist_id` (assigned therapist) |
| `exercises` | `id` = EX001… | target ROM, target reps, ROM ceiling |
| `exercise_sessions` | `id` = S001… (seeded) or `D-<userId>-<startedAtEpochMs>` (device) | metrics + `repetition_details` JSON |

Row-level security:

- A therapist can read only their assigned patients, those patients' profiles and sessions.
- A patient can read their own sessions and insert new `DEVICE` sessions for themselves only.
- Nobody can change `role`, delete sessions, or read anything without signing in.

To add a table for another feature (e.g. Therapy Plan, PGF-THR-05), add it to `schema.sql` with RLS enabled and explicit `grant`s, because new Supabase projects do not grant table access to `authenticated` automatically.

## Android sync (`data/cloud/`)

- On patient login the app signs in to Supabase with the same email/password and keeps only the refresh token.
- After a session is saved, on login, and on app start, `SessionCloudSync.syncPending` uploads sessions not yet synced. Uploads are idempotent (`on_conflict=id`, ignore duplicates).
- Offline or failed uploads stay pending and are retried next time; a toast tells the patient whether the session reached the cloud.
- Locally registered demo patients have no cloud account, so their sessions stay on the phone.

## Therapist web (`therapist-web/`)

```
cd therapist-web
npm install
npm run dev      # http://localhost:5173
npm test
```

| Route | Task | Page |
|---|---|---|
| `/login`, `/forgot-password`, `/reset-password` | THR-01 | Therapist sign in, reset email, set new password |
| `/settings` | THR-01 | Profile (saved to cloud), preferences, password change, logout |
| `/patients` | INT-01 | Assigned patients with session counts |
| `/patients/:patientId` | INT-01 | Sessions of one patient (auto-refresh every 15 s when Auto-Sync is on) |
| `/patients/:patientId/sessions/:sessionId` | INT-01 | Session detail with ROM target/ceiling outcome and per-repetition data |
| `/dashboard`, `/reports` | THR-02, THR-06 | Placeholders for the web versions |

Data access lives in `src/lib/api.ts`; pure helpers (ROM outcome, durations, validation) in `src/lib/logic.ts` with tests in `logic.test.ts`. New pages go in `src/pages/` and are registered in `src/App.tsx` inside the `RequireTherapist` route.

Forgot password uses Supabase's built-in email. With the default Supabase mailer, emails are only delivered to members of the Supabase project team and are rate limited, so the `@pulihgo.demo` addresses will not receive them.

## Demo check

1. Phone: log in as `patient01` / `demo123`, record a Live Exercise session → toast "Session synced to the cloud."
2. Laptop: `npm run dev` in `therapist-web`, sign in as `therapist@pulihgo.demo` / `demo123`.
3. Therapy → Alya Putri → the new session appears at the top (no "Seeded" tag) → open it for the detail.
