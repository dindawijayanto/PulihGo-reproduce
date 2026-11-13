package com.pulihgo.app.navigation

import com.pulihgo.app.data.model.Role

enum class AppRoute(val allowedRoles: Set<Role>) {
    LOGIN(emptySet()),

    PATIENT_HOME(setOf(Role.PATIENT)),
    PATIENT_EXERCISES(setOf(Role.PATIENT)),
    PATIENT_EXERCISE_PREPARATION(setOf(Role.PATIENT)),
    SENSOR_CHECK(setOf(Role.PATIENT)),

    THERAPIST_HOME(setOf(Role.THERAPIST));

    val isPublic: Boolean get() = allowedRoles.isEmpty()
}

object RoleRouter {
    fun homeFor(role: Role): AppRoute = when (role) {
        Role.PATIENT -> AppRoute.PATIENT_HOME
        Role.THERAPIST -> AppRoute.THERAPIST_HOME
    }

    fun canAccess(route: AppRoute, role: Role?): Boolean =
        route.isPublic || (role != null && role in route.allowedRoles)

    fun resolve(requested: AppRoute, role: Role?): AppRoute = when {
        role == null -> if (requested.isPublic) requested else AppRoute.LOGIN
        requested == AppRoute.LOGIN -> homeFor(role)
        canAccess(requested, role) -> requested
        else -> homeFor(role)
    }
}
