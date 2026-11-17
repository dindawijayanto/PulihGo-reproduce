package com.pulihgo.app.navigation

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.pulihgo.app.auth.AuthRepository
import com.pulihgo.app.auth.LoginResult
import com.pulihgo.app.auth.SessionStore
import com.pulihgo.app.data.MockDataSource
import com.pulihgo.app.data.exercise.ExerciseCatalog
import com.pulihgo.app.data.session.ExerciseSessionRecord
import com.pulihgo.app.data.session.ExerciseSessionStore
import com.pulihgo.app.data.session.SessionPainLevel
import com.pulihgo.app.data.model.Role
import com.pulihgo.app.data.model.User
import com.pulihgo.app.ui.account.ForgotPasswordScreen
import com.pulihgo.app.ui.account.LoginScreen
import com.pulihgo.app.ui.account.RegisterScreen
import com.pulihgo.app.ui.account.ResetLinkSentScreen
import com.pulihgo.app.ui.account.SettingsScreen
import com.pulihgo.app.ui.home.HomeExerciseSession
import com.pulihgo.app.ui.home.PatientHomeScreen
import com.pulihgo.app.ui.onboarding.OnboardingScreen
import com.pulihgo.app.ui.patient.PatientExerciseLibraryScreen
import com.pulihgo.app.ui.patient.PatientExercisePreparationScreen
import com.pulihgo.app.ui.sensor.LiveExerciseScreen
import com.pulihgo.app.ui.therapist.TherapistHomeScreen
import com.pulihgo.app.ui.therapist.TherapistPatientDetailHandoff
import com.pulihgo.app.ui.therapist.TherapistPatientListLogic
import com.pulihgo.app.ui.therapist.TherapistPatientListScreen
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

private const val PREF_FILE = "pulihgo_local_prefs"
private const val ONBOARDING_SEEN_KEY = "pulihgo_onboarding_seen"

private fun ExerciseSessionRecord.toHomeExerciseSession(): HomeExerciseSession {
    val zdt = Instant.ofEpochMilli(startedAtEpochMs).atZone(ZoneId.systemDefault())
    return HomeExerciseSession(
        date = zdt.toLocalDate(),
        repetitions = repetitions,
        peakRomDegrees = peakRomDeg.toInt(),
        smoothnessPercent = ((averageSmoothness ?: 0.0) * 100).toInt(),
        exerciseName = "Forearm Rotation",
        time = zdt.toLocalTime(),
    )
}

private val BackStackSaver = listSaver<List<AppRoute>, String>(
    save = { stack -> stack.map { it.name } },
    restore = { names -> names.map { AppRoute.valueOf(it) } },
)

/**
 * Combined navigation: Orie's Patient Hi-Fi screens + team role-aware
 * auth, exercise preparation, sensor calibration, and therapist flow.
 */
@Composable
fun AppNavigation(
    isDark: Boolean,
    appearance: String,
    onAppearanceChange: (String) -> Unit,
) {
    val context = LocalContext.current
    val dataSource = remember { MockDataSource(context.applicationContext) }
    val auth = remember { AuthRepository(dataSource.loadUsers()) }
    val sessionStore = remember { SessionStore(context.applicationContext) }
    val exerciseSessionStore = remember { ExerciseSessionStore(context.applicationContext) }
    val prefs = remember { context.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE) }
    val exercises = remember {
        runCatching { ExerciseCatalog(context.applicationContext).loadExercises() }
            .getOrDefault(emptyList())
    }

    var selectedExerciseId by rememberSaveable { mutableStateOf<String?>(null) }
    var lastSavedPainLevel by remember { mutableStateOf<SessionPainLevel?>(null) }
    var draftEmail by rememberSaveable { mutableStateOf("") }
    var selectedTherapistPatientId by rememberSaveable { mutableStateOf<String?>(null) }
    // UI-only exercise data until the sensor/session repository is integrated.
    var demoSessionCompleted by rememberSaveable { mutableStateOf(false) }
    var currentUser by remember { mutableStateOf<User?>(sessionStore.currentUserId?.let(auth::findActiveUser)) }

    var backStack by rememberSaveable(stateSaver = BackStackSaver) {
        val initialRoute = when {
            currentUser != null -> RoleRouter.homeFor(currentUser!!.role)
            !prefs.getBoolean(ONBOARDING_SEEN_KEY, false) -> AppRoute.ONBOARDING
            else -> AppRoute.LOGIN
        }
        mutableStateOf(listOf(initialRoute))
    }

    fun navigate(destination: AppRoute) {
        val resolved = RoleRouter.resolve(destination, currentUser?.role)
        if (resolved != backStack.lastOrNull()) backStack = backStack + resolved
    }
    fun resetTo(destination: AppRoute) {
        backStack = listOf(RoleRouter.resolve(destination, currentUser?.role))
    }
    fun goBack() {
        if (backStack.size > 1) backStack = backStack.dropLast(1)
    }
    fun logout() {
        sessionStore.currentUserId = null
        currentUser = null
        demoSessionCompleted = false
        selectedExerciseId = null
        selectedTherapistPatientId = null
        draftEmail = ""
        resetTo(AppRoute.LOGIN)
    }

    BackHandler(enabled = backStack.size > 1) { goBack() }

    val route = RoleRouter.resolve(backStack.last(), currentUser?.role)
    val user = currentUser

    val storedRecords = remember(user?.id, lastSavedPainLevel, demoSessionCompleted) {
        if (user != null) exerciseSessionStore.read(user.id) else emptyList()
    }
    val homeSessions = remember(storedRecords, demoSessionCompleted) {
        val realMapped = storedRecords.map { it.toHomeExerciseSession() }
        if (demoSessionCompleted && realMapped.isEmpty()) {
            listOf(
                HomeExerciseSession(LocalDate.now(), 4, 62, 84, time = LocalTime.of(7, 45)),
                HomeExerciseSession(LocalDate.now(), 5, 75, 86, time = LocalTime.of(7, 51)),
                HomeExerciseSession(LocalDate.now(), 3, 68, 88, time = LocalTime.of(7, 56)),
            )
        } else {
            realMapped
        }
    }

    // Therapist Patient List rows: only this therapist's patients, with sessions saved
    // on this device. Sessions are stored under the patient's user id (see SENSOR_CHECK).
    // Re-read whenever the back stack changes so returning to the list shows fresh data.
    val therapistPatientItems = remember(user?.id, backStack) {
        val therapist = user
        if (therapist == null || therapist.role != Role.THERAPIST) {
            emptyList()
        } else {
            val assigned = dataSource.loadPatients().filter { it.therapistId == therapist.id }
            TherapistPatientListLogic.buildItems(
                therapistId = therapist.id,
                patients = assigned,
                users = auth.demoAccounts(),
                sessionsByUserId = assigned.associate { it.userId to exerciseSessionStore.read(it.userId) },
            )
        }
    }

    when (route) {
        AppRoute.ONBOARDING -> OnboardingScreen(
            isDark = isDark,
            onDone = {
                prefs.edit().putBoolean(ONBOARDING_SEEN_KEY, true).apply()
                resetTo(AppRoute.LOGIN)
            },
        )

        AppRoute.LOGIN -> LoginScreen(
            prefillEmail = draftEmail,
            isDark = isDark,
            onLogin = { identifier, password ->
                when (val result = auth.login(identifier, password)) {
                    is LoginResult.Failure -> result.message
                    is LoginResult.Success -> {
                        // In-memory demo registrants intentionally don't persist across app restarts.
                        sessionStore.currentUserId =
                            result.user.id.takeUnless { it.startsWith("U_LOCAL_") }
                        currentUser = result.user
                        demoSessionCompleted = false
                        resetTo(RoleRouter.homeFor(result.user.role))
                        null
                    }
                }
            },
            onRegister = { navigate(AppRoute.REGISTER) },
            onForgotPassword = { navigate(AppRoute.FORGOT) },
        )

        AppRoute.REGISTER -> RegisterScreen(
            isDark = isDark,
            onBackToLogin = { goBack() },
            onRegister = { name, email, password ->
                if (auth.registerDemoPatient(name, email, password)) {
                    draftEmail = email.trim()
                    resetTo(AppRoute.LOGIN)
                    null
                } else "This demo email is already registered."
            },
        )

        AppRoute.FORGOT -> ForgotPasswordScreen(
            isDark = isDark,
            onBackToLogin = { goBack() },
            onRequestReset = { email ->
                draftEmail = email.trim()
                navigate(AppRoute.LINK_SENT)
            },
        )

        AppRoute.LINK_SENT -> ResetLinkSentScreen(
            email = draftEmail,
            isDark = isDark,
            onBackToLogin = { resetTo(AppRoute.LOGIN) },
        )

        AppRoute.PATIENT_HOME -> PatientHomeScreen(
            patientName = user?.fullName.orEmpty(),
            sessions = homeSessions,
            onSettings = { navigate(AppRoute.PATIENT_SETTINGS) },
            onStartExercise = { navigate(AppRoute.PATIENT_EXERCISES) },
            onOpenSummary = { navigate(AppRoute.PATIENT_SUMMARY_DEMO) },
            isDark = isDark,
        )

        AppRoute.PATIENT_SETTINGS -> SettingsScreen(
            name = user?.fullName.orEmpty(),
            email = user?.email.orEmpty(),
            selectedAppearance = appearance,
            isDark = isDark,
            onAppearanceChange = onAppearanceChange,
            onBack = { goBack() },
            onLogOut = { logout() },
        )

        AppRoute.PATIENT_EXERCISES -> PatientExerciseLibraryScreen(
            exercises = exercises,
            onSelectExercise = { exercise ->
                selectedExerciseId = exercise.id
                lastSavedPainLevel = null
                navigate(AppRoute.PATIENT_EXERCISE_PREPARATION)
            },
            onBack = { goBack() },
            onHome = { resetTo(AppRoute.PATIENT_HOME) },
            onSummary = { navigate(AppRoute.PATIENT_SUMMARY_DEMO) },
            onSettings = { navigate(AppRoute.PATIENT_SETTINGS) },
            isDark = isDark,
        )

        AppRoute.PATIENT_EXERCISE_PREPARATION -> {
            val exercise = exercises.firstOrNull { it.id == selectedExerciseId }
            if (exercise != null) {
                PatientExercisePreparationScreen(
                    exercise = exercise,
                    onBack = { goBack() },
                    onContinueToCalibration = {
                        lastSavedPainLevel = null
                        navigate(AppRoute.SENSOR_CHECK)
                    },
                    onSettings = { navigate(AppRoute.PATIENT_SETTINGS) },
                    isDark = isDark,
                    savedPainLevel = lastSavedPainLevel,
                )
            } else {
                PatientExerciseLibraryScreen(
                    exercises = exercises,
                    onSelectExercise = { selected ->
                        selectedExerciseId = selected.id
                        navigate(AppRoute.PATIENT_EXERCISE_PREPARATION)
                    },
                    onBack = { goBack() },
                    onHome = { resetTo(AppRoute.PATIENT_HOME) },
                    onSummary = { navigate(AppRoute.PATIENT_SUMMARY_DEMO) },
                    onSettings = { navigate(AppRoute.PATIENT_SETTINGS) },
                    isDark = isDark,
                )
            }
        }

        AppRoute.SENSOR_CHECK -> {
            val exercise = exercises.firstOrNull { it.id == selectedExerciseId }
            if (exercise != null && user != null) {
                LiveExerciseScreen(
                    exercise = exercise,
                    patientId = user.id,
                    isDark = isDark,
                    onBack = { goBack() },
                    onSettings = {
                        // Leave the unsaved live screen before opening Settings.
                        goBack()
                        navigate(AppRoute.PATIENT_SETTINGS)
                    },
                    onSessionCompleted = { record ->
                        val stored = exerciseSessionStore.append(record)
                        if (stored) {
                            lastSavedPainLevel = record.painLevel
                            goBack() // return to Preparation with Figma saved status banner
                        }
                        stored
                    },
                )
            } else {
                // No fabricated exercise and no recording under a different patient.
                PatientExerciseLibraryScreen(
                    exercises = exercises,
                    onSelectExercise = { selected ->
                        selectedExerciseId = selected.id
                        navigate(AppRoute.PATIENT_EXERCISE_PREPARATION)
                    },
                    onBack = { goBack() },
                    onHome = { resetTo(AppRoute.PATIENT_HOME) },
                    onSummary = { navigate(AppRoute.PATIENT_SUMMARY_DEMO) },
                    onSettings = { navigate(AppRoute.PATIENT_SETTINGS) },
                    isDark = isDark,
                )
            }
        }

        AppRoute.PATIENT_SUMMARY_DEMO -> SummaryDemoHandoff(
            hasDemoSessions = demoSessionCompleted,
            onBack = { goBack() },
            onSimulate = {
                demoSessionCompleted = true
                resetTo(AppRoute.PATIENT_HOME)
            },
            onReset = {
                demoSessionCompleted = false
                resetTo(AppRoute.PATIENT_HOME)
            },
        )

        AppRoute.THERAPIST_HOME -> TherapistHomeScreen(
            therapist = user,
            patients = remember(user?.id) {
                dataSource.loadPatients().filter { it.therapistId == user?.id }
            },
            users = auth.demoAccounts(),
            onLogout = { logout() },
            onOpenPatients = { navigate(AppRoute.THERAPIST_PATIENTS) },
        )

        // PGF-THR-03: Patient List & Search.
        AppRoute.THERAPIST_PATIENTS -> TherapistPatientListScreen(
            therapistName = user?.fullName.orEmpty(),
            items = therapistPatientItems,
            isDark = isDark,
            onOpenDashboard = { resetTo(AppRoute.THERAPIST_HOME) },
            onViewPatient = { patientId ->
                selectedTherapistPatientId = patientId
                navigate(AppRoute.THERAPIST_PATIENT_DETAIL)
            },
        )

        // Temporary handoff until PGF-THR-04 (Patient Detail) lands.
        AppRoute.THERAPIST_PATIENT_DETAIL -> TherapistPatientDetailHandoff(
            patient = therapistPatientItems.firstOrNull { it.patientId == selectedTherapistPatientId },
            onBack = { goBack() },
        )
    }
}

/** Temporary summary handoff; never labels simulated data as real sensor readings. */
@Composable
private fun SummaryDemoHandoff(
    hasDemoSessions: Boolean,
    onBack: () -> Unit,
    onSimulate: () -> Unit,
    onReset: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier.safeDrawingPadding().padding(24.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text("Session Summary — UI Demo", style = MaterialTheme.typography.headlineMedium)
            Text(
                "No real session records are stored yet. Exercise measurements and the final summary are managed by the team's upcoming integration.",
                modifier = Modifier.padding(vertical = 20.dp),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(if (hasDemoSessions) "Synthetic demo sessions: ON" else "Synthetic demo sessions: OFF")
            Button(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                onClick = onSimulate,
            ) { Text("Simulate Completed Sessions") }
            OutlinedButton(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                onClick = onReset,
            ) { Text("Clear Demo Sessions") }
            OutlinedButton(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                onClick = onBack,
            ) { Text("Back to Patient Home") }
        }
    }
}
