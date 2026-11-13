package com.pulihgo.app.ui.patient

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pulihgo.app.data.exercise.Exercise
import java.util.Locale

private val Teal = Color(0xFF087F7D)
private val TealLight = Color(0xFFE9F7F5)
private val Ink = Color(0xFF152A2D)
private val Muted = Color(0xFF60777A)
private val Canvas = Color(0xFFFAFBFA)

@Composable
fun PatientExerciseLibraryScreen(
    exercises: List<Exercise>,
    onSelectExercise: (Exercise) -> Unit,
    onBack: () -> Unit,
) {
    Surface(modifier = Modifier.fillMaxSize().testTag("exercise_library"), color = Canvas) {
        Column(
            modifier = Modifier.safeDrawingPadding().verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ExerciseTopBar(onBack)
            Text("CHOOSE EXERCISE", color = Teal, style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold)
            Text("Pilih latihan Anda", color = Ink, style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold)
            Text("Pilih latihan yang tersedia, baca panduan, lalu persiapkan ponsel sebelum kalibrasi.",
                color = Muted, style = MaterialTheme.typography.bodyMedium)

            if (exercises.isEmpty()) {
                Card(colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Latihan belum tersedia", color = Ink, style = MaterialTheme.typography.titleMedium)
                        Text("Data latihan belum dapat dimuat. Coba buka halaman ini kembali.",
                            color = Muted, style = MaterialTheme.typography.bodySmall)
                    }
                }
            } else {
                exercises.forEach { exercise ->
                    Card(
                        modifier = Modifier.fillMaxWidth().testTag("exercise_card_${exercise.id}"),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(1.dp, Color(0xFFE0ECEA)),
                    ) {
                        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Box(
                                    modifier = Modifier.size(48.dp).background(TealLight, RoundedCornerShape(14.dp)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text("↻", color = Teal, style = MaterialTheme.typography.headlineMedium)
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(exercise.name.replace(" and ", " / "), color = Ink,
                                        fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                                    Text("Latihan rotasi lengan bawah", color = Muted,
                                        style = MaterialTheme.typography.bodySmall)
                                }
                            }
                            Text(exercise.description, color = Muted, style = MaterialTheme.typography.bodyMedium)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                InfoPill("Target ${formatRom(exercise.targetRomDeg)}")
                                InfoPill("${exercise.targetReps} repetisi")
                            }
                            Button(
                                onClick = { onSelectExercise(exercise) },
                                modifier = Modifier.fillMaxWidth().testTag("exercise_select_${exercise.id}"),
                                colors = ButtonDefaults.buttonColors(containerColor = Teal),
                            ) { Text("Lihat Persiapan Latihan") }
                        }
                    }
                }
            }
            Text("Latihan sensor yang tersedia saat ini berfokus pada rotasi lengan bawah.",
                color = Muted, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun PatientExercisePreparationScreen(
    exercise: Exercise,
    onBack: () -> Unit,
    onContinueToCalibration: () -> Unit,
) {
    Surface(modifier = Modifier.fillMaxSize().testTag("exercise_preparation"), color = Canvas) {
        Column(
            modifier = Modifier.safeDrawingPadding().verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ExerciseTopBar(onBack)
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.size(60.dp).background(TealLight, CircleShape), contentAlignment = Alignment.Center) {
                    Text("✓", color = Teal, style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold)
                }
                Text("Ready to Practice?", color = Ink, style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold)
                Text("Ikuti langkah persiapan sebelum memulai ${exercise.name.replace(" and ", " / ")}.",
                    color = Muted, style = MaterialTheme.typography.bodyMedium)
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, Color(0xFFE0ECEA)),
            ) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("Persiapan perangkat dan posisi", color = Ink,
                        style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    PreparationStep(1, "Pasang ponsel dengan stabil di lengan bawah yang akan dilatih.")
                    PreparationStep(2, "Duduk atau berdiri dengan nyaman. Dekatkan siku ke badan dan tekuk sekitar 90°.")
                    PreparationStep(3, "Posisikan lengan bawah secara netral, ibu jari menghadap ke atas. Tahan diam saat kalibrasi.")
                    Button(
                        onClick = onContinueToCalibration,
                        modifier = Modifier.fillMaxWidth().testTag("start_calibration"),
                        colors = ButtonDefaults.buttonColors(containerColor = Teal),
                    ) { Text("Cek Sensor & Kalibrasi") }
                    Text("Langkah berikutnya membuka pemeriksaan sensor. Kalibrasi dilakukan setelah sensor siap.",
                        color = Muted, style = MaterialTheme.typography.bodySmall)
                }
            }

            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = TealLight)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Panduan gerakan", color = Ink, fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.titleSmall)
                    exercise.instructions.forEachIndexed { index, instruction ->
                        Text("${index + 1}. $instruction", color = Ink, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            if (exercise.safetyNotes.isNotEmpty()) {
                Card(modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3EF))) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Perhatian selama latihan", color = Color(0xFF874B3F),
                            fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
                        exercise.safetyNotes.forEach { note ->
                            Text("• $note", color = Color(0xFF874B3F), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
            OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                Text("Kembali ke Daftar Latihan", color = Teal)
            }
        }
    }
}

@Composable
private fun ExerciseTopBar(onBack: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        TextButton(onClick = onBack) { Text("‹ Kembali", color = Teal) }
        Spacer(Modifier.weight(1f))
        Text("PULIHGO", color = Ink, fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun InfoPill(label: String) {
    Box(Modifier.background(TealLight, RoundedCornerShape(24.dp)).padding(horizontal = 10.dp, vertical = 7.dp)) {
        Text(label, color = Teal, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun PreparationStep(number: Int, content: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top) {
        Box(Modifier.size(26.dp).background(TealLight, CircleShape), contentAlignment = Alignment.Center) {
            Text("$number", color = Teal, style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold)
        }
        Text(content, modifier = Modifier.weight(1f), color = Ink,
            style = MaterialTheme.typography.bodyMedium)
    }
}

private fun formatRom(degrees: Double): String = String.format(Locale.US, "%.0f°", degrees)
