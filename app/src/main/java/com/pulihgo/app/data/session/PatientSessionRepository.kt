package com.pulihgo.app.data.session

import com.pulihgo.app.data.model.Patient

sealed interface TherapistSessionLookup {
    data class Found(val patient: Patient, val session: PatientSession) : TherapistSessionLookup
    data object PatientNotAssigned : TherapistSessionLookup
    data object SessionNotFound : TherapistSessionLookup
}

class PatientSessionRepository(
    private val patients: List<Patient>,
    private val seeded: List<PatientSession>,
    private val readDevice: (userId: String) -> List<ExerciseSessionRecord>,
) {
    fun patient(patientId: String): Patient? = patients.firstOrNull { it.id == patientId }

    fun patientForUser(userId: String): Patient? = patients.firstOrNull { it.userId == userId }

    fun sessionsForUser(userId: String): List<PatientSession> =
        PatientSessionHistory.merge(seeded, readDevice(userId), userId)

    fun sessionsForPatient(patientId: String): List<PatientSession> =
        patient(patientId)?.let { sessionsForUser(it.userId) }.orEmpty()

    fun session(patientId: String, sessionId: String): PatientSession? =
        sessionsForPatient(patientId).firstOrNull { it.id == sessionId }

    fun patientsOf(therapistId: String): List<Patient> = patients.filter { it.therapistId == therapistId }

    fun cohortSessions(therapistId: String): Map<String, List<PatientSession>> =
        patientsOf(therapistId).associate { it.userId to sessionsForUser(it.userId) }

    fun sessionsForTherapist(therapistId: String, patientId: String): List<PatientSession>? =
        patient(patientId)?.takeIf { it.therapistId == therapistId }?.let { sessionsForUser(it.userId) }

    fun sessionForTherapist(therapistId: String, patientId: String, sessionId: String): TherapistSessionLookup {
        val owned = patient(patientId)?.takeIf { it.therapistId == therapistId }
            ?: return TherapistSessionLookup.PatientNotAssigned
        val session = sessionsForUser(owned.userId).firstOrNull { it.id == sessionId }
            ?: return TherapistSessionLookup.SessionNotFound
        return TherapistSessionLookup.Found(owned, session)
    }
}
