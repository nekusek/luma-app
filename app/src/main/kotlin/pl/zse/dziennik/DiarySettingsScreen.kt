package pl.zse.dziennik

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import pl.zse.dziennik.model.*

@Composable
fun DiarySettingsScreen(state: FeaturesUiState, theme: ThemeMode, onTheme: (ThemeMode) -> Unit, onPolicy: (GradePolicy) -> Unit, onReceipts: (Boolean) -> Unit, onSync: (Int, Boolean) -> Unit, onRefresh: () -> Unit, modifier: Modifier = Modifier) {
    val options = state.options
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { Text("Ustawienia dziennika", style = MaterialTheme.typography.headlineMedium) }
        state.error?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
        item { Text("Wygląd", style = MaterialTheme.typography.titleLarge); FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { ThemeMode.entries.forEach { mode -> FilterChip(selected = theme == mode, onClick = { onTheme(mode) }, label = { Text(when (mode) { ThemeMode.SYSTEM -> "System"; ThemeMode.LIGHT -> "Jasny"; ThemeMode.DARK -> "Ciemny"; ThemeMode.AMOLED -> "AMOLED" }) }) } } }
        item {
            Text("Średnie ocen", style = MaterialTheme.typography.titleLarge)
            Row(verticalAlignment = Alignment.CenterVertically) { Switch(options.gradePolicy.useModifiers, { onPolicy(options.gradePolicy.copy(useModifiers = it)) }, enabled = !state.busy); Text("Własne wartości + i −") }
            Text("To lokalne obliczenia. Nie zmieniają ocen ani zasad klasyfikacji szkoły.", style = MaterialTheme.typography.bodySmall)
            if (options.gradePolicy.useModifiers) {
                Text("Wartość plusa")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf(0.0, 0.25, 0.5, 0.75, 1.0).forEach { value -> FilterChip(selected = options.gradePolicy.plus == value, onClick = { onPolicy(options.gradePolicy.copy(plus = value)) }, enabled = !state.busy, label = { Text("+$value") }) } }
                Text("Wartość minusa")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf(0.0, 0.25, 0.5, 0.75, 1.0).forEach { value -> FilterChip(selected = options.gradePolicy.minus == value, onClick = { onPolicy(options.gradePolicy.copy(minus = value)) }, enabled = !state.busy, label = { Text("−$value") }) } }
            }
        }
        item { HorizontalDivider(); Text("Wiadomości", style = MaterialTheme.typography.titleLarge); Row(verticalAlignment = Alignment.CenterVertically) { Switch(options.readReceipts, onReceipts, enabled = !state.busy); Text("Oznaczaj wiadomości jako przeczytane") }; Text("Wyłączenie dotyczy kolejnych otwieranych wiadomości w demo. Nie cofa wcześniejszych odczytów.", style = MaterialTheme.typography.bodySmall) }
        item {
            HorizontalDivider(); Text("Odświeżanie w tle", style = MaterialTheme.typography.titleLarge)
            Text("WorkManager odświeża wyłącznie lokalne źródło demo. Android może opóźnić uruchomienie zadania.", style = MaterialTheme.typography.bodySmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf(0 to "Wyłączone", 15 to "15 min", 60 to "1 godz.", 240 to "4 godz.", 1440 to "24 godz.").forEach { (minutes, label) -> FilterChip(selected = options.syncMinutes == minutes, onClick = { onSync(minutes, options.wifiOnly) }, enabled = !state.busy, label = { Text(label) }) } }
            Row(verticalAlignment = Alignment.CenterVertically) { Switch(options.wifiOnly, { onSync(options.syncMinutes, it) }, enabled = !state.busy); Text("Tylko Wi-Fi / sieć bez limitu") }
            OutlinedButton(onClick = onRefresh, enabled = !state.busy) { Text("Odśwież dane demo teraz") }
        }
        item { HorizontalDivider(); Text("Luma 0.2.0", style = MaterialTheme.typography.titleLarge); Text("Natywna aplikacja Android · projekt niezależny od szkoły i VULCAN. Inspiracja funkcjami Vulcanovy, własna implementacja Kotlin. Kod Luma: MIT.", style = MaterialTheme.typography.bodyMedium); Text("Produkcja eduVULCAN: logowanie i synchronizacja sieciowa nie są jeszcze aktywne.", style = MaterialTheme.typography.bodySmall) }
    }
}
