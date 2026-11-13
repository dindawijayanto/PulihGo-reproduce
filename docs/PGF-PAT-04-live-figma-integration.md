# PulihGo Figma Exercise Flow — incremental integration

**Visual sources:** user-supplied `Patient Hi-Fi Final.png`, Figma frames P07 (`76:1802`), P09 (`76:1876`), P10 (`76:1920`), P11 (`76:1968`), P12 (`76:1998`), P13 (`76:2048`), P14 (`76:2098`), P19 (`76:2775`), P20 (`76:2823`).

## Existing work preserved

- Keep P07 from uncommitted Figma-aligned `PatientExerciseScreens.kt`.
- P09 retains its card and gains saved outcome banners; its `Calibrate & Start` opens `LiveExerciseScreen` instead of the long `SensorCheckScreen`.
- Keep `SensorCheckScreen.kt`, sensor controller, repetition detector, smoothness evaluator, and ROM safety evaluator unchanged.
- No modifications to exercise mock JSON or existing authentication.

## State mapping

| Figma | Trigger | State |
| --- | --- | --- |
| P10 Live Baseline | Active calibrated sensor with angle below target | teal ring and slow-control instruction |
| P19 Target Hit | `abs(roll)` reaches configured target (currently 70°), remains below ceiling | green ring and TARGET HIT |
| P20 Ceiling Warning | `abs(roll)` reaches configured ceiling (currently 80°, Figma's 90° is mock data) | red ring and warning |
| P11 Pain Check | Finish Practice | dialog with No pain / Mild pain / Return to Session |
| P12 Stopped Due to Pain | Stop, it hurts | save with `STOPPED_DUE_TO_PAIN`, red banner on P09 |
| P13 No Pain | Choose No pain | save with `NO_PAIN`, green banner on P09 |
| P14 Mild Pain | Choose Mild pain | save with `MILD_PAIN`, green banner on P09 |

## Calibration and records

- `CalibratedAngleController` runs only while UI lifecycle is started.
- `Calibrate & Start` initializes calibration once the sensor begins streaming. `AngleCalibrator` requires stable samples; its failure state shows manual Retry Calibration.
- Repetitions use the existing `RepetitionDetector` and configured target ROM.
- Smoothness uses `SessionSmoothnessTracker` on completed reps; missing value remains `null`, never fabricated 0.
- `ExerciseSessionStore` stores local JSON per patient id and returns boolean write success. Failed write shows error and does not navigate.
- This patch does **not** rewrite the mock Home/Session Summary modules; persisted data is ready for integration by their owners.

## Tests and limitations

- Added 3 local JVM tests for encode/decode, missing smoothness and invalid pain level.
- All prior unit tests should still execute.
- Build and screenshot QA must run on Android environment; no claim of passing from this sandbox.
- Medical safety: warning threshold is app configuration, not a prescribed clinical limit. Never force ROM to test alerts.
