package pl.zse.dziennik

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import pl.zse.dziennik.model.*

@Composable
fun JustificationsScreen(account: DiaryAccount, attendance: List<Attendance>, state: FeaturesUiState, onSubmit: (List<String>, String, () -> Unit) -> Unit, onDecide: (String, Boolean) -> Unit, contacts: List<DiaryAccount>, modifier: Modifier = Modifier, subjectNames: Map<String, String> = emptyMap()) {
    var selected by rememberSaveable { mutableStateOf(emptyList<String>()) }
    var reason by rememberSaveable { mutableStateOf("") }
    var confirm by rememberSaveable { mutableStateOf(false) }
    val requests = if (account.role == AccountRole.TEACHER) state.pending else state.data.justifications
    val used = requests.filter { it.status != JustificationStatus.REJECTED }.flatMap { it.attendanceIds }.toSet()
    val eligible = attendance.filter { it.status == AttendanceStatus.ABSENT && it.id !in used }.sortedByDescending { it.date }
    val selection = selected.filter { id -> eligible.any { it.id == id } }
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { Text("Usprawiedliwienia", style = MaterialTheme.typography.headlineMedium); Text("DEMO · wnioski nie trafiają do szkoły ani eduVULCAN. Decyzję podejmuje nauczyciel demo na tym urządzeniu.", style = MaterialTheme.typography.bodySmall) }
        state.error?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
        if (account.role == AccountRole.PARENT) {
            item { Text("Wybierz nieobecności", style = MaterialTheme.typography.titleLarge) }
            if (eligible.isEmpty()) item { Text("Brak nieobecności do usprawiedliwienia.") }
            items(eligible, key = { it.id }) { entry -> Row {
                Checkbox(modifier = Modifier.testTag("absence:${entry.id}"), checked = entry.id in selection, onCheckedChange = { checked -> selected = if (checked) selection + entry.id else selection - entry.id }, enabled = !state.busy)
                Text("${entry.date} · ${subjectNames[entry.subjectId] ?: "Zajęcia"}", Modifier.padding(top = 12.dp))
            } }
            item {
                OutlinedTextField(reason, { reason = it.take(1000) }, Modifier.fillMaxWidth(), label = { Text("Powód nieobecności") }, minLines = 3, enabled = !state.busy)
                Button(onClick = { confirm = true }, enabled = !state.busy && selection.isNotEmpty() && reason.trim().length >= 5) { Text("Złóż wniosek demo") }
            }
        } else if (account.role == AccountRole.STUDENT) item { Text("Wniosek może złożyć konto rodzica demo. Dodaj profil Marii Kowalskiej w sekcji Konta. Uczeń widzi status swoich wniosków.") }
        item { Text(if (account.role == AccountRole.TEACHER) "Do rozpatrzenia" else "Historia wniosków", style = MaterialTheme.typography.titleLarge) }
        if (requests.isEmpty()) item { Text("Brak wniosków.") }
        items(requests, key = { it.id }) { request ->
            OutlinedCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(contacts.firstOrNull { it.studentId == request.studentId && it.role == AccountRole.STUDENT }?.displayName ?: "Uczeń demo", style = MaterialTheme.typography.titleMedium)
                Text("Liczba nieobecności: ${request.attendanceIds.size}")
                Text(request.reason)
                Text(when (request.status) { JustificationStatus.PENDING -> "Oczekuje na decyzję"; JustificationStatus.ACCEPTED -> "Zaakceptowany"; JustificationStatus.REJECTED -> "Odrzucony" }, color = MaterialTheme.colorScheme.primary)
                if (account.role == AccountRole.TEACHER) FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { onDecide(request.id, true) }, enabled = !state.busy) { Text("Zaakceptuj") }
                    OutlinedButton(onClick = { onDecide(request.id, false) }, enabled = !state.busy) { Text("Odrzuć") }
                }
            } }
        }
    }
    if (confirm) AlertDialog(onDismissRequest = { confirm = false }, title = { Text("Złożyć lokalny wniosek demo?") }, text = { Text("Wybrane nieobecności: ${selection.size}. Wniosek będzie czekał na nauczyciela demo. Nie zostanie wysłany do szkoły.") }, confirmButton = { TextButton(onClick = { confirm = false; onSubmit(selection, reason) { selected = emptyList(); reason = "" } }, enabled = !state.busy) { Text("Potwierdź wniosek") } }, dismissButton = { TextButton(onClick = { confirm = false }) { Text("Anuluj") } })
}
