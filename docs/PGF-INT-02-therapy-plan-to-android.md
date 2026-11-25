# PGF-INT-02 — Therapy Plan → Patient Exercise (Android)

## Acceptance criteria from Task Tracker

> Parameter dari Therapy Plan dapat digunakan oleh Patient App ketika latihan dimulai. Perubahan target dan batas latihan tercermin pada sesi berikutnya.

This package targets main `cf618ae980c7bc5752ab5f90f3698355c075ccff` (PR #25 THR-05 merged).
The Therapist Portal continues owning writes to `public.therapy_plans`; Android only reads that RLS-protected table with its patient's Supabase access token.

## Behavior

1. Patient logs in. Existing `SessionCloudSync` continues owning credentials and upload jobs. Plan reads borrow a freshly refreshed access token under its existing mutex, avoiding refresh-token rotation races.
2. On opening the Exercise Library and on entering Preparation, Android queries Supabase's `therapy_plans` filtered by the current cloud patient id. RLS also verifies the logged-in patient owns the record.
3. Only validated, patient-scoped plans can affect exercise parameters. Missing plans mean the catalog defaults **only after a successful cloud check or prior cached successful check**. A malformed remote response never replaces valid local cache.
4. Save successful results in per-user/per-patient SharedPreferences (numeric settings + Active/Paused status). Clinical notes are not written to cache. Offline repeats can use the last verified prescription. Without a verified snapshot, start is disabled when Supabase is configured and the patient is mapped, until online confirmation. Offline catalog/demo accounts with no matching Supabase patient remain a separate demo path.
5. The library displays the latest known target ROM. Preparation displays target, safety ceiling, reps, sets/frequency, and notes when online. A Paused prescription blocks Calibrate & Start, including offline when cached.
6. At Calibrate & Start, copy the effective `Exercise`. `LiveExerciseTracker` thus receives the intended target/ceiling and is not changed mid-session by a later therapist edit.
7. `PainCheckSessionGate` freezes the session's target ROM and reps alongside measured metrics. `ExerciseSessionRecordJson` persists these fields locally with backward-compatible defaults. Existing Supabase `exercise_sessions.target_rom_deg` and `target_reps` columns receive the snapshot via `CloudSessionMapper`; **no new SQL migration is needed**.

## Important limits / ownership boundaries

- No writes from Android to `therapy_plans` or changes to RLS, SQL, web portal or the sensor/repetition/smoothness algorithms.
- `sets_per_session`, `sessions_per_week` are visible prescription guidance. The existing tracker counts full repetitions; it does NOT implement a set scheduler or weekly adherence enforcement.
- No previous-epoch ceiling snapshot field exists in `exercise_sessions` schema; reports can see saved target ROM and repetitions, but historical ceiling is still inferred elsewhere. PGF-INT-05 should verify this limitation and decide with the team whether a schema extension is needed.
- The Android project currently has local demo login plus separate Supabase password token in `SessionCloudSync`; the first plan read requires its patient cloud login credentials. Test with mapped demo Patient P001/U002 or P002/U003 and the same configured Supabase project. Never commit local.properties.
- The SharedPreferences cache is device-local and is not encrypted; avoid storing clinical free text. A stale-but-validated cached plan may remain until network returns. This is a demonstration workflow; do not use it for clinical care without additional safety design and authorization testing.

## Suggested QA

1. In Therapist web, save plan for P001/EX001: target 60°, ceiling 75°, 8 reps, 2 sets, 4/week, Active.
2. On Android, log in as matching Patient `patient01` / `alya@pulihgo.demo`, open Exercise → Preparation. Verify 60° / 75° / 8 reps, source marked synced, and Calibrate & Start enabled.
3. Start session, calibrate, move carefully. `LiveExerciseTracker` target and ROM warning should reflect the new values; finish with No pain. Verify `target_rom_deg=60` and `target_reps=8` in the synced Supabase session (new session, not seeded).
4. Change Therapist plan to 65° / 78°, return to Library and re-enter Preparation. Verify new values apply to **next** session, not previous saved session.
5. Pause plan. Online refresh should block Calibrate & Start. Disconnect network; cached Paused plan must still block.
6. Re-activate, sync once, then disconnect network. Cache should retain valid plan and allow offline exercises; local session must still be saved and sync later.
7. Separate P001/P002 login: ensure no other patient's cached plan is visible.
8. No plan yet: a successful empty read displays catalog defaults. On brand-new offline install with Supabase configured, Start is disabled until first successful check (safe fallback).
9. Test old local session JSON still decodes without targets; new local sessions round-trip with targets.

## Local validation

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug
.\gradlew.bat :app:installDebug
```

The PR should be reviewed by Dinda because `SessionCloudSync.kt` is shared ownership, Erza because record/mapper fields are shared, and the Patient UI owner for preparation visuals. Do not merge until device QA and role isolation are verified.
