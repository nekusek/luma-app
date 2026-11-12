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
import androidx.compose.ui.res.painterResource
import android.content.ActivityNotFoundException
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import pl.zse.dziennik.dashboard.DashboardUiState
import pl.zse.dziennik.model.*


@Composable
fun DemoSection(destination: Int, state: DashboardUiState, theme: ThemeMode, onTheme: (ThemeMode) -> Unit, modifier: Modifier = Modifier, onError: (String) -> Unit = {}, showAssessments: Boolean = false, onShowAssessments: (Boolean) -> Unit = {}, onAccounts: () -> Unit = {}) {
    val data = state.snapshot
    if (destination == 1) {
        GradesScreen(data?.grades.orEmpty(), state.now.atZone(SchoolZone).toLocalDate(), data?.student?.name.orEmpty(), onAccounts, modifier)
        return
    }
    val titles = listOf("Pulpit", "Oceny", "Plan lekcji", "Frekwencja", "Więcej")
    Column(modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(titles[destination], style = MaterialTheme.typography.headlineLarge)
        Text("DEMO · dane fikcyjne", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        if (destination != 4 && data == null) { Text("Brak zapisanych danych. Odśwież Pulpit."); return@Column }
        when (destination) {
            2 -> TimetableViewer(data!!.timetable, state.now.atZone(SchoolZone).toLocalDate())
            3 -> AttendanceScreen(data!!.attendance, data.grades, state.now.atZone(SchoolZone).toLocalDate(), Modifier.weight(1f))
            4 -> {
                val uri = LocalUriHandler.current
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    item { OutlinedCard(onClick = onAccounts, modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(painterResource(R.drawable.ic_switch_account), null)
                            Column { Text("Konta i profile", style = MaterialTheme.typography.titleMedium); Text("Przełącz konto lub dodaj nowy profil", style = MaterialTheme.typography.bodySmall) }
                        }
                    } }
                    item { TextButton(onClick = { onShowAssessments(!showAssessments) }) { Text(if (showAssessments) "Ustawienia" else "Sprawdziany") } }
                    if (showAssessments) {
                        item { Text("Sprawdziany", style = MaterialTheme.typography.titleLarge) }
                        val assessments = data?.assessments.orEmpty().sortedBy { it.date }
                        if (assessments.isEmpty()) item { Text("Brak zapisanych sprawdzianów.") }
                        assessments.forEach { assessment -> item(key = assessment.id) {
                            OutlinedCard(Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(assessment.subject, style = MaterialTheme.typography.titleMedium)
                                    Text(assessment.title)
                                    Text(assessment.date.format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.forLanguageTag("pl-PL"))))
                                }
                            }
                        } }
                        return@LazyColumn
                    }
                    item { Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { Icon(painterResource(R.drawable.ic_palette), null); Text("Wygląd", style = MaterialTheme.typography.titleMedium) } }
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
