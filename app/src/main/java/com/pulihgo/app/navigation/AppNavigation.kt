package com.pulihgo.app.navigation

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.pulihgo.app.auth.AuthRepository
import com.pulihgo.app.auth.LoginResult
import com.pulihgo.app.auth.SessionStore
import com.pulihgo.app.data.MockDataSource
import com.pulihgo.app.data.exercise.ExerciseCatalog
import com.pulihgo.app.data.session.ExerciseSessionRecord
import com.pulihgo.app.data.session.ExerciseSessionStore
import com.pulihgo.app.data.session.PatientSessionHistory
import com.pulihgo.app.data.session.SeededSessionSource
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
import com.pulihgo.app.ui.summary.PatientSessionSummaryScreen
import com.pulihgo.app.ui.summary.SessionHistoryLogic
import com.pulihgo.app.ui.therapist.ExerciseTarget
import com.pulihgo.app.ui.therapist.TherapistDashboardLogic
import com.pulihgo.app.ui.therapist.TherapistDashboardScreen
import com.pulihgo.app.ui.therapist.TherapistPatientDetailHandoff
import com.pulihgo.app.ui.therapist.TherapistPatientListLogic
import com.pulihgo.app.ui.therapist.TherapistPatientListScreen
import com.pulihgo.app.ui.therapist.TherapistTab
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

private const val PREF_FILE = "pulihgo_local_prefs"
private const val ONBOARDING_SEEN_KEY = "pulihgo_onboarding_seen"

private fun ExerciseSessionRecord.toHomeExerciseSession(): HomeExerciseSession {
    val zdt = Instant.ofEpochMilli(startedAtEpochMs).atZone(ZoneId.systemDefault())
    return HomeExerciseSession(
        date = zdt.toLocalDate(),
        repetitions = repetitions,
        peakRomDegrees = peakRomDeg.toInt(),
        smoothnessPercent = averageSmoothness?.let { (it * 100).toInt() },
        exerciseName = "Forearm Rotation",
        time = zdt.toLocalTime(),
        painLevel = painLevel,
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
    val allPatients = remember { dataSource.loadPatients() }
    // Seeded demo history (assets/mock_data/sessions.json), re-keyed to patient user ids.
    val seededSessions = remember {
        SeededSessionSource(context.applicationContext).load(allPatients.associate { it.id to it.userId })
    }
    val exerciseTargets = remember(exercises) {
        exercises.associate { it.id to ExerciseTarget(it.name, it.targetRomDeg, it.romCeilingDeg) }
    }
    var therapistRefreshTick by remember { mutableStateOf(0) }

    var selectedExerciseId by rememberSaveable { mutableStateOf<String?>(null) }
    var lastSavedPainLevel by remember { mutableStateOf<SessionPainLevel?>(null) }
    var draftEmail by rememberSaveable { mutableStateOf("") }
    var selectedTherapistPatientId by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedSummarySessionId by rememberSaveable { mutableStateOf<String?>(null) }
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
        selectedExerciseId = null
        selectedTherapistPatientId = null
        selectedSummarySessionId = null
        draftEmail = ""
        resetTo(AppRoute.LOGIN)
    }

    BackHandler(enabled = backStack.size > 1) { goBack() }

    val route = RoleRouter.resolve(backStack.last(), currentUser?.role)
    val user = currentUser

    val storedRecords = remember(user?.id, lastSavedPainLevel) {
        if (user != null) exerciseSessionStore.read(user.id) else emptyList()
    }
    // PGF-PRO-03: the patient's own sessions (seeded history + this device), newest first.
    val patientSessions = remember(user?.id, storedRecords) {
        val patientUser = user
        if (patientUser == null || patientUser.role != Role.PATIENT) emptyList()
        else PatientSessionHistory.merge(seededSessions, storedRecords, patientUser.id)
    }
    val homeSessions = remember(patientSessions) { patientSessions.map { it.record.toHomeExerciseSession() } }

    // Therapist portal data: only this therapist's patients, with seeded history plus
    // sessions saved on this device (stored under the patient's user id, see SENSOR_CHECK).
    // Re-read whenever the back stack changes or Refresh is tapped.
    val therapistCohort = remember(user?.id, backStack, therapistRefreshTick) {
        val therapist = user
        if (therapist == null || therapist.role != Role.THERAPIST) {
            emptyList()
        } else {
            val assigned = allPatients.filter { it.therapistId == therapist.id }
            TherapistDashboardLogic.assemble(
                therapistId = therapist.id,
                patients = assigned,
                users = auth.demoAccounts(),
                sessionsByUserId = assigned.associate {
                    it.userId to PatientSessionHistory.merge(seededSessions, exerciseSessionStore.read(it.userId), it.userId)
                },
            )
        }
    }
    val therapistPatientItems = remember(therapistCohort) {
        val therapist = user
        if (therapist == null) emptyList() else TherapistPatientListLogic.buildItems(
            therapistId = therapist.id,
            patients = therapistCohort.map { it.patient },
            users = auth.demoAccounts(),
            sessionsByUserId = therapistCohort.associate { p -> p.patient.userId to p.sessions.map { it.record } },
        )
    }

    /** Summary tab always opens on the newest session. */
    fun openPatientSummary() {
        selectedSummarySessionId = null
        navigate(AppRoute.PATIENT_SUMMARY)
    }
    fun openTherapistTab(tab: TherapistTab) {
        resetTo(AppRoute.THERAPIST_HOME)
        when (tab) {
            TherapistTab.THERAPY -> navigate(AppRoute.THERAPIST_PATIENTS)
            else -> Unit
        }
    }
    fun openTherapistPatient(patientId: String) {
        selectedTherapistPatientId = patientId
        navigate(AppRoute.THERAPIST_PATIENT_DETAIL)
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
            onOpenSummary = { openPatientSummary() },
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
            onSummary = { openPatientSummary() },
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
                    onSummary = { openPatientSummary() },
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
                    onSummary = { openPatientSummary() },
                    onSettings = { navigate(AppRoute.PATIENT_SETTINGS) },
                    isDark = isDark,
                )
            }
        }

        // PGF-PRO-02 Session Summary + PGF-PRO-03 Progress & Session History.
        AppRoute.PATIENT_SUMMARY -> {
            val shown = SessionHistoryLogic.selected(patientSessions, selectedSummarySessionId)
            val exercise = exercises.firstOrNull { it.id == shown?.record?.exerciseId }
            PatientSessionSummaryScreen(
                session = shown,
                exerciseName = exercise?.name,
                romCeilingDeg = exercise?.romCeilingDeg,
                isDark = isDark,
                onHome = { resetTo(AppRoute.PATIENT_HOME) },
                onExercise = { navigate(AppRoute.PATIENT_EXERCISES) },
                onSettings = { navigate(AppRoute.PATIENT_SETTINGS) },
                history = patientSessions,
                onSelectSession = { selectedSummarySessionId = it },
            )
        }

        // PGF-THR-02: Therapist Dashboard is the therapist's home.
        AppRoute.THERAPIST_HOME -> TherapistDashboardScreen(
            therapistName = user?.fullName.orEmpty(),
            summary = remember(therapistCohort, exerciseTargets) {
                TherapistDashboardLogic.summarize(therapistCohort, exerciseTargets, LocalDate.now())
            },
            isDark = isDark,
            today = LocalDate.now(),
            onNavigate = ::openTherapistTab,
            onLogout = { logout() },
            onRefresh = { therapistRefreshTick++ },
            onViewPatient = ::openTherapistPatient,
        )

        // PGF-THR-03: Patient List & Search.
        AppRoute.THERAPIST_PATIENTS -> TherapistPatientListScreen(
            therapistName = user?.fullName.orEmpty(),
            items = therapistPatientItems,
            isDark = isDark,
            onNavigate = ::openTherapistTab,
            onLogout = { logout() },
            onViewPatient = ::openTherapistPatient,
        )

        // Temporary handoff until PGF-THR-04 (Patient Detail) lands.
        AppRoute.THERAPIST_PATIENT_DETAIL -> TherapistPatientDetailHandoff(
            patient = therapistPatientItems.firstOrNull { it.patientId == selectedTherapistPatientId },
            onBack = { goBack() },
        )
    }
}
