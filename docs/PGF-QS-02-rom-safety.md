# PGF-QS-02 — ROM Safety Warning

## Scope
Displays real-time guidance when calibrated forearm ROM approaches or reaches the configured ceiling. The warning does not change sensor calibration, repetition detection, or movement smoothness logic.

## Configuration
- Source: `app/src/main/assets/mock_data/exercises.json`.
- Current demo exercise: `EX001` (Forearm Supination and Pronation).
- Ceiling: `defaultRomCeilingDeg` (currently 80°).
- Warning margin: 10° before ceiling (currently starts at 70°).
- Both positive and negative calibrated roll angles are treated using their absolute magnitude.
- This screen currently has one demo exercise; when exercise selection is connected to the sensor screen, pass the selected exercise ID instead of the current `EX001` constant.

## States
- Below warning threshold: informational *ROM dalam batas* card.
- At/above warning threshold, below ceiling: *Mendekati batas ROM*; instruct the user to slow down.
- At/above ceiling: *Batas ROM terlampaui*; instruct the user to return slowly to the starting/comfortable position.
- Not calibrated / sensor not streaming: no safety judgment; the card asks for calibration.
- Missing or invalid configuration: safety checks remain unavailable and the UI explicitly says so; no fabricated ceiling is used.

## Testing
Run `./gradlew :app:testDebugUnitTest :app:assembleDebug`, then manually verify on an Android device by calibrating and observing the warning states during gentle movement. Do not force painful movement to test the over-limit warning; simulated/unit-test inputs cover the exact threshold.

## Integration note
`SensorCheckScreen.kt` is also touched by PGF-QS-01. These features are independent; keep both the smoothness summary and ROM warning card when merging PRs. ROM warnings are guidance only and do not claim to verify medical safety.
