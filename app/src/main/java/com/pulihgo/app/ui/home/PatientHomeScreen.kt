package com.pulihgo.app.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pulihgo.app.R
import com.pulihgo.app.ui.theme.PulihGoTheme
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale

/**
 * PGF-PAT-03: display-only patient home. Session values are supplied by the host.
 * The MainActivity demo data must be replaced by the real session repository later.
 * All four display states are available: empty / expanded / session / expanded session.
 */
data class HomeExerciseSession(
    val date: LocalDate,
    val repetitions: Int,
    val peakRomDegrees: Int,
    val smoothnessPercent: Int,
    val exerciseName: String = "Forearm Rotation",
    val time: LocalTime = LocalTime.of(7, 45)
)

private val Teal = Color(0xFF0D7C7C)
private val TealGreen = Color(0xFF159F73)
private val AlertRed = Color(0xFFD9383A)

private data class HomePalette(
    val background: Color,
    val surface: Color,
    val surfaceAlt: Color,
    val text: Color,
    val muted: Color,
    val outline: Color,
    val practiceStart: Color,
    val practiceEnd: Color,
    val progressStart: Color,
    val progressEnd: Color,
    val coachBackground: Color,
    val metricTrack: Color,
    val selectedDate: Color,
    val restingBackground: Color
)

private fun homePalette(dark: Boolean): HomePalette = if (dark) {
    HomePalette(
        background = Color(0xFF111717),
        surface = Color(0xFF202727),
        surfaceAlt = Color(0xFF172020),
        text = Color(0xFFF2F7F6),
        muted = Color(0xFFB9C6C6),
        outline = Color(0xFF3A4949),
        practiceStart = Color(0xFF183838),
        practiceEnd = Color(0xFF203232),
        progressStart = Color(0xFF202727),
        progressEnd = Color(0xFF1C3435),
        coachBackground = Color(0xFF1B3C39),
        metricTrack = Color(0xFF36504F),
        selectedDate = Color(0xFFECF7F4),
        restingBackground = Color(0xFF2A2425)
    )
} else {
    HomePalette(
        background = Color(0xFFF9F9F7),
        surface = Color.White,
        surfaceAlt = Color(0xFFFBFCFC),
        text = Color(0xFF1A1E21),
        muted = Color(0xFF66757D),
        outline = Color(0xFFE4EAEB),
        practiceStart = Color(0xFFEAF9FB),
        practiceEnd = Color(0xFFF4FBFB),
        progressStart = Color.White,
        progressEnd = Color(0xFFEAF9FB),
        coachBackground = Color(0xFFE8F7F5),
        metricTrack = Color(0xFFE6F3F2),
        selectedDate = Color(0xFF1C2526),
        restingBackground = Color.White
    )
}

private val CardShape = RoundedCornerShape(17.dp)
private val FormatDate = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)
private val FormatTime = DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH)
private fun monday(day: LocalDate) = day.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

@Composable
fun PatientHomeScreen(
    patientName: String,
    sessions: List<HomeExerciseSession>,
    onSettings: () -> Unit,
    onStartExercise: () -> Unit,
    onOpenSummary: () -> Unit,
    modifier: Modifier = Modifier,
    today: LocalDate = LocalDate.now(),
    isDark: Boolean = false
) {
    val colors = homePalette(isDark)
    var expanded by rememberSaveable { mutableStateOf(false) }
    var selectedDayOffset by rememberSaveable { mutableIntStateOf(0) }
    var weekOffset by rememberSaveable { mutableIntStateOf(0) }
    val selectedDate = today.plusDays(selectedDayOffset.toLong())
    val displayedWeekStart = monday(today).plusWeeks(weekOffset.toLong())
    val selectedSessions = sessions.filter { it.date == selectedDate }
    val allSessions = sessions.sortedWith(compareBy<HomeExerciseSession> { it.date }.thenBy { it.time })
    val isToday = today == selectedDate

    Column(modifier.fillMaxSize().background(colors.background).statusBarsPadding().navigationBarsPadding()) {
        HomeTopBar(colors, onSettings)
        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item(key = "practice") {
                PracticeCard(
                    colors = colors,
                    selectedDate = selectedDate,
                    isToday = isToday,
                    sessionCount = selectedSessions.size,
                    onStartExercise = onStartExercise
                )
            }
            item(key = "progress_toggle") {
                ProgressToggle(colors, expanded) { expanded = !expanded }
            }
            if (expanded) {
                item(key = "weekly_activity") {
                    WeeklyActivity(
                        colors = colors,
                        displayedWeekStart = displayedWeekStart,
                        selectedDate = selectedDate,
                        sessions = sessions,
                        onPreviousWeek = {
                            weekOffset -= 1
                            val start = monday(today).plusWeeks(weekOffset.toLong())
                            selectedDayOffset = (start.toEpochDay() - today.toEpochDay()).toInt()
                        },
                        onNextWeek = {
                            weekOffset += 1
                            val start = monday(today).plusWeeks(weekOffset.toLong())
                            selectedDayOffset = (start.toEpochDay() - today.toEpochDay()).toInt()
                        },
                        onSelectDate = { date ->
                            selectedDayOffset = (date.toEpochDay() - today.toEpochDay()).toInt()
                        }
                    )
                }
                item(key = "metrics_heading") {
                    SectionHeading(if (isToday) "Today's Target Metrics" else "Selected Day Metrics", colors)
                }
                item(key = "metrics") {
                    TargetMetrics(colors, selectedSessions)
                }
                // The additional sections in Figma's tall Home / Session / Expanded
                // are BELOW the viewport. Keep all inside the scrollable LazyColumn.
                if (selectedSessions.isNotEmpty()) {
                    item(key = "coach") { ListeningToBody(colors) }
                    item(key = "effort") { TimeToRest(colors, selectedSessions) }
                    item(key = "alltime_title") { SectionHeading("All-Time Stats", colors) }
                    item(key = "alltime_stats") { AllTimeStats(colors, allSessions) }
                    item(key = "session_logs_title") { SectionHeading("Today's Session Logs", colors) }
                    items(count = selectedSessions.size, key = { index -> "session_$index" }) { index ->
                        SessionLog(colors, selectedSessions.sortedByDescending { it.time }[index])
                    }
                } else {
                    item(key = "empty_coach") { ReadyToPractice(colors) }
                }
            }
        }
        BottomTabs(colors, onExercise = onStartExercise, onSummary = onOpenSummary)
    }
}

@Composable
private fun HomeTopBar(c: HomePalette, onSettings: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().height(72.dp).padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(R.drawable.onboarding_logo),
            contentDescription = "PulihGo logo",
            modifier = Modifier.size(30.dp),
            tint = Color.Unspecified
        )
        Spacer(Modifier.weight(1f))
        IconButton(
            onClick = onSettings,
            modifier = Modifier.size(44.dp).background(c.surface, CircleShape)
                .border(1.dp, c.outline, CircleShape)
        ) {
            Icon(
                painter = painterResource(R.drawable.pg_home_settings),
                contentDescription = "Settings",
                tint = c.text,
                modifier = Modifier.size(21.dp)
            )
        }
    }
}

@Composable
private fun PracticeCard(
    colors: HomePalette,
    selectedDate: LocalDate,
    isToday: Boolean,
    sessionCount: Int,
    onStartExercise: () -> Unit
) {
    Column(
        Modifier.fillMaxWidth().clip(CardShape)
            .background(Brush.horizontalGradient(listOf(colors.practiceStart, colors.practiceEnd)))
            .border(1.dp, colors.outline, CardShape)
            .padding(16.dp)
    ) {
        Text(
            if (isToday) "TODAY'S PRACTICE" else selectedDate.format(FormatDate).uppercase(Locale.ENGLISH),
            fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 0.55.sp,
            color = colors.text
        )
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(
                    if (sessionCount > 0) R.drawable.pg_home_status_complete else R.drawable.pg_home_circle
                ),
                contentDescription = if (sessionCount > 0) "Practice complete" else "Not practiced yet",
                tint = if (sessionCount > 0) Color.Unspecified else Color(0xFF8CA8AF),
                modifier = Modifier.size(34.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    if (sessionCount > 0) "Practice Complete!" else "Not Practiced Yet",
                    color = colors.text, fontWeight = FontWeight.Bold, fontSize = 17.sp
                )
                Text(
                    if (sessionCount > 0) "You have logged $sessionCount ${if (sessionCount == 1) "session" else "sessions"} ${if (isToday) "today" else "this day"}."
                    else "Complete a scheduled range of motion activity.",
                    color = colors.muted, fontSize = 12.sp, lineHeight = 18.sp
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = onStartExercise,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Teal, contentColor = Color.White)
        ) {
            Icon(painterResource(R.drawable.pg_home_play), contentDescription = null,
                tint = Color.White, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Start Exercise", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
    }
}

@Composable
private fun ProgressToggle(c: HomePalette, expanded: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().height(56.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Brush.horizontalGradient(listOf(c.progressStart, c.progressEnd)))
            .border(1.dp, c.outline, RoundedCornerShape(14.dp))
            .clickable(onClick = onToggle)
            .semantics { contentDescription = if (expanded) "Hide Detailed Stats" else "See My Progress" },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text(if (expanded) "Hide Detailed Stats" else "See My Progress",
            color = c.text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.width(9.dp))
        Icon(painterResource(if (expanded) R.drawable.pg_home_chevron_up else R.drawable.pg_home_chevron_down),
            contentDescription = null, tint = c.muted, modifier = Modifier.size(17.dp))
    }
}

@Composable
private fun SectionHeading(title: String, c: HomePalette) {
    Text(title, color = c.text, fontSize = 17.sp, fontWeight = FontWeight.Bold)
}

@Composable
private fun WeeklyActivity(
    colors: HomePalette,
    displayedWeekStart: LocalDate,
    selectedDate: LocalDate,
    sessions: List<HomeExerciseSession>,
    onPreviousWeek: () -> Unit,
    onNextWeek: () -> Unit,
    onSelectDate: (LocalDate) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Weekly Activity", color = colors.text, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            Row(
                modifier = Modifier.background(colors.surface, CircleShape)
                    .border(1.dp, colors.outline, CircleShape),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onPreviousWeek, modifier = Modifier.size(35.dp)) {
                    Icon(painterResource(R.drawable.pg_home_chevron_left),
                        contentDescription = "Previous week", tint = colors.muted,
                        modifier = Modifier.size(18.dp))
                }
                Text("Week", color = colors.text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                IconButton(onClick = onNextWeek, modifier = Modifier.size(35.dp)) {
                    Icon(painterResource(R.drawable.pg_home_chevron_right),
                        contentDescription = "Next week", tint = colors.muted,
                        modifier = Modifier.size(18.dp))
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Column(
            Modifier.fillMaxWidth().background(colors.surface, CardShape)
                .border(1.dp, colors.outline, CardShape).padding(horizontal = 10.dp, vertical = 18.dp)
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                val letters = listOf("M", "T", "W", "T", "F", "S", "S")
                letters.forEachIndexed { index, letter ->
                    val day = displayedWeekStart.plusDays(index.toLong())
                    val selected = day == selectedDate
                    val hasSession = sessions.any { it.date == day }
                    Column(
                        modifier = Modifier.weight(1f).clickable { onSelectDate(day) }
                            .padding(vertical = 2.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(letter, color = colors.muted, fontSize = 11.sp)
                        Spacer(Modifier.height(9.dp))
                        Box(
                            modifier = Modifier.size(33.dp).clip(CircleShape)
                                .background(if (selected) colors.selectedDate else Color.Transparent),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(day.dayOfMonth.toString(),
                                color = if (selected) colors.background else colors.text,
                                fontSize = 12.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
                        }
                        Spacer(Modifier.height(4.dp))
                        Box(Modifier.size(5.dp).background(
                            if (hasSession) TealGreen else Color.Transparent, CircleShape))
                    }
                }
            }
        }
    }
}

@Composable
private fun TargetMetrics(c: HomePalette, sessions: List<HomeExerciseSession>) {
    val peak = sessions.maxOfOrNull { it.peakRomDegrees } ?: 0
    val smooth = if (sessions.isEmpty()) 0 else sessions.map { it.smoothnessPercent }.average().toInt()
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        MetricTile(c, "PEAK ROM", "$peak°", (peak / 180f).coerceIn(0f, 1f), Teal, Modifier.weight(1f))
        MetricTile(c, "SMOOTHNESS", "$smooth%", (smooth / 100f).coerceIn(0f, 1f), TealGreen, Modifier.weight(1f))
    }
}

@Composable
private fun MetricTile(c: HomePalette, label: String, value: String, progress: Float, tint: Color, modifier: Modifier) {
    Column(modifier.background(c.surface, CardShape).border(1.dp, c.outline, CardShape)
        .padding(vertical = 21.dp, horizontal = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(88.dp)) {
            Canvas(Modifier.fillMaxSize()) {
                val thickness = 5.dp.toPx()
                val r = (size.minDimension - thickness) / 2
                drawCircle(c.metricTrack, radius = r, style = Stroke(thickness))
                drawArc(tint, -90f, progress * 360f, useCenter = false,
                    style = Stroke(thickness, cap = StrokeCap.Round))
            }
            Text(value, color = c.text, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
        }
        Spacer(Modifier.height(13.dp))
        Text(label, color = c.muted, fontSize = 11.sp, fontWeight = FontWeight.Bold,
            letterSpacing = 0.6.sp, textAlign = TextAlign.Center)
    }
}

@Composable
private fun ListeningToBody(c: HomePalette) {
    Column(Modifier.fillMaxWidth().background(c.surface, CardShape)
        .border(1.dp, c.outline, CardShape).padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(painterResource(R.drawable.pg_home_heart), null,
                tint = Color(0xFFE96670), modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text("Listening to Your Body", color = c.text, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
        Spacer(Modifier.height(9.dp))
        Text("Good job stopping for discomfort; rest is essential for recovery.",
            color = c.muted, fontSize = 12.sp, lineHeight = 18.sp)
    }
}

@Composable
private fun TimeToRest(c: HomePalette, sessions: List<HomeExerciseSession>) {
    val effortPosition = (sessions.sumOf { it.repetitions } / 20f).coerceIn(0f, 1f)
    Column(Modifier.fillMaxWidth().background(c.restingBackground, CardShape)
        .border(1.dp, c.outline, CardShape).padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Time to rest", color = AlertRed, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(2.dp))
                Text("TODAY'S EFFORT", color = c.muted, fontSize = 10.sp,
                    fontWeight = FontWeight.Bold, letterSpacing = 0.7.sp)
            }
            Icon(painterResource(R.drawable.pg_home_info),
                contentDescription = "Effort indicator is a demo estimate",
                tint = c.muted, modifier = Modifier.size(17.dp))
        }
        Spacer(Modifier.height(18.dp))
        Box(Modifier.fillMaxWidth().height(14.dp), contentAlignment = Alignment.CenterStart) {
            Row(Modifier.fillMaxWidth().height(5.dp).clip(CircleShape)) {
                Box(Modifier.weight(1f).fillMaxSize().background(Color(0xFF22A268)))
                Box(Modifier.weight(1f).fillMaxSize().background(Color(0xFFE7AE34)))
                Box(Modifier.weight(1f).fillMaxSize().background(Color(0xFFE55353)))
            }
            // Decorative dot only: not a medically validated intensity classification.
            Canvas(Modifier.fillMaxSize()) {
                val r = 5.dp.toPx()
                val x = (size.width * effortPosition).coerceIn(r + 2.dp.toPx(), size.width - r - 2.dp.toPx())
                drawCircle(Color.White, radius = r + 1.dp.toPx(), center = androidx.compose.ui.geometry.Offset(x, size.height / 2))
                drawCircle(AlertRed, radius = r, center = androidx.compose.ui.geometry.Offset(x, size.height / 2))
            }
        }
        Spacer(Modifier.height(8.dp))
        Text("Illustrative effort indicator · not medical advice", color = c.muted, fontSize = 10.sp)
    }
}

@Composable
private fun AllTimeStats(c: HomePalette, sessions: List<HomeExerciseSession>) {
    val totalReps = sessions.sumOf { it.repetitions }
    val maxRom = sessions.maxOfOrNull { it.peakRomDegrees } ?: 0
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile(c, sessions.size.toString(), "TOTAL SESSIONS", Modifier.weight(1f))
            StatTile(c, totalReps.toString(), "TOTAL REPETITIONS", Modifier.weight(1f))
        }
        StatTile(c, "$maxRom°", "BEST ROM REACHED", Modifier.fillMaxWidth())
    }
}

@Composable
private fun StatTile(c: HomePalette, value: String, label: String, modifier: Modifier) {
    Column(modifier.background(c.surface, RoundedCornerShape(14.dp))
        .border(1.dp, c.outline, RoundedCornerShape(14.dp))
        .padding(16.dp)) {
        Text(value, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = c.text)
        Spacer(Modifier.height(2.dp))
        Text(label, fontWeight = FontWeight.Bold, fontSize = 10.sp, color = c.muted, letterSpacing = 0.7.sp)
    }
}

@Composable
private fun SessionLog(c: HomePalette, session: HomeExerciseSession) {
    Column(Modifier.fillMaxWidth().background(c.surface, RoundedCornerShape(14.dp))
        .border(1.dp, c.outline, RoundedCornerShape(14.dp)).padding(horizontal = 14.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(painterResource(R.drawable.pg_home_pulse), contentDescription = null,
                tint = Teal, modifier = Modifier.size(17.dp))
            Spacer(Modifier.width(6.dp))
            Text(session.exerciseName, fontWeight = FontWeight.Bold, fontSize = 12.sp,
                color = c.text, maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f))
            Text(session.time.format(FormatTime), fontSize = 11.sp, color = c.muted)
        }
        Spacer(Modifier.height(8.dp))
        HorizontalDivider(color = c.outline)
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            LogMetric(session.repetitions.toString(), "REPS", c, Modifier.weight(1f))
            LogMetric("${session.peakRomDegrees}°", "ROM", c, Modifier.weight(1f))
            LogMetric("${session.smoothnessPercent}%", "SMOOTHNESS", c, Modifier.weight(1f))
        }
    }
}

@Composable
private fun LogMetric(value: String, label: String, c: HomePalette, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = c.text)
        Text(label, fontWeight = FontWeight.Bold, fontSize = 10.sp,
            letterSpacing = 0.35.sp, color = c.muted)
    }
}

@Composable
private fun ReadyToPractice(c: HomePalette) {
    Row(Modifier.fillMaxWidth().background(c.coachBackground, CardShape)
        .border(1.dp, c.outline, CardShape).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(painterResource(R.drawable.pg_home_check_circle), null,
            tint = TealGreen, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(10.dp))
        Column {
            Text("Ready to Practice", color = c.text, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text("Begin your scheduled exercise when you feel ready.",
                color = c.muted, fontSize = 12.sp, lineHeight = 18.sp)
        }
    }
}

@Composable
private fun BottomTabs(c: HomePalette, onExercise: () -> Unit, onSummary: () -> Unit) {
    Column {
        HorizontalDivider(color = c.outline)
        Row(
            modifier = Modifier.fillMaxWidth().height(66.dp).background(c.surface),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomTab(c, "HOME", R.drawable.pg_home_home, true, {}, Modifier.weight(1f))
            BottomTab(c, "EXERCISE", R.drawable.pg_home_activity, false, onExercise, Modifier.weight(1f))
            BottomTab(c, "SUMMARY", R.drawable.pg_home_chart, false, onSummary, Modifier.weight(1f))
        }
    }
}

@Composable
private fun BottomTab(c: HomePalette, name: String, icon: Int, selected: Boolean,
                      onClick: () -> Unit, modifier: Modifier) {
    Column(modifier.clickable(onClick = onClick).padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Icon(painterResource(icon), contentDescription = name,
            tint = if (selected) Teal else c.muted, modifier = Modifier.size(20.dp))
        Text(name, color = if (selected) Teal else c.muted, fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium)
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 924)
@Composable
private fun HomeLightPreview() {
    PulihGoTheme(darkTheme = false, dynamicColor = false) {
        PatientHomeScreen("Budi Santoso", emptyList(), {}, {}, {}, isDark = false)
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 924)
@Composable
private fun HomeDarkPreview() {
    PulihGoTheme(darkTheme = true, dynamicColor = false) {
        PatientHomeScreen("Budi Santoso", listOf(HomeExerciseSession(LocalDate.now(), 12, 75, 86)),
            {}, {}, {}, isDark = true)
    }
}
