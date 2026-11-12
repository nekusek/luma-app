package pl.zse.dziennik

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import pl.zse.dziennik.model.*

private fun LocalDate.schoolYear() = year - if (monthValue < 9) 1 else 0
private fun Double.gradeFormat() = String.format(Locale.forLanguageTag("pl-PL"), "%.2f", this)

@Composable
fun GradesScreen(grades: List<Grade>, today: LocalDate, studentName: String, onAccounts: () -> Unit, modifier: Modifier = Modifier) {
    val years = (grades.map { it.receivedOn.schoolYear() } + today.schoolYear()).distinct().sorted()
    var selectedYear by rememberSaveable { mutableIntStateOf(today.schoolYear()) }
    val year = selectedYear.takeIf { it in years } ?: years.last()
    val index = years.indexOf(year)
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var selectedSubject by rememberSaveable { mutableStateOf<String?>(null) }
    val groups = grades.filter { it.receivedOn.schoolYear() == year }.groupBy { it.subjectId }.values.sortedBy { it.first().subjectName.lowercase(Locale.forLanguageTag("pl-PL")) }
    Column(modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Spacer(Modifier.size(44.dp))
            Text("Oceny", Modifier.weight(1f), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            FilledTonalIconButton(onClick = onAccounts, modifier = Modifier.size(44.dp).semantics { contentDescription = "Konta: $studentName" }, shape = CircleShape) {
                Text(studentName.split(' ').filter { it.isNotBlank() }.take(2).joinToString("") { it.take(1) }.ifBlank { "L" }, style = MaterialTheme.typography.labelLarge)
            }
        }
        PrimaryTabRow(selectedTabIndex = tab) {
            listOf("Oceny częściowe", "Oceny śródroczne").forEachIndexed { i, title ->
                Tab(selected = tab == i, onClick = { tab = i; selectedSubject = null }, text = { Text(title, color = if (tab == i) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface) })
            }
        }
        Surface(Modifier.fillMaxWidth().padding(16.dp), shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
            Row(Modifier.padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { selectedYear = years[index - 1]; selectedSubject = null }, enabled = index > 0) { Icon(painterResource(R.drawable.ic_chevron_left), "Poprzedni rok szkolny") }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("$year/${year + 1}", style = MaterialTheme.typography.titleLarge)
                    Text("Rok szkolny · demo", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = { selectedYear = years[index + 1]; selectedSubject = null }, enabled = index < years.lastIndex) { Icon(painterResource(R.drawable.ic_chevron_right), "Następny rok szkolny") }
            }
        }
        if (tab == 1) {
            Text("Oceny śródroczne nie są dostępne w danych demo.", Modifier.padding(24.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = 24.dp)) {
                if (groups.isEmpty()) item { Text("Nie ma jeszcze ocen w tym roku szkolnym.", Modifier.padding(24.dp)) }
                items(groups, key = { it.first().subjectId }) { entries ->
                    Surface(onClick = { selectedSubject = entries.first().subjectId }, color = MaterialTheme.colorScheme.background) {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(entries.first().subjectName, style = MaterialTheme.typography.titleLarge)
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    entries.sortedBy { it.receivedOn }.forEach { Text(it.displayValue, color = gradeColor(it), style = MaterialTheme.typography.titleLarge) }
                                }
                            }
                            Text(entries.weightedAverage()?.gradeFormat() ?: "—", style = MaterialTheme.typography.titleMedium)
                            Icon(painterResource(R.drawable.ic_chevron_right), "Szczegóły: ${entries.first().subjectName}", Modifier.size(28.dp))
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                }
                if (groups.isNotEmpty()) item { Text("Średnie ważone obliczone w aplikacji", Modifier.padding(20.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        }
    }
    groups.firstOrNull { it.first().subjectId == selectedSubject }?.let { entries ->
        GradeDetails(entries, onDismiss = { selectedSubject = null })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GradeDetails(entries: List<Grade>, onDismiss: () -> Unit) {
    var simulate by rememberSaveable(entries.first().subjectId) { mutableStateOf(false) }
    var value by rememberSaveable(entries.first().subjectId) { mutableFloatStateOf(5f) }
    var weight by rememberSaveable(entries.first().subjectId) { mutableFloatStateOf(2f) }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        LazyColumn(Modifier.fillMaxWidth(), contentPadding = PaddingValues(bottom = 28.dp)) {
            item {
                Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(entries.first().subjectName, Modifier.weight(1f), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                    FilledTonalIconButton(onClick = onDismiss, shape = CircleShape) { Icon(painterResource(R.drawable.ic_close), "Zamknij szczegóły ocen") }
                }
                Row(Modifier.padding(20.dp), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    GradeMetric("ŚREDNIA", entries.weightedAverage()?.gradeFormat() ?: "—")
                    VerticalDivider(Modifier.height(44.dp))
                    GradeMetric("LICZBA OCEN", entries.size.toString())
                }
            }
            items(entries.sortedByDescending { it.receivedOn }, key = { it.id }) { grade ->
                Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp), horizontalArrangement = Arrangement.spacedBy(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(grade.displayValue, Modifier.widthIn(min = 40.dp), style = MaterialTheme.typography.headlineLarge, color = gradeColor(grade))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(grade.description.ifBlank { grade.category }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text("${grade.category} · waga: ${grade.weight}", style = MaterialTheme.typography.bodyMedium)
                        Text("Data: ${grade.receivedOn.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (!grade.countsTowardsAverage || grade.numericValue == null || grade.weight == 0) Text("Nie wlicza się do średniej", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            }
            item {
                Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { simulate = !simulate }) { Text(if (simulate) "Ukryj symulator" else "Symulator średniej") }
                    if (simulate) {
                        Text("Ocena ${value.toInt()} · waga ${weight.toInt()}")
                        Slider(value = value, onValueChange = { value = it }, valueRange = 1f..6f, steps = 4)
                        Slider(value = weight, onValueChange = { weight = it }, valueRange = 1f..5f, steps = 3)
                        val simulated = entries + entries.first().copy(id = "simulation", numericValue = value.toInt().toDouble(), weight = weight.toInt(), countsTowardsAverage = true)
                        Text("Po dodaniu: ${simulated.weightedAverage()?.gradeFormat()}", style = MaterialTheme.typography.titleMedium)
                        Text("Symulacja nie zmienia zapisanych ocen.", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun GradeMetric(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun gradeColor(grade: Grade): Color {
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    return when (grade.numericValue?.toInt()) {
        1, 2 -> if (dark) Color(0xFFFFB4AB) else Color(0xFFBA1A1A)
        3 -> if (dark) Color(0xFFFFD180) else Color(0xFF825500)
        4 -> if (dark) Color(0xFFA8C7FA) else Color(0xFF245DA8)
        5 -> if (dark) Color(0xFF80DAB5) else Color(0xFF006C4C)
        6 -> if (dark) Color(0xFFD6BAFF) else Color(0xFF7843B5)
        else -> MaterialTheme.colorScheme.onSurface
    }
}
