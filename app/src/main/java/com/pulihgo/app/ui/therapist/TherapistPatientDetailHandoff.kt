package com.pulihgo.app.ui.therapist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

/**
 * TEMPORARY handoff for PGF-THR-04 (Patient Detail, owner: Farrel).
 *
 * Navigation contract from PGF-THR-03:
 * - Route: AppRoute.THERAPIST_PATIENT_DETAIL (THERAPIST role only).
 * - Input: the selected [PatientListItem], already resolved from the logged-in
 *   therapist's own assigned patients (null when the id is not assigned to them).
 * - Back: returns to the Patient List.
 * Replace this composable with the real Patient Detail screen; keep the contract.
 */
@Composable
fun TherapistPatientDetailHandoff(
    patient: PatientListItem?,
    onBack: () -> Unit,
) {
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier.safeDrawingPadding().padding(24.dp).testTag("therapist_patient_detail_handoff"),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Patient Detail", style = MaterialTheme.typography.labelLarge)
            if (patient == null) {
                Text("Patient not found", style = MaterialTheme.typography.headlineSmall)
                Text(
                    "This patient is not assigned to your account.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else {
                Text(patient.name, style = MaterialTheme.typography.headlineMedium)
                Text(
                    "#${patient.patientId} • ${patient.age} yrs • ${patient.rehabilitationGoal}",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    "${patient.sessionCount} session(s) saved on this device.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    "The full Patient Detail screen is being built in PGF-THR-04.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Back to My Patients") }
        }
    }
}
