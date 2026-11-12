package pl.zse.dziennik.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import java.time.Duration
import java.time.format.DateTimeFormatter
import java.util.Locale
import pl.zse.dziennik.model.*

private val polish = Locale.forLanguageTag("pl-PL")
private val timeFormat = DateTimeFormatter.ofPattern("HH:mm", polish).withZone(SchoolZone)
private val dateFormat = DateTimeFormatter.ofPattern("EEEE, d MMMM", polish).withZone(SchoolZone)
private fun Double.display() = String.format(polish, "%.1f", this)

@Composable
fun DashboardRoute(viewModel: DashboardViewModel, modifier: Modifier = Modifier) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val now by viewModel.now.collectAsStateWithLifecycle()
    DashboardScreen(state.copy(now = now), viewModel::refresh, modifier)
}

@Composable
fun DashboardScreen(state: DashboardUiState, onRefresh: () -> Unit, modifier: Modifier = Modifier) {
    val data = state.snapshot
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Text("ZSE / RZESZÓW", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontFamily = FontFamily.Monospace)
            Spacer(Modifier.height(8.dp))
            Text("Pulpit", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            Text(dateFormat.format(state.now).replaceFirstChar { it.titlecase(polish) }, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(data?.student?.className ?: "Brak zapisanych danych", modifier = Modifier.padding(top = 12.dp))
                TextButton(onClick = onRefresh, enabled = !state.refreshing) { Text(if (state.refreshing) "Odświeżanie…" else "Odśwież") }
            }
        }
        item {
            Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = MaterialTheme.shapes.small) {
                Text("TRYB DEMO · fikcyjne dane", Modifier.padding(horizontal = 12.dp, vertical = 8.dp), style = MaterialTheme.typography.labelMedium)
            }
        }
        state.error?.let { message -> item { Text(message, color = MaterialTheme.colorScheme.error) } }
        if (data == null) {
            item { Tile("Dane lokalne") { Text(if (state.refreshing) "Przygotowywanie pierwszego zestawu…" else "Nie ma jeszcze danych. Wybierz Odśwież.") } }
        } else {
            val next = data.timetable.currentOrNextLesson(state.now)
            item {
                Tile(if (next != null && next.startsAt <= state.now) "Teraz" else "Następna lekcja", emphasized = true) {
                    if (next == null) {
                        Text("Na dziś to wszystko", style = MaterialTheme.typography.headlineSmall)
                        Text("Brak kolejnych lekcji w zapisanym planie.")
                    } else {
                        if (next.startsAt.atZone(SchoolZone).toLocalDate() != state.now.atZone(SchoolZone).toLocalDate()) {
                            Text(dateFormat.format(next.startsAt), style = MaterialTheme.typography.labelLarge)
                        }
                        Text(next.subjectName, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text("${timeFormat.format(next.startsAt)}–${timeFormat.format(next.endsAt)} · sala ${next.room ?: "—"}${next.group?.let { " · grupa $it" } ?: ""}")
                        val active = next.startsAt <= state.now
                        val seconds = Duration.between(state.now, if (active) next.endsAt else next.startsAt).seconds.coerceAtLeast(0)
                        Text("${if (active) "Do końca" else "Za"} ${seconds / 60} min ${seconds % 60} s", fontFamily = FontFamily.Monospace, modifier = Modifier.padding(top = 12.dp))
                        next.changeDescription?.let { Text(it, style = MaterialTheme.typography.labelLarge) }
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.height(IntrinsicSize.Min)) {
                    Tile("Frekwencja", Modifier.weight(1f).fillMaxHeight()) {
                        Text(data.attendance.attendancePercent()?.let { "${it.display()}%" } ?: "—", style = MaterialTheme.typography.headlineMedium)
                        Text("Zapisane lekcje", style = MaterialTheme.typography.bodySmall)
                    }
                    Tile("Dzisiaj", Modifier.weight(1f).fillMaxHeight()) {
                        Text("${data.timetable.count { it.startsAt.atZone(SchoolZone).toLocalDate() == state.now.atZone(SchoolZone).toLocalDate() && it.status != LessonStatus.CANCELLED && it.status != LessonStatus.WINDOW }}", style = MaterialTheme.typography.headlineMedium)
                        Text("Aktywne lekcje", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            item { Text("Ostatnie oceny", style = MaterialTheme.typography.titleMedium) }
            items(data.grades.sortedByDescending { it.receivedOn }.take(3), key = { it.id }) { grade ->
                Tile(grade.subjectName) {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        val gradeColor = when {
                            grade.numericValue == null -> MaterialTheme.colorScheme.surfaceContainerHighest
                            grade.numericValue!! < 3 -> MaterialTheme.colorScheme.errorContainer
                            grade.numericValue!! >= 5 -> MaterialTheme.colorScheme.primaryContainer
                            else -> MaterialTheme.colorScheme.tertiaryContainer
                        }
                        Surface(color = gradeColor, shape = MaterialTheme.shapes.small) {
                            Text(grade.displayValue, Modifier.padding(12.dp), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        }
                        Column {
                            Text(grade.category, fontWeight = FontWeight.Medium)
                            Text("Waga ${grade.weight} · ${grade.receivedOn}", style = MaterialTheme.typography.bodySmall)
                            if (grade.description.isNotBlank()) Text(grade.description, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
            item {
                Tile("Najbliższy sprawdzian") {
                    val assessment = data.assessments.filter { it.date >= state.now.atZone(SchoolZone).toLocalDate() }.minByOrNull { it.date }
                    Text(assessment?.subject ?: "Brak zaplanowanych sprawdzianów", fontWeight = FontWeight.SemiBold)
                    assessment?.let { Text("${it.date} · ${it.title}") }
                }
            }
            item { Text("Zapisano ${timeFormat.format(data.syncedAt)} · ${data.syncedAt.atZone(SchoolZone).toLocalDate()} · dostępne offline", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}

@Composable
private fun Tile(title: String, modifier: Modifier = Modifier, emphasized: Boolean = false, content: @Composable ColumnScope.() -> Unit) {
    Card(modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = if (emphasized) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.labelLarge, color = if (emphasized) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant)
            content()
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun EmptyDashboardPreview() { MaterialTheme { DashboardScreen(DashboardUiState(now = java.time.Instant.parse("2026-11-12T07:45:00Z")), {}) } }
