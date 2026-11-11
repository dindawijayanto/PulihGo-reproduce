package com.pulihgo.app.navigation

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
import com.pulihgo.app.data.model.User
import com.pulihgo.app.ui.auth.RoleLoginScreen
import com.pulihgo.app.ui.patient.PatientHomePlaceholder
import com.pulihgo.app.ui.sensor.SensorCheckScreen
import com.pulihgo.app.ui.therapist.TherapistHomeScreen

private val BackStackSaver = listSaver<List<AppRoute>, String>(
    save = { stack -> stack.map { it.name } },
    restore = { names -> names.map { AppRoute.valueOf(it) } },
)

/**
 * Root navigasi aplikasi (PGF-ALL-04).
 *
 * Setiap perpindahan layar melewati [RoleRouter.resolve], jadi tidak ada layar
 * yang bisa dibuka oleh role yang salah, termasuk jika dipanggil dari tombol yang keliru.
 */
@Composable
fun AppNavigation() {
    val context = LocalContext.current
    val dataSource = remember { MockDataSource(context.applicationContext) }
    val auth = remember { AuthRepository(dataSource.loadUsers()) }
    val sessionStore = remember { SessionStore(context.applicationContext) }

    var currentUser by remember {
        mutableStateOf<User?>(sessionStore.currentUserId?.let(auth::findActiveUser))
    }
    var backStack by rememberSaveable(stateSaver = BackStackSaver) {
        mutableStateOf(listOf(RoleRouter.resolve(AppRoute.LOGIN, currentUser?.role)))
    }

    fun navigate(route: AppRoute) {
        backStack = backStack + RoleRouter.resolve(route, currentUser?.role)
    }

    fun resetTo(route: AppRoute) {
        backStack = listOf(RoleRouter.resolve(route, currentUser?.role))
    }

    fun logout() {
        sessionStore.currentUserId = null
        currentUser = null
        resetTo(AppRoute.LOGIN)
    }

    BackHandler(enabled = backStack.size > 1) { backStack = backStack.dropLast(1) }

    // Jaga-jaga: jika state dipulihkan dengan route yang tidak sesuai role, arahkan ulang.
    val route = RoleRouter.resolve(backStack.last(), currentUser?.role)
    val user = currentUser

    when (route) {
        AppRoute.LOGIN -> RoleLoginScreen(
            demoAccounts = auth.demoAccounts(),
            onLogin = { identifier, password ->
                when (val result = auth.login(identifier, password)) {
                    is LoginResult.Failure -> result.message
                    is LoginResult.Success -> {
                        sessionStore.currentUserId = result.user.id
                        currentUser = result.user
                        // Login mengganti seluruh back stack, jadi tombol Back tidak kembali ke Login.
                        resetTo(RoleRouter.homeFor(result.user.role))
                        null
                    }
                }
            },
        )

        AppRoute.PATIENT_HOME -> PatientHomePlaceholder(
            patientName = user?.fullName.orEmpty(),
            onOpenSensorCheck = { navigate(AppRoute.SENSOR_CHECK) },
            onLogout = ::logout,
        )

        AppRoute.SENSOR_CHECK -> SensorCheckScreen(onBack = { backStack = backStack.dropLast(1) })

        AppRoute.THERAPIST_HOME -> TherapistHomeScreen(
            therapist = user,
            patients = remember(user?.id) { dataSource.loadPatients().filter { it.therapistId == user?.id } },
            users = auth.demoAccounts(),
            onLogout = ::logout,
        )
    }
}
