package pl.zse.dziennik

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import pl.zse.dziennik.model.*

@Composable
fun TeacherScreen(destination: Int, viewModel: WorkspaceViewModel, state: WorkspaceUiState, onNavigate: (Int) -> Unit, onAccounts: () -> Unit, modifier: Modifier = Modifier) {
    val students = viewModel.contacts.filter { it.role == AccountRole.STUDENT }
    val classes = students.map { it.className }.distinct()
    var chosenClass by rememberSaveable { mutableStateOf(classes.first()) }
    val classStudents = students.filter { it.className == chosenClass }
    var chosenStudent by rememberSaveable { mutableStateOf<String?>(null) }
    val student = classStudents.firstOrNull { it.studentId == chosenStudent } ?: classStudents.first()
    var grade by rememberSaveable { mutableIntStateOf(5) }
    var weight by rememberSaveable { mutableIntStateOf(1) }
    var description by rememberSaveable { mutableStateOf("") }
    var topic by rememberSaveable { mutableStateOf("") }
    var homework by rememberSaveable { mutableStateOf("") }
    var due by rememberSaveable { mutableStateOf(viewModel.today.plusDays(1).toString()) }
    val dueDate = runCatching { LocalDate.parse(due) }.getOrNull()
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Row {
                Text(when (destination) { 1 -> "Dziennik nauczyciela"; 2 -> "Plan nauczyciela"; else -> "Pulpit nauczyciela" }, Modifier.weight(1f), style = MaterialTheme.typography.headlineMedium)
                IconButton(onClick = onAccounts) { androidx.compose.material3.Icon(androidx.compose.ui.res.painterResource(R.drawable.ic_switch_account), "Konta: ${viewModel.account.displayName}") }
            }
            Text(viewModel.account.displayName, style = MaterialTheme.typography.titleMedium)
            Text("DEMO · wpisy i wiadomości działają tylko lokalnie. Bez połączenia z eduVULCAN.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        state.error?.let { item { Text(it, color = MaterialTheme.colorScheme.error); TextButton(onClick = viewModel::retry) { Text("Spróbuj ponownie") } } }
        if (destination == 0) {
            item { TeacherCard("Dziennik klas", "Obecności, oceny, tematy lekcji i zadania", { onNavigate(1) }) }
            item { TeacherCard("Wiadomości", "Nieprzeczytane: ${state.messages.count { it.recipientId == viewModel.account.id && !it.read }}", { onNavigate(5) }) }
            item { TeacherCard("Plan zajęć", "Dwie przykładowe grupy laboratoryjne", { onNavigate(2) }) }
            item { Text("Wpisy nauczyciela: ${state.entries.size}", style = MaterialTheme.typography.titleMedium) }
        }
        if (destination == 2) {
            item { Text("Przykładowy dzień zajęć · ${viewModel.today}", style = MaterialTheme.typography.titleMedium) }
            classes.forEachIndexed { index, name -> item { OutlinedCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) { Text(if (index == 0) "08:00–08:45" else "08:55–09:40", style = MaterialTheme.typography.titleMedium); Text("Programowanie · $name"); Text("Sala L12 · laboratorium"); TextButton(onClick = { chosenClass = name; onNavigate(1) }) { Text("Otwórz dziennik klasy") } } } } }
        }
        if (destination == 1) {
            item {
                Text("Klasa / grupa", style = MaterialTheme.typography.titleMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { classes.forEach { name -> FilterChip(selected = chosenClass == name, onClick = { chosenClass = name; chosenStudent = null }, label = { Text(name) }) } }
                Text("Programowanie · ${viewModel.today}", style = MaterialTheme.typography.bodyMedium)
            }
            item { Text("Obecność", style = MaterialTheme.typography.titleLarge) }
            classStudents.forEach { pupil -> item {
                OutlinedCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) {
                    Text(pupil.displayName, style = MaterialTheme.typography.titleMedium)
                    val attendance = state.entries.firstOrNull { it.studentId == pupil.studentId && it.kind == TeachingKind.ATTENDANCE && it.date == viewModel.today }?.value
                    Text(if (attendance == null) "Obecność jeszcze niesprawdzona" else "Zapisano: ${AttendanceStatus.valueOf(attendance).teacherLabel()}", style = MaterialTheme.typography.bodySmall)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) { AttendanceStatus.entries.forEach { status -> FilterChip(selected = attendance == status.name, onClick = { viewModel.save(pupil.studentId, chosenClass, TeachingKind.ATTENDANCE, status.name) }, enabled = !state.busy, label = { Text(status.teacherLabel()) }) } }
                } }
            } }
            item {
                Text("Dodaj ocenę", style = MaterialTheme.typography.titleLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { classStudents.forEach { pupil -> FilterChip(selected = student.studentId == pupil.studentId, onClick = { chosenStudent = pupil.studentId }, label = { Text(pupil.displayName) }) } }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { (1..6).forEach { value -> FilterChip(selected = grade == value, onClick = { grade = value }, label = { Text("$value") }) } }
                Text("Waga oceny")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { (1..5).forEach { value -> FilterChip(selected = weight == value, onClick = { weight = value }, label = { Text("Waga $value") }) } }
                OutlinedTextField(description, { description = it.take(1000) }, Modifier.fillMaxWidth(), label = { Text("Opis oceny") }, enabled = !state.busy)
                Button(onClick = { viewModel.save(student.studentId, chosenClass, TeachingKind.GRADE, grade.toString(), description, weight, onSuccess = { description = "" }) }, enabled = !state.busy && description.isNotBlank()) { Text("Zapisz ocenę demo") }
            }
            item {
                Text("Temat lekcji", style = MaterialTheme.typography.titleLarge)
                OutlinedTextField(topic, { topic = it.take(1000) }, Modifier.fillMaxWidth(), label = { Text("Temat zajęć") }, enabled = !state.busy)
                Button(onClick = { viewModel.save(null, chosenClass, TeachingKind.TOPIC, topic, onSuccess = { topic = "" }) }, enabled = !state.busy && topic.isNotBlank()) { Text("Zapisz temat") }
            }
            item {
                Text("Zadanie domowe", style = MaterialTheme.typography.titleLarge)
                OutlinedTextField(homework, { homework = it.take(1000) }, Modifier.fillMaxWidth(), label = { Text("Treść zadania") }, enabled = !state.busy)
                OutlinedTextField(due, { due = it.take(10) }, Modifier.fillMaxWidth(), label = { Text("Termin (RRRR-MM-DD)") }, singleLine = true, isError = dueDate == null || dueDate < viewModel.today, enabled = !state.busy)
                Button(onClick = { dueDate?.let { viewModel.save(null, chosenClass, TeachingKind.HOMEWORK, homework, date = it, onSuccess = { homework = "" }) } }, enabled = !state.busy && homework.isNotBlank() && dueDate != null && dueDate >= viewModel.today) { Text("Dodaj zadanie demo") }
            }
        }
        if (destination != 2) {
            item { HorizontalDivider(); Text("Ostatnie wpisy demo", style = MaterialTheme.typography.titleLarge) }
            val entries = if (destination == 1) state.entries.filter { it.className == chosenClass } else state.entries
            if (entries.isEmpty()) item { Text("Brak wpisów. Otwórz dziennik klasy, aby rozpocząć.") }
            entries.take(20).forEach { entry -> item(key = entry.id) {
                OutlinedCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("${entry.kind.teacherLabel()} · ${entry.className}", style = MaterialTheme.typography.titleSmall)
                    entry.studentId?.let { id -> Text(students.firstOrNull { it.studentId == id }?.displayName.orEmpty()) }
                    Text(if (entry.kind == TeachingKind.ATTENDANCE) AttendanceStatus.valueOf(entry.value).teacherLabel() else entry.value)
                    if (entry.description.isNotBlank()) Text(entry.description)
                    Text("${entry.date}${if (entry.kind == TeachingKind.GRADE) " · waga ${entry.weight}" else ""}", style = MaterialTheme.typography.bodySmall)
                } }
            } }
        }
    }
}

@Composable
private fun TeacherCard(title: String, description: String, onClick: () -> Unit) { OutlinedCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) { Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) { Text(title, style = MaterialTheme.typography.titleLarge); Text(description) } } }
private fun AttendanceStatus.teacherLabel() = when (this) { AttendanceStatus.PRESENT -> "Obecność"; AttendanceStatus.LATE -> "Spóźnienie"; AttendanceStatus.ABSENT -> "Nieobecność"; AttendanceStatus.EXCUSED_ABSENCE -> "Usprawiedliwiona"; AttendanceStatus.RELEASED -> "Zwolnienie" }
private fun TeachingKind.teacherLabel() = when (this) { TeachingKind.GRADE -> "Ocena"; TeachingKind.ATTENDANCE -> "Frekwencja"; TeachingKind.TOPIC -> "Temat"; TeachingKind.HOMEWORK -> "Zadanie" }
