package pl.zse.dziennik

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import pl.zse.dziennik.model.*

@Composable
fun AttendanceScreen(attendance: List<Attendance>, grades: List<Grade>, today: LocalDate, modifier: Modifier = Modifier) {
    var monthOnly by rememberSaveable { mutableStateOf(false) }
    var selectedStatus by rememberSaveable { mutableStateOf<String?>(null) }
    var expandedSubject by rememberSaveable { mutableStateOf<String?>(null) }
    val entries = remember(attendance, monthOnly, today) {
        attendance.filter { !it.date.isAfter(today) && (!monthOnly || it.date.year == today.year && it.date.month == today.month) }
    }
    val names = remember(grades) { grades.associate { it.subjectId to it.subjectName } }
    val percentage = entries.attendancePercent()
    val present = entries.count { it.status == AttendanceStatus.PRESENT || it.status == AttendanceStatus.LATE }
    val counted = entries.count { it.status != AttendanceStatus.RELEASED }
    val groups = entries.groupBy { it.subjectId }.toList().sortedBy { names[it.first] ?: it.first }
    val history = entries.filter { selectedStatus == null || it.status.name == selectedStatus }.sortedWith(compareByDescending<Attendance> { it.date }.thenBy { it.id })
    LazyColumn(modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp), contentPadding = PaddingValues(bottom = 20.dp)) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = !monthOnly, onClick = { monthOnly = false }, label = { Text("Wszystkie dane") })
                FilterChip(selected = monthOnly, onClick = { monthOnly = true }, label = { Text("Ten miesiąc") })
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Twoja frekwencja", style = MaterialTheme.typography.titleMedium)
                    Text(percentage?.let { String.format(Locale.forLanguageTag("pl-PL"), "%.1f%%", it) } ?: "—", style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold)
                    if (percentage != null) LinearProgressIndicator(progress = { (percentage / 100.0).toFloat() }, modifier = Modifier.fillMaxWidth(), trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.12f))
                    Text(if (counted == 0) "Brak lekcji do obliczenia frekwencji" else "Obecność na $present z $counted lekcji")
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                AttendanceStatus.entries.chunked(2).forEach { pair ->
                    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        pair.forEach { status ->
                            OutlinedCard(Modifier.weight(1f).fillMaxHeight()) {
                                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(entries.count { it.status == status }.toString(), style = MaterialTheme.typography.headlineSmall, color = status.color())
                                    Text(status.label(), style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                        if (pair.size == 1) OutlinedCard(Modifier.weight(1f).fillMaxHeight()) {
                            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(entries.size.toString(), style = MaterialTheme.typography.headlineSmall)
                                Text("Łącznie wpisów", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
        }
        item { Text("Według przedmiotu", style = MaterialTheme.typography.titleLarge) }
        if (groups.isEmpty()) item { Text("Brak zapisów w wybranym okresie.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        items(groups, key = { "subject-${it.first}" }) { (id, subjectEntries) ->
            val expanded = expandedSubject == id
            val subjectPercent = subjectEntries.attendancePercent()
            OutlinedCard(onClick = { expandedSubject = if (expanded) null else id }, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(names[id] ?: "Przedmiot bez ocen", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                        Text(subjectPercent?.let { String.format(Locale.forLanguageTag("pl-PL"), "%.1f%%", it) } ?: "—", fontWeight = FontWeight.Bold)
                        Text(if (expanded) "−" else "+")
                    }
                    if (subjectPercent != null) LinearProgressIndicator(progress = { (subjectPercent / 100.0).toFloat() }, modifier = Modifier.fillMaxWidth())
                    Text("${subjectEntries.size} zapisów", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (expanded) {
                        HorizontalDivider()
                        AttendanceStatus.entries.forEach { status ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(status.label())
                                Text(subjectEntries.count { it.status == status }.toString(), color = status.color(), fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
        item {
            Text("Historia obecności", style = MaterialTheme.typography.titleLarge)
            LazyRow(modifier = Modifier.testTag("attendance_filters"), horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(top = 8.dp)) {
                item { FilterChip(selected = selectedStatus == null, onClick = { selectedStatus = null }, label = { Text("Wszystkie") }) }
                items(AttendanceStatus.entries) { status ->
                    FilterChip(selected = selectedStatus == status.name, onClick = { selectedStatus = if (selectedStatus == status.name) null else status.name }, label = { Text(status.label()) })
                }
            }
        }
        if (history.isEmpty()) item { Text("Nie ma zapisów pasujących do tego filtra.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        items(history, key = { "entry-${it.studentId}-${it.id}" }) { entry ->
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(names[entry.subjectId] ?: "Przedmiot bez ocen", style = MaterialTheme.typography.titleSmall)
                        Text(entry.date.format(DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Locale.forLanguageTag("pl-PL"))), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(entry.status.entryLabel(), color = entry.status.color(), style = MaterialTheme.typography.labelLarge, modifier = Modifier.widthIn(max = 140.dp))
                }
                HorizontalDivider()
            }
        }
        item { Text("W danych demo spóźnienia liczą się jako obecność. Zwolnienia nie wpływają na procent frekwencji. Filtr historii nie zmienia podsumowania.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

private fun AttendanceStatus.label() = when (this) {
    AttendanceStatus.PRESENT -> "Obecności"
    AttendanceStatus.LATE -> "Spóźnienia"
    AttendanceStatus.ABSENT -> "Nieobecności"
    AttendanceStatus.EXCUSED_ABSENCE -> "Usprawiedliwione"
    AttendanceStatus.RELEASED -> "Zwolnienia"
}

private fun AttendanceStatus.entryLabel() = when (this) {
    AttendanceStatus.PRESENT -> "Obecność"
    AttendanceStatus.LATE -> "Spóźnienie"
    AttendanceStatus.ABSENT -> "Nieobecność"
    AttendanceStatus.EXCUSED_ABSENCE -> "Usprawiedliwiona"
    AttendanceStatus.RELEASED -> "Zwolnienie"
}

@Composable
private fun AttendanceStatus.color(): Color = when (this) {
    AttendanceStatus.PRESENT -> MaterialTheme.colorScheme.primary
    AttendanceStatus.LATE -> MaterialTheme.colorScheme.tertiary
    AttendanceStatus.ABSENT -> MaterialTheme.colorScheme.error
    AttendanceStatus.EXCUSED_ABSENCE -> MaterialTheme.colorScheme.secondary
    AttendanceStatus.RELEASED -> MaterialTheme.colorScheme.onSurfaceVariant
}
