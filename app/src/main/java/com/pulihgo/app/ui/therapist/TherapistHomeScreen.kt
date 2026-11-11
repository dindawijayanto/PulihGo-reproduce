package com.pulihgo.app.ui.therapist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.pulihgo.app.data.model.Patient
import com.pulihgo.app.data.model.User

/**
 * Tujuan login untuk role THERAPIST. Menampilkan pasien yang `therapistId`-nya
 * sama dengan therapist yang login, sebagai bukti data sudah terikat ke role.
 * Dashboard lengkap dikerjakan di PGF-THR-02.
 */
@Composable
fun TherapistHomeScreen(
    therapist: User?,
    patients: List<Patient>,
    users: List<User>,
    onLogout: () -> Unit,
) {
    val namesByUserId = users.associate { it.id to it.fullName }
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier.safeDrawingPadding().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Therapist Home", style = MaterialTheme.typography.labelLarge, modifier = Modifier.testTag("therapist_home"))
            Text("Halo, ${therapist?.fullName.orEmpty()}", style = MaterialTheme.typography.headlineMedium)
            Text("Pasien Anda (${patients.size})", style = MaterialTheme.typography.titleMedium)
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(patients, key = { it.id }) { patient ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Text(namesByUserId[patient.userId] ?: patient.id, style = MaterialTheme.typography.titleSmall)
                            Text(
                                "${patient.id} · ${patient.age} th · lengan ${patient.affectedArm.lowercase()}",
                                style = MaterialTheme.typography.bodySmall,
                            )
                            Text(patient.rehabilitationGoal, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
            OutlinedButton(onClick = onLogout, modifier = Modifier.fillMaxWidth()) { Text("Keluar") }
        }
    }
}
