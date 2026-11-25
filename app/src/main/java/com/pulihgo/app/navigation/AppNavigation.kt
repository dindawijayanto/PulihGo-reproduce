package com.pulihgo.app.navigation

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.pulihgo.app.BuildConfig
import com.pulihgo.app.auth.AuthRepository
import com.pulihgo.app.auth.LoginResult
import com.pulihgo.app.auth.PasswordChangeResult
import com.pulihgo.app.auth.SessionStore
import com.pulihgo.app.auth.TherapistProfileStore
import com.pulihgo.app.data.MockDataSource
import com.pulihgo.app.data.cloud.CloudSyncResult
import com.pulihgo.app.data.cloud.SessionCloudSync
import com.pulihgo.app.data.cloud.SupabaseConfig
import com.pulihgo.app.data.exercise.ExerciseCatalog
import com.pulihgo.app.data.exercise.Exercise
import com.pulihgo.app.data.therapy.PatientTherapyPlan
import com.pulihgo.app.data.therapy.PatientTherapyPlanCloud
import com.pulihgo.app.data.therapy.PatientTherapyPlanStore
import com.pulihgo.app.data.session.ExerciseSessionRecord
import com.pulihgo.app.data.session.ExerciseSessionStore
import com.pulihgo.app.data.session.PatientSessionRepository
import com.pulihgo.app.data.session.SeededSessionSource
import com.pulihgo.app.data.session.SessionPainLevel
import com.pulihgo.app.data.session.TherapistSessionLookup
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
import com.pulihgo.app.ui.therapist.TherapistForgotPasswordScreen
import com.pulihgo.app.ui.therapist.TherapistLoginScreen
import com.pulihgo.app.ui.therapist.TherapistPatientDetailHandoff
import com.pulihgo.app.ui.therapist.TherapistPatientListLogic
import com.pulihgo.app.ui.therapist.TherapistPatientListScreen
import com.pulihgo.app.ui.therapist.TherapistReportsScreen
import com.pulihgo.app.ui.therapist.TherapistSessionDetailLogic
import com.pulihgo.app.ui.therapist.TherapistSessionDetailScreen
import com.pulihgo.app.ui.therapist.TherapistSettingsLogic
import com.pulihgo.app.ui.therapist.TherapistSettingsScreen
import com.pulihgo.app.ui.therapist.TherapistTab
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.launch

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
    val therapistProfileStore = remember { TherapistProfileStore(context.applicationContext) }
    var therapistProfileVersion by remember { mutableStateOf(0) }
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
    val sessionRepository = remember(allPatients, seededSessions) {
        PatientSessionRepository(allPatients, seededSessions, exerciseSessionStore::read)
    }
    val cloudSync = remember(allPatients) {
        SessionCloudSync(
            context = context.applicationContext,
            config = SupabaseConfig(BuildConfig.SUPABASE_URL, BuildConfig.SUPABASE_KEY),
            patientIdForUser = { userId -> allPatients.firstOrNull { it.userId == userId }?.id },
            readDevice = exerciseSessionStore::read,
        )
    }
    val therapyStore = remember { PatientTherapyPlanStore(context.applicationContext) }
    val therapyCloud = remember { PatientTherapyPlanCloud(SupabaseConfig(BuildConfig.SUPABASE_URL, BuildConfig.SUPABASE_KEY)) }
    val scope = rememberCoroutineScope()
    fun syncToCloud(userId: String, announce: Boolean) {
        scope.launch {
            val message = when (val result = cloudSync.syncPending(userId)) {
                is CloudSyncResult.Synced -> "Session synced to the cloud."
                is CloudSyncResult.Offline -> "Saved on this phone. It will sync when you're online."
                is CloudSyncResult.Failed -> "Saved on this phone. Cloud sync failed (${result.reason})."
                CloudSyncResult.NotConnected -> "Saved on this phone. Log in again to sync to the cloud."
                else -> null
            }
            if (announce && message != null) Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }
    val exerciseTargets = remember(exercises) {
        exercises.associate { it.id to ExerciseTarget(it.name, it.targetRomDeg, it.romCeilingDeg, it.targetReps) }
    }
    var therapistRefreshTick by remember { mutableStateOf(0) }

    var selectedExerciseId by rememberSaveable { mutableStateOf<String?>(null) }
    var lastSavedPainLevel by remember { mutableStateOf<SessionPainLevel?>(null) }
    var draftEmail by rememberSaveable { mutableStateOf("") }
    var selectedTherapistPatientId by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedSummarySessionId by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedReportsPatientId by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedTherapistSessionId by rememberSaveable { mutableStateOf<String?>(null) }
    var currentUser by remember { mutableStateOf<User?>(sessionStore.currentUserId?.let(auth::findActiveUser)) }
    // INT-02: never share cached or in-flight plan data across patient identities.
    var plansForUser by remember { mutableStateOf<List<PatientTherapyPlan>>(emptyList()) }
    var planLoadedScope by remember { mutableStateOf<String?>(null) }
    var planSource by remember { mutableStateOf("offline") }
    var planVerified by remember { mutableStateOf(false) }
    var planChecking by remember { mutableStateOf(false) }
    var planRefreshTick by remember { mutableStateOf(0) }
    // Snapshot the exact parameters at Calibrate & Start, not after the therapist edits them.
    var lockedSessionExercise by remember { mutableStateOf<Exercise?>(null) }

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
        val loginRoute = RoleRouter.loginFor(currentUser?.role)
        currentUser?.let { cloudSync.disconnect(it.id) }
        sessionStore.currentUserId = null
        currentUser = null
        plansForUser = emptyList()
        planLoadedScope = null
        planVerified = false
        lockedSessionExercise = null
        selectedExerciseId = null
        selectedTherapistPatientId = null
        selectedSummarySessionId = null
        selectedReportsPatientId = null
        selectedTherapistSessionId = null
        draftEmail = ""
        resetTo(loginRoute)
    }
    fun completeLogin(loggedIn: User) {
        sessionStore.currentUserId = loggedIn.id.takeUnless { it.startsWith("U_LOCAL_") }
        currentUser = loggedIn
        plansForUser = emptyList()
        planLoadedScope = null
        planVerified = false
        lockedSessionExercise = null
        resetTo(RoleRouter.homeFor(loggedIn.role))
    }

    BackHandler(enabled = backStack.size > 1) { goBack() }

    LaunchedEffect(currentUser?.id) {
        val patient = currentUser?.takeIf { it.role == Role.PATIENT }
        if (patient != null) syncToCloud(patient.id, announce = false)
    }

    val route = RoleRouter.resolve(backStack.last(), currentUser?.role)
    val user = currentUser
    val patientPlanUser = user?.takeIf { it.role == Role.PATIENT }
    val cloudPatient = allPatients.firstOrNull { it.userId == patientPlanUser?.id }
    val therapyScope = "${patientPlanUser?.id}:${cloudPatient?.id}"
    val visiblePlans = if (planLoadedScope == therapyScope) plansForUser else emptyList()
    val effectiveExercises = exercises.map { catalog ->
        visiblePlans.firstOrNull { it.exerciseId == catalog.id }?.applyTo(catalog) ?: catalog
    }
    val planStillLoading = planLoadedScope != therapyScope || planChecking
    val planSyncLabel = when {
        planStillLoading -> "Checking latest Therapy Plan..."
        !planVerified -> "Plan not verified. Connect to Supabase and refresh before starting."
        planSource == "cloud" -> "Therapist plan checked with Supabase"
        planSource == "cache" -> "Offline: using last saved therapist plan"
        else -> "Offline or no cloud account: using available catalog defaults"
    }

    // Refresh on library entry AND before each new session (Preparation). Never refetch mid-session.
    // A failure retains the last valid cache; a successful empty response clears it.
    LaunchedEffect(route, patientPlanUser?.id, selectedExerciseId, planRefreshTick) {
        if (route != AppRoute.PATIENT_EXERCISES && route != AppRoute.PATIENT_EXERCISE_PREPARATION) return@LaunchedEffect
        val patient = patientPlanUser ?: return@LaunchedEffect
        val patientId = cloudPatient?.id
        planLoadedScope = therapyScope
        planChecking = true
        plansForUser = if (patientId != null) therapyStore.read(patient.id, patientId) else emptyList()
        planSource = if (plansForUser.isNotEmpty()) "cache" else "defaults"
        planVerified = if (patientId != null) therapyStore.hasSnapshot(patient.id, patientId)
            else true // Local-only demo account: no matching cloud patient exists.
        if (!SupabaseConfig(BuildConfig.SUPABASE_URL, BuildConfig.SUPABASE_KEY).isConfigured) planVerified = true
        if (patientId != null) {
            val accessToken = cloudSync.refreshAccessTokenForPlan(patient.id)
            if (accessToken != null) {
                val remote = runCatching { therapyCloud.readForPatient(patientId, accessToken) }.getOrNull()
                if (remote != null) {
                    therapyStore.replace(patient.id, patientId, remote)
                    plansForUser = remote
                    planSource = "cloud"
                    planVerified = true
                }
            }
        }
        planChecking = false
    }

    val therapistUser = user?.takeIf { it.role == Role.THERAPIST }
    val therapistProfile = remember(therapistUser?.id, therapistProfileVersion) {
        therapistUser?.let(therapistProfileStore::profile)
    }
    val therapistName = therapistProfile?.fullName ?: user?.fullName.orEmpty()

    // PGF-PRO-03: the patient's own sessions (seeded history + this device), newest first.
    val patientSessions = remember(user?.id, lastSavedPainLevel) {
        val patientUser = user
        if (patientUser == null || patientUser.role != Role.PATIENT) emptyList()
        else sessionRepository.sessionsForUser(patientUser.id)
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
            TherapistDashboardLogic.assemble(
                therapistId = therapist.id,
                patients = sessionRepository.patientsOf(therapist.id),
                users = auth.demoAccounts(),
                sessionsByUserId = sessionRepository.cohortSessions(therapist.id),
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
            TherapistTab.REPORTS -> navigate(AppRoute.THERAPIST_REPORTS)
            TherapistTab.SETTINGS -> navigate(AppRoute.THERAPIST_SETTINGS)
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
                        completeLogin(result.user)
                        if (result.user.role == Role.PATIENT) {
                            scope.launch {
                                if (cloudSync.connect(result.user, password)) syncToCloud(result.user.id, announce = false)
                            }
                        }
                        null
                    }
                }
            },
            onRegister = { navigate(AppRoute.REGISTER) },
            onForgotPassword = { navigate(AppRoute.FORGOT) },
            onTherapistLogin = { navigate(AppRoute.THERAPIST_LOGIN) },
        )

        AppRoute.THERAPIST_LOGIN -> TherapistLoginScreen(
            isDark = isDark,
            onLogin = { identifier, password ->
                when (val result = auth.loginTherapist(identifier, password)) {
                    is LoginResult.Failure -> result.message
                    is LoginResult.Success -> {
                        completeLogin(result.user)
                        null
                    }
                }
            },
            onForgotPassword = { navigate(AppRoute.THERAPIST_FORGOT) },
            onPatientLogin = { if (backStack.size > 1) goBack() else resetTo(AppRoute.LOGIN) },
        )

        AppRoute.THERAPIST_FORGOT -> TherapistForgotPasswordScreen(
            isDark = isDark,
            onBackToLogin = { goBack() },
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
            exercises = effectiveExercises,
            planStatus = planSyncLabel,
            planLoading = planStillLoading,
            onSelectExercise = { exercise ->
                selectedExerciseId = exercise.id
                lockedSessionExercise = null
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
            val exercise = effectiveExercises.firstOrNull { it.id == selectedExerciseId }
            val prescription = visiblePlans.firstOrNull { it.exerciseId == selectedExerciseId }
            val paused = prescription?.isActive == false
            if (exercise != null) {
                PatientExercisePreparationScreen(
                    exercise = exercise,
                    planStatus = planSyncLabel,
                    planDetails = "Target ${exercise.targetRomDeg.toInt()}° • Ceiling ${exercise.romCeilingDeg.toInt()}° • ${exercise.targetReps} reps" +
                        (prescription?.let { " • ${it.setsPerSession} sets • ${it.sessionsPerWeek} days/week" } ?: ""),
                    planNotes = prescription?.notes.orEmpty(),
                    isPlanLoading = planStillLoading,
                    isPlanReady = planVerified,
                    isPlanPaused = paused,
                    onRefreshPlan = { planRefreshTick += 1 },
                    onBack = { goBack() },
                    onContinueToCalibration = {
                        if (!planStillLoading && planVerified && !paused &&
                            exercise.targetRomDeg > 0.0 && exercise.romCeilingDeg > exercise.targetRomDeg
                        ) {
                            lockedSessionExercise = exercise.copy()
                            lastSavedPainLevel = null
                            navigate(AppRoute.SENSOR_CHECK)
                        }
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
            // Do not re-resolve from an updated plan while an exercise is in progress.
            val exercise = lockedSessionExercise?.takeIf { it.id == selectedExerciseId }
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
                            syncToCloud(record.patientId, announce = true)
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
            therapistName = therapistName,
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
            therapistName = therapistName,
            items = therapistPatientItems,
            isDark = isDark,
            onNavigate = ::openTherapistTab,
            onLogout = { logout() },
            onViewPatient = ::openTherapistPatient,
        )

        // PGF-THR-06: Reports UI for one selected patient.
        AppRoute.THERAPIST_REPORTS -> TherapistReportsScreen(
            therapistName = therapistName,
            patients = therapistCohort,
            selectedPatientId = selectedReportsPatientId,
            exercises = exerciseTargets,
            isDark = isDark,
            today = LocalDate.now(),
            onSelectPatient = { selectedReportsPatientId = it },
            onNavigate = ::openTherapistTab,
            onLogout = { logout() },
            onRefresh = { therapistRefreshTick++ },
            onOpenSession = { patientId, sessionId ->
                selectedReportsPatientId = patientId
                selectedTherapistSessionId = sessionId
                navigate(AppRoute.THERAPIST_SESSION_DETAIL)
            },
        )

        AppRoute.THERAPIST_SESSION_DETAIL -> {
            val therapist = therapistUser
            val patientId = selectedReportsPatientId
            val sessionId = selectedTherapistSessionId
            val detail = remember(therapist?.id, patientId, sessionId, therapistCohort) {
                if (therapist == null || patientId == null || sessionId == null) null
                else when (val lookup = sessionRepository.sessionForTherapist(therapist.id, patientId, sessionId)) {
                    is TherapistSessionLookup.Found -> TherapistSessionDetailLogic.build(
                        found = lookup,
                        patientName = therapistCohort.firstOrNull { it.patient.id == patientId }?.name ?: patientId,
                        exercises = exerciseTargets,
                    )
                    else -> null
                }
            }
            TherapistSessionDetailScreen(
                therapistName = therapistName,
                detail = detail,
                isDark = isDark,
                onBack = { goBack() },
                onNavigate = ::openTherapistTab,
                onLogout = { logout() },
            )
        }

        AppRoute.THERAPIST_SETTINGS -> {
            val therapist = therapistUser
            val profile = therapistProfile
            if (therapist != null && profile != null) {
                TherapistSettingsScreen(
                    user = therapist,
                    profile = profile,
                    preferences = remember(therapist.id, therapistProfileVersion) {
                        therapistProfileStore.preferences(therapist.id)
                    },
                    assignedPatientCount = therapistCohort.size,
                    isDark = isDark,
                    today = LocalDate.now(),
                    onSaveProfile = { edited ->
                        TherapistSettingsLogic.validate(edited) ?: run {
                            therapistProfileStore.saveProfile(therapist.id, TherapistSettingsLogic.normalize(edited))
                            therapistProfileVersion++
                            null
                        }
                    },
                    onPreferencesChange = {
                        therapistProfileStore.savePreferences(therapist.id, it)
                        therapistProfileVersion++
                    },
                    onDarkModeChange = { onAppearanceChange(if (it) "Dark" else "Light") },
                    onChangePassword = { current, new, confirmation ->
                        when (val result = auth.changePassword(therapist.id, current, new, confirmation)) {
                            PasswordChangeResult.Success -> null
                            is PasswordChangeResult.Failure -> result.message
                        }
                    },
                    onViewCaseload = { openTherapistTab(TherapistTab.THERAPY) },
                    onNavigate = ::openTherapistTab,
                    onLogout = { logout() },
                )
            }
        }

        // Temporary handoff until PGF-THR-04 (Patient Detail) lands.
        AppRoute.THERAPIST_PATIENT_DETAIL -> TherapistPatientDetailHandoff(
            patient = therapistPatientItems.firstOrNull { it.patientId == selectedTherapistPatientId },
            onBack = { goBack() },
        )
    }
}
