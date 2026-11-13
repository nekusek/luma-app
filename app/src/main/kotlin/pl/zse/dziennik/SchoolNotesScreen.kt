package pl.zse.dziennik

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import pl.zse.dziennik.model.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SchoolNotesScreen(notes: List<SchoolNote>, modifier: Modifier = Modifier) {
    var filter by rememberSaveable { mutableIntStateOf(0) }
    var selected by rememberSaveable { mutableStateOf<String?>(null) }
    val entries = notes.filter { when (filter) { 1 -> it.positive == true; 2 -> it.positive == false; else -> true } }.sortedByDescending { it.date }
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Uwagi i pochwały", style = MaterialTheme.typography.headlineMedium); Text("DEMO · dane fikcyjne", style = MaterialTheme.typography.labelMedium) }
        item { FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("Wszystkie", "Pochwały", "Uwagi").forEachIndexed { i, label -> FilterChip(selected = filter == i, onClick = { filter = i }, label = { Text(label) }) } } }
        item { Text("Punkty w wybranym widoku: ${entries.sumOf { it.points ?: 0 }}", style = MaterialTheme.typography.titleMedium) }
        if (entries.isEmpty()) item { Text("Brak wpisów w tym widoku.") }
        items(entries, key = { it.id }) { note ->
            OutlinedCard(onClick = { selected = note.id }, modifier = Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(note.category, style = MaterialTheme.typography.titleMedium, color = if (note.positive == false) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
                Text(note.body, maxLines = 3, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                Text("${note.date} · ${note.teacher}", style = MaterialTheme.typography.bodySmall)
                note.points?.let { Text("Punkty: ${if (it > 0) "+" else ""}$it", style = MaterialTheme.typography.labelLarge) }
            } }
        }
    }
    notes.firstOrNull { it.id == selected }?.let { note -> ModalBottomSheet(onDismissRequest = { selected = null }) {
        LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { Text(note.category, style = MaterialTheme.typography.headlineSmall) }
            item { Text(note.body); Text("Autor: ${note.teacher}\nData: ${note.date}"); note.points?.let { Text("Punkty: $it") } }
            item { TextButton(onClick = { selected = null }) { Text("Zamknij uwagę") } }
        }
    } }
}
