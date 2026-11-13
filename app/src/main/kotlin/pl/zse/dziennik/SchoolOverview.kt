package pl.zse.dziennik

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import pl.zse.dziennik.model.*

@Composable
fun SchoolOverview(data: DiaryFeatures, today: LocalDate, onOpen: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        data.luckyNumber?.let { number -> Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
            Column(Modifier.padding(16.dp)) {
                Text("Szczęśliwy numerek · demo", style = MaterialTheme.typography.titleSmall)
                Text(number.toString(), style = MaterialTheme.typography.headlineLarge)
                Text("${data.luckyDate} · Twój numer: ${data.pupilNumber ?: "—"}")
                if (data.luckyDate == today && data.pupilNumber == number) Text("Dziś Twój numerek!", style = MaterialTheme.typography.labelLarge)
            }
        } }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { onOpen(6) }) { Text("Kalendarz i zadania") }
            OutlinedButton(onClick = { onOpen(7) }) { Text("Uwagi i pochwały") }
        }
        val next = data.tasks.filter { it.kind == SchoolTaskKind.HOMEWORK && it.id !in data.completedHomework && it.dueOn >= today }.minByOrNull { it.dueOn }
        next?.let { task -> OutlinedCard(onClick = { onOpen(6) }, modifier = Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) { Text("Najbliższe zadanie", style = MaterialTheme.typography.labelLarge); Text(task.title, style = MaterialTheme.typography.titleMedium); Text("${task.subject} · ${task.dueOn}") } } }
    }
}
