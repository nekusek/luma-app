package pl.zse.dziennik

import android.content.ActivityNotFoundException
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import pl.zse.dziennik.dashboard.DashboardUiState
import pl.zse.dziennik.model.*

@Composable
fun DemoSection(destination: Int, state: DashboardUiState, theme: ThemeMode, onTheme: (ThemeMode) -> Unit, modifier: Modifier = Modifier, onError: (String) -> Unit = {}, showAssessments: Boolean = false, onShowAssessments: (Boolean) -> Unit = {}, onAccounts: () -> Unit = {}, onAttendance: (() -> Unit)? = null, features: DiaryFeatures = DiaryFeatures(), options: DiaryOptions = DiaryOptions(), onOpenSection: (Int) -> Unit = {}) {
    val data = state.snapshot
    if (destination == 1) { GradesScreen(data?.grades.orEmpty(), state.now.atZone(SchoolZone).toLocalDate(), data?.student?.name.orEmpty(), onAccounts, modifier, features, options.gradePolicy); return }
    Column(modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(when (destination) { 2 -> "Plan lekcji"; 3 -> "Frekwencja"; else -> "Więcej" }, style = MaterialTheme.typography.headlineLarge)
        Text("DEMO · dane fikcyjne", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        if (destination != 4 && data == null) { Text("Brak zapisanych danych. Odśwież Pulpit."); return@Column }
        when (destination) {
            2 -> TimetableScreen(data!!.timetable, state.now.atZone(SchoolZone).toLocalDate())
            3 -> AttendanceScreen(data!!.attendance, data.grades, state.now.atZone(SchoolZone).toLocalDate(), Modifier.weight(1f))
            4 -> {
                val uri = LocalUriHandler.current
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    item { MoreCard("Konta i profile", "Przełącz konto lub dodaj nowy profil", R.drawable.ic_switch_account, onAccounts) }
                    if (onAttendance != null) {
                        item { MoreCard("Frekwencja", "Podsumowanie i historia obecności", R.drawable.ic_attendance, onAttendance) }
                        item { MoreCard("Kalendarz i zadania", "Sprawdziany, zadania i terminy", R.drawable.ic_timetable) { onOpenSection(6) } }
                        item { MoreCard("Uwagi i pochwały", "Wpisy nauczycieli, kategorie i punkty", R.drawable.ic_notes) { onOpenSection(7) } }
                    }
                    item { MoreCard("Usprawiedliwienia", "Wnioski rodzica i decyzje nauczyciela demo", R.drawable.ic_check) { onOpenSection(8) } }
                    item { MoreCard("Ustawienia dziennika", "Wygląd, średnie, odczyty i synchronizacja", R.drawable.ic_palette) { onOpenSection(9) } }
                    item { OutlinedButton(onClick = {
                        try { uri.openUri("https://zse.rzeszow.pl/") }
                        catch (_: ActivityNotFoundException) { onError("Nie znaleziono przeglądarki.") }
                        catch (_: IllegalArgumentException) { onError("Nie udało się otworzyć strony.") }
                    }) { Text("zse.rzeszow.pl ↗") } }
                }
            }
        }
    }
}
@Composable
private fun MoreCard(title: String, subtitle: String, icon: Int, onClick: () -> Unit) { OutlinedCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) { Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) { Icon(painterResource(icon), null); Column { Text(title, style = MaterialTheme.typography.titleMedium); Text(subtitle, style = MaterialTheme.typography.bodySmall) } } } }
