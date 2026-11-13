package pl.zse.dziennik

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.launch
import pl.zse.dziennik.model.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimetableScreen(entries: List<TimetableEntry>, today: LocalDate, modifier: Modifier = Modifier) {
    val anchor by rememberSaveable { mutableLongStateOf(today.toEpochDay()) }
    val pager = rememberPagerState(initialPage = 7, pageCount = { 15 })
    val scope = rememberCoroutineScope()
    var selected by rememberSaveable { mutableStateOf<String?>(null) }
    Column(modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = { scope.launch { pager.animateScrollToPage(pager.currentPage - 1) } }, enabled = pager.currentPage > 0) { Text("‹ Poprzedni") }
            TextButton(onClick = { scope.launch { pager.scrollToPage(7) } }) { Text("Dzisiaj") }
            TextButton(onClick = { scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } }, enabled = pager.currentPage < 14) { Text("Następny ›") }
        }
        HorizontalPager(state = pager, modifier = Modifier.weight(1f)) { page ->
            val day = LocalDate.ofEpochDay(anchor).plusDays((page - 7).toLong())
            val lessons = entries.filter { it.startsAt.atZone(SchoolZone).toLocalDate() == day }
            LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 20.dp)) {
                item { Text(day.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.forLanguageTag("pl-PL"))), style = MaterialTheme.typography.titleLarge) }
                if (lessons.isEmpty()) item { Text("Brak zapisanych lekcji na ten dzień.") }
                items(lessons, key = { it.id }) { lesson ->
                    OutlinedCard(onClick = { selected = lesson.id }, modifier = Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("${lesson.number}. ${lesson.subjectName}", style = MaterialTheme.typography.titleMedium, textDecoration = if (lesson.status == LessonStatus.CANCELLED) TextDecoration.LineThrough else TextDecoration.None)
                        Text("${lesson.startsAt.atZone(SchoolZone).toLocalTime()}–${lesson.endsAt.atZone(SchoolZone).toLocalTime()} · sala ${lesson.room ?: "—"}")
                        lesson.group?.let { Text("Grupa $it", style = MaterialTheme.typography.bodySmall) }
                        Text(lesson.status.planLabel(), color = if (lesson.status == LessonStatus.CANCELLED) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
                        lesson.changeDescription?.let { Text(it) }
                    } }
                }
            }
        }
    }
    entries.firstOrNull { it.id == selected }?.let { lesson -> ModalBottomSheet(onDismissRequest = { selected = null }) {
        LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { Text(lesson.subjectName, style = MaterialTheme.typography.headlineSmall) }
            item { Text("${lesson.startsAt.atZone(SchoolZone).format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))}–${lesson.endsAt.atZone(SchoolZone).toLocalTime()}\nSala: ${lesson.room ?: "—"}\nProwadzący: ${lesson.teacherName ?: "—"}\nGrupa: ${lesson.group ?: "cała klasa"}") }
            item { Text(lesson.status.planLabel()); lesson.changeDescription?.let { Text(it) }; TextButton(onClick = { selected = null }) { Text("Zamknij szczegóły lekcji") } }
        }
    } }
}
private fun LessonStatus.planLabel() = when (this) { LessonStatus.SCHEDULED -> "Planowa"; LessonStatus.SUBSTITUTION -> "Zastępstwo"; LessonStatus.CANCELLED -> "Odwołana"; LessonStatus.WINDOW -> "Okienko" }
