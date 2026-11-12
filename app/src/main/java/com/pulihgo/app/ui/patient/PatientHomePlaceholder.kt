package com.pulihgo.app.ui.patient

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

@Composable
fun PatientHomePlaceholder(
    patientName: String,
    onOpenSensorCheck: () -> Unit,
    onLogout: () -> Unit,
) {
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier.safeDrawingPadding().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Patient Home", style = MaterialTheme.typography.labelLarge, modifier = Modifier.testTag("patient_home"))
            Text("Halo, $patientName", style = MaterialTheme.typography.headlineMedium)
            Text(
                "Sebelum latihan, pastikan sensor ponsel terbaca dan lakukan kalibrasi posisi awal.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Button(onClick = onOpenSensorCheck, modifier = Modifier.fillMaxWidth()) {
                Text("Cek Sensor & Kalibrasi")
            }
            OutlinedButton(onClick = onLogout, modifier = Modifier.fillMaxWidth()) { Text("Keluar") }
        }
    }
}
