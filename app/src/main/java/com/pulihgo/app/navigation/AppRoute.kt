package com.pulihgo.app.navigation

import com.pulihgo.app.data.model.Role

/**
 * Daftar route aplikasi beserta role yang boleh membukanya.
 * `allowedRoles` kosong berarti route publik (bisa dibuka sebelum login).
 *
 * Saat modul tim lain siap (Onboarding, Exercise, Summary, Therapist Portal, dan
 * lainnya), tambahkan route-nya di sini dengan role yang sesuai. Router akan
 * otomatis menjaga aksesnya.
 */
enum class AppRoute(val allowedRoles: Set<Role>) {
    LOGIN(emptySet()),

    PATIENT_HOME(setOf(Role.PATIENT)),
    SENSOR_CHECK(setOf(Role.PATIENT)), // PGF-SEN-01 / PGF-SEN-02

    THERAPIST_HOME(setOf(Role.THERAPIST));

    val isPublic: Boolean get() = allowedRoles.isEmpty()
}

/** Aturan redirect murni tanpa UI, supaya bisa diuji dengan unit test. */
object RoleRouter {

    /** Halaman utama untuk setiap role, tujuan setelah login berhasil. */
    fun homeFor(role: Role): AppRoute = when (role) {
        Role.PATIENT -> AppRoute.PATIENT_HOME
        Role.THERAPIST -> AppRoute.THERAPIST_HOME
    }

    fun canAccess(route: AppRoute, role: Role?): Boolean =
        route.isPublic || (role != null && role in route.allowedRoles)

    /**
     * Route yang benar-benar dibuka saat aplikasi diminta ke [requested]:
     * - belum login dan route butuh role, diarahkan ke LOGIN
     * - sudah login tapi membuka LOGIN, diarahkan ke Home sesuai role
     * - sudah login tapi role tidak berhak, diarahkan ke Home sesuai role
     */
    fun resolve(requested: AppRoute, role: Role?): AppRoute = when {
        role == null -> if (requested.isPublic) requested else AppRoute.LOGIN
        requested == AppRoute.LOGIN -> homeFor(role)
        canAccess(requested, role) -> requested
        else -> homeFor(role)
    }
}
