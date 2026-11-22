# PGF-INT-04 — Device test: sensor, calibration and Live Exercise

Goal: confirm on a **real phone** that sensor reading, recalibration, movement direction, ±180° normalization, baseline, target hit and the ceiling warning behave as designed. Emulator results do not count for this task.

Fill in the **Result** (✅ / ❌) and **Notes** columns while testing. Attach screenshots or a short screen recording to the PR.

## 1. Test info

| Item | Value |
|---|---|
| Tester | |
| Date | |
| Phone model / Android version | |
| Sensor source shown in app (Game rotation vector / Rotation vector / Accelerometer) | |
| App commit (`git log -1 --oneline`) | |
| Exercise | Forearm Supination and Pronation (target 70°, ceiling 80°) |

## 2. Setup

1. Phone: Settings → About phone → tap **Build number** 7× → Developer options → enable **USB debugging**.
2. Connect the phone by USB, select it in Android Studio, press **Run**.
3. Log in as `patient01` / `demo123` → Start exercise → Forearm Supination / Pronation.
4. Calibration starts automatically when the Live Exercise screen opens, so be in position before tapping Continue.
5. Strap the phone to the forearm (screen facing out, top of the phone towards the hand). Elbow against the body and bent 90°, palm facing inward (thumb up). This is the **neutral / 0°** position.
6. Tap Continue to calibration and hold still.

Automated equivalents of each check run on the JVM in `app/src/test/java/com/pulihgo/app/sensor/DeviceScenarioTest.kt` (`./gradlew testDebugUnitTest`). They prove the logic; this checklist proves the real sensor and screen.

## 3. Checklist

### A. Sensor reading

| # | Step | Expected | Result | Notes |
|---|---|---|---|---|
| A1 | Open the Live Exercise screen | Sensor connects without a "Sensor unavailable" / "Sensor could not start" message | | |
| A2 | Rotate the forearm slowly | Angle on screen changes smoothly, no freezing or jumps | | |
| A3 | Leave the phone still for 10 s | Angle stays steady (drift ≤ 2°) | | |
| A4 | Lock the screen for 5 s, unlock and return | Reading resumes; if it paused, "Sensor reading paused" disappears once data flows again | | |

### B. Calibration

| # | Step | Expected | Result | Notes |
|---|---|---|---|---|
| B1 | Hold the arm still in neutral while the screen opens | "Hold still while calibrating", then calibration finishes by itself; angle shows **0°** (±2°) | | |
| B2 | Leave the screen, re-enter it and keep rotating the arm while it calibrates | "Calibration was unstable" with a **Retry calibration** button | | |
| B3 | Hold still in neutral and tap **Retry calibration** | Calibrates; neutral reads 0° | | |
| B4 | Leave the screen, re-enter it with the palm tilted ~30° up, then return to the old neutral | The tilted position became 0°; the old neutral now reads about 30° | | |
| B5 | Leave and re-enter in neutral again | Neutral reads 0° again | | |

### C. Movement direction

| # | Step | Expected | Result | Notes |
|---|---|---|---|---|
| C1 | **Supination**: turn palm up | Angle grows; record the sign shown (+ or −) | | |
| C2 | **Pronation**: turn palm down | Angle grows in the opposite direction; sign opposite to C1 | | |
| C3 | Same movement on the other arm | Displayed ROM magnitude is similar (sign may flip) | | |

The ROM shown during exercise is the absolute value, so both directions count toward target and ceiling. If C1 shows a negative sign, note it here: it only matters for reports that separate supination from pronation.

### D. Angle normalization (±180°)

| # | Step | Expected | Result | Notes |
|---|---|---|---|---|
| D1 | Calibrate with the phone almost upside down (forearm rotated ~170°), then rotate 30–40° further | Angle increases smoothly (e.g. 10° → 40°), never jumps to ~300° or flips sign suddenly | | |
| D2 | Rotate back to the calibrated position | Angle returns to ~0° | | |

### E. Live Exercise phases

| # | Step | Expected | Result | Notes |
|---|---|---|---|---|
| E1 | Rotate to about 40–50° and back | Stays in **baseline** state; PEAK ANGLE updates; rep counted when returning near 0° | | |
| E2 | Rotate past 70° and back | **TARGET HIT!** appears and stays while returning; REPETITIONS +1 | | |
| E3 | Rotate slowly towards 75–80° | "Approaching ROM limit (80°). Move slowly." | | |
| E4 | Rotate past 80° | **WARNING: SLOW DOWN & RETURN** / "Exceeded ROM limit of 80°. Return slowly." | | |
| E5 | Return below the ceiling | Warning clears, back to TARGET HIT then baseline at rest | | |
| E6 | Small shaking around neutral for 10 s | No repetitions are added | | |
| E7 | Half movement: rotate to 50° and stay there | No repetition until the arm returns to start | | |
| E8 | Tap **Finish Practice**, answer "How did that feel?" | Session saved; returns to Preparation with the saved banner | | |
| E9 | Start again, tap **Stop, it hurts** | Session saved as stopped due to pain | | |

### F. Cloud sync (PGF-INT-01 follow-up)

| # | Step | Expected | Result | Notes |
|---|---|---|---|---|
| F1 | Finish a session with internet on | Toast "Session synced to the cloud." | | |
| F2 | Finish a session in airplane mode | Toast "Saved on this phone. It will sync when you're online." | | |
| F3 | Turn internet back on and reopen the app | Pending session uploads | | |
| F4 | Therapist web → Therapy → Alya Putri | Sessions from F1 and F3 appear with matching reps and peak ROM | | |

## 4. Result summary

| Area | Pass / Fail | Issue / follow-up |
|---|---|---|
| A. Sensor reading | | |
| B. Calibration | | |
| C. Direction | | |
| D. Normalization | | |
| E. Live Exercise | | |
| F. Cloud sync | | |

Record failures as GitHub issues (or in the task tracker Blocker / Notes column) with the step number, the phone model and a screenshot.
