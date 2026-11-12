package pl.zse.dziennik

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import android.content.ActivityNotFoundException
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import pl.zse.dziennik.dashboard.DashboardUiState
import pl.zse.dziennik.model.*


@Composable
fun DemoSection(destination: Int, state: DashboardUiState, theme: ThemeMode, onTheme: (ThemeMode) -> Unit, modifier: Modifier = Modifier, onError: (String) -> Unit = {}) {
    val data = state.snapshot
    val titles = listOf("Pulpit", "Oceny", "Plan lekcji", "Frekwencja", "Więcej")
    Column(modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(titles[destination], style = MaterialTheme.typography.headlineLarge)
        Text("DEMO · dane fikcyjne", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        if (destination != 4 && data == null) { Text("Brak zapisanych danych. Odśwież Pulpit."); return@Column }
        when (destination) {
            1 -> GradeViewer(data!!.grades)
            2 -> TimetableViewer(data!!.timetable, state.now.atZone(SchoolZone).toLocalDate())
            3 -> {
                Text(data!!.attendance.attendancePercent()?.let { "${it.format()}%" } ?: "Brak danych", style = MaterialTheme.typography.displaySmall)
                Text("Spóźnienie liczone jako obecność; zwolnienie poza mianownikiem. Reguła demonstracyjna.")
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    data.attendance.groupBy { it.subjectId }.forEach { (subject, entries) -> item {
                        Text("${data.grades.firstOrNull { it.subjectId == subject }?.subjectName ?: subject}: ${entries.attendancePercent()?.let { "${it.format()}%" } ?: "Brak danych"}")
                    } }
                    AttendanceStatus.entries.forEach { status -> item { Text("${status.label()}: ${data.attendance.count { it.status == status }}") } }
                }
            }
            4 -> {
                val uri = LocalUriHandler.current
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    item { Text("Wygląd", style = MaterialTheme.typography.titleMedium) }
                    ThemeMode.entries.forEach { mode -> item {
                        Row { RadioButton(selected = theme == mode, onClick = { onTheme(mode) }); TextButton(onClick = { onTheme(mode) }) { Text(mode.label()) } }
                    } }
                    item { HorizontalDivider() }
                    item { TextButton(onClick = {
                        try { uri.openUri("https://zse.rzeszow.pl/") }
                        catch (_: ActivityNotFoundException) { onError("Nie znaleziono przeglądarki do otwarcia strony.") }
                        catch (_: IllegalArgumentException) { onError("Nie udało się otworzyć strony w przeglądarce.") }
                    }) { Text("zse.rzeszow.pl ↗") } }
                    item { Text("Zastępstwa i plan laboratoriów są niedostępne w wersji demo.", style = MaterialTheme.typography.bodySmall) }
                    item { Text("Wiadomości i uwagi są niedostępne w wersji demo.", style = MaterialTheme.typography.bodySmall) }
                }
            }
        }
    }
}

@Composable
private fun GradeViewer(grades: List<Grade>) {
    val groups = grades.groupBy { it.subjectId }.values.toList()
    var selectedSubjectId by rememberSaveable { mutableStateOf<String?>(null) }
    var value by rememberSaveable { mutableFloatStateOf(5f) }
    var weight by rememberSaveable { mutableFloatStateOf(2f) }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        if (groups.isEmpty()) item { Text("Nie ma jeszcze ocen.") }
        groups.forEach { entries -> item {
            OutlinedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text(entries.first().subjectName, style = MaterialTheme.typography.titleMedium)
                    Text("Średnia ważona: ${entries.weightedAverage()?.format() ?: "—"}")
                    entries.forEach { Text("${it.displayValue} · ${it.category} · waga ${it.weight}") }
                }
            }
        } }
        if (groups.isNotEmpty()) item {
            val index = groups.indexOfFirst { it.first().subjectId == selectedSubjectId }.coerceAtLeast(0)
            val entries = groups[index]
            Text("Symulator średniej", style = MaterialTheme.typography.titleLarge)
            TextButton(onClick = { selectedSubjectId = groups[(index + 1) % groups.size].first().subjectId }) { Text("Przedmiot: ${entries.first().subjectName}") }
            Text("Ocena ${value.toInt()} · waga ${weight.toInt()}")
            Slider(value = value, onValueChange = { value = it }, valueRange = 1f..6f, steps = 4)
            Slider(value = weight, onValueChange = { weight = it }, valueRange = 1f..5f, steps = 3)
            val simulated = entries + entries.first().copy(id = "simulation", displayValue = value.toInt().toString(), numericValue = value.toInt().toDouble(), weight = weight.toInt(), countsTowardsAverage = true)
            Text("Po dodaniu: ${simulated.weightedAverage()?.format()}", style = MaterialTheme.typography.titleMedium)
            Text("Symulacja nie zmienia zapisanych ocen.", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun TimetableViewer(entries: List<TimetableEntry>, today: LocalDate) {
    val anchorEpochDay by rememberSaveable { mutableLongStateOf(today.toEpochDay()) }
    val pager = rememberPagerState(initialPage = 7, pageCount = { 15 })
    HorizontalPager(state = pager, modifier = Modifier.fillMaxSize()) { page ->
        val day = LocalDate.ofEpochDay(anchorEpochDay).plusDays((page - 7).toLong())
        LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item { Text(day.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.forLanguageTag("pl-PL"))), style = MaterialTheme.typography.titleMedium) }
            val daily = entries.filter { it.startsAt.atZone(SchoolZone).toLocalDate() == day }
            if (daily.isEmpty()) item { Text("Brak zapisanych lekcji na ten dzień.") }
            daily.forEach { lesson -> item {
                OutlinedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("${lesson.number}. ${lesson.subjectName}", style = MaterialTheme.typography.titleMedium)
                        Text("${lesson.startsAt.atZone(SchoolZone).toLocalTime()}–${lesson.endsAt.atZone(SchoolZone).toLocalTime()} · sala ${lesson.room ?: "—"}")
                        lesson.group?.let { Text("Grupa $it") }
                        Text(when (lesson.status) { LessonStatus.CANCELLED -> "Odwołana"; LessonStatus.WINDOW -> "Okienko"; LessonStatus.SUBSTITUTION -> "Zastępstwo"; LessonStatus.SCHEDULED -> "Planowa" })
                        lesson.changeDescription?.let { Text(it) }
                    }
                }
            } }
        }
    }
}

private fun Double.format() = String.format(Locale.forLanguageTag("pl-PL"), "%.2f", this)
private fun ThemeMode.label() = when (this) { ThemeMode.SYSTEM -> "Systemowy / Material You"; ThemeMode.LIGHT -> "Jasny"; ThemeMode.DARK -> "Ciemny"; ThemeMode.AMOLED -> "AMOLED" }
private fun AttendanceStatus.label() = when (this) { AttendanceStatus.PRESENT -> "Obecności"; AttendanceStatus.LATE -> "Spóźnienia"; AttendanceStatus.ABSENT -> "Nieobecności"; AttendanceStatus.EXCUSED_ABSENCE -> "Usprawiedliwione"; AttendanceStatus.RELEASED -> "Zwolnienia" }
