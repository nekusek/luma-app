package pl.zse.dziennik

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import pl.zse.dziennik.model.*

@Composable
fun SchoolCalendarScreen(data: DiaryFeatures, today: LocalDate, onDone: (String, Boolean) -> Unit, modifier: Modifier = Modifier, busy: Boolean = false) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var day by rememberSaveable { mutableLongStateOf(today.toEpochDay()) }
    var monthIndex by rememberSaveable { mutableLongStateOf(today.year.toLong() * 12 + today.monthValue - 1) }
    var selected by rememberSaveable { mutableStateOf<String?>(null) }
    var showDone by rememberSaveable { mutableStateOf(true) }
    val month = YearMonth.of((monthIndex / 12).toInt(), (monthIndex % 12).toInt() + 1)
    val date = LocalDate.ofEpochDay(day)
    val tasks = data.tasks.filter { when (tab) { 0 -> it.dueOn == date; 1 -> it.kind == SchoolTaskKind.EXAM; else -> it.kind == SchoolTaskKind.HOMEWORK && (showDone || it.id !in data.completedHomework) } }.sortedWith(compareBy<SchoolTask> { it.dueOn }.thenBy { it.subject })
    Column(modifier.fillMaxSize().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Kalendarz i zadania", style = MaterialTheme.typography.headlineMedium)
        Text("DEMO · dane fikcyjne", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        PrimaryTabRow(selectedTabIndex = tab) { listOf("Kalendarz", "Sprawdziany", "Zadania").forEachIndexed { i, title -> Tab(selected = tab == i, onClick = { tab = i }, text = { Text(title, maxLines = 1, style = MaterialTheme.typography.labelSmall, color = if (tab == i) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface) }) } }
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 20.dp)) {
            if (tab == 0) item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(onClick = { monthIndex--; day = month.minusMonths(1).atDay(1).toEpochDay() }) { Text("‹", style = MaterialTheme.typography.headlineMedium) }
                    Text(month.format(DateTimeFormatter.ofPattern("LLLL yyyy", Locale.forLanguageTag("pl-PL"))), style = MaterialTheme.typography.titleMedium)
                    TextButton(onClick = { monthIndex++; day = month.plusMonths(1).atDay(1).toEpochDay() }) { Text("›", style = MaterialTheme.typography.headlineMedium) }
                }
                Row(Modifier.fillMaxWidth()) { listOf("Pn", "Wt", "Śr", "Cz", "Pt", "So", "Nd").forEach { Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { Text(it, style = MaterialTheme.typography.labelSmall) } } }
                val start = month.atDay(1).dayOfWeek.value - 1
                (0 until (start + month.lengthOfMonth() + 6) / 7).forEach { week ->
                    Row(Modifier.fillMaxWidth()) { (0..6).forEach { weekday ->
                        val number = week * 7 + weekday - start + 1
                        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            if (number in 1..month.lengthOfMonth()) {
                                val cell = month.atDay(number)
                                val hasEvent = data.tasks.any { it.dueOn == cell }
                                TextButton(onClick = { day = cell.toEpochDay() }, contentPadding = PaddingValues(0.dp), colors = ButtonDefaults.textButtonColors(containerColor = if (date == cell) MaterialTheme.colorScheme.primaryContainer else androidx.compose.ui.graphics.Color.Transparent)) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) { Text(number.toString(), fontWeight = if (cell == today) FontWeight.Bold else FontWeight.Normal); Text(if (hasEvent) "•" else " ", style = MaterialTheme.typography.labelSmall) }
                                }
                            }
                        }
                    } }
                }
                TextButton(onClick = { day = today.toEpochDay(); monthIndex = today.year.toLong() * 12 + today.monthValue - 1 }) { Text("Dzisiaj") }
                Text(date.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.forLanguageTag("pl-PL"))), style = MaterialTheme.typography.titleMedium)
            }
            if (tab == 2) item { Row(verticalAlignment = Alignment.CenterVertically) { Switch(checked = showDone, onCheckedChange = { showDone = it }); Spacer(Modifier.width(8.dp)); Text("Pokaż wykonane") } }
            if (tasks.isEmpty()) item { Text("Brak wydarzeń w tym widoku.", Modifier.padding(vertical = 16.dp)) }
            items(tasks, key = { it.id }) { task ->
                OutlinedCard(onClick = { selected = task.id }, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(if (task.kind == SchoolTaskKind.EXAM) "Sprawdzian" else "Zadanie domowe", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        Text(task.subject, style = MaterialTheme.typography.titleMedium)
                        Text(task.title)
                        Text("Termin: ${task.dueOn}", style = MaterialTheme.typography.bodySmall)
                        if (task.kind == SchoolTaskKind.HOMEWORK) Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(modifier = Modifier.testTag("homework:${task.id}"), checked = task.id in data.completedHomework, onCheckedChange = { onDone(task.id, it) }, enabled = !busy)
                            Text(if (task.id in data.completedHomework) "Wykonane lokalnie" else if (task.dueOn < today) "Po terminie · do wykonania" else "Do wykonania", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
    data.tasks.firstOrNull { it.id == selected }?.let { task -> TaskDetails(task) { selected = null } }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskDetails(task: SchoolTask, onDismiss: () -> Unit) {
    var attachment by remember { mutableStateOf(false) }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        LazyColumn(contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item { Text(task.subject, style = MaterialTheme.typography.labelLarge); Text(task.title, style = MaterialTheme.typography.headlineSmall) }
            item { Text("Termin: ${task.dueOn}\nNauczyciel: ${task.teacher}"); Text(task.description, Modifier.padding(top = 12.dp)) }
            if (task.attachmentName != null) item { OutlinedButton(onClick = { attachment = !attachment }) { Text(task.attachmentName.orEmpty()) }; if (attachment) androidx.compose.foundation.text.selection.SelectionContainer { Text(task.attachmentContent.orEmpty()) } }
            item { Text("Oznaczenie wykonania nie wysyła odpowiedzi do dziennika.", style = MaterialTheme.typography.bodySmall); TextButton(onClick = onDismiss) { Text("Zamknij szczegóły zadania") } }
        }
    }
}
