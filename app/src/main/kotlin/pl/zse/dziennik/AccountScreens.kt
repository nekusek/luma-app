package pl.zse.dziennik

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import pl.zse.dziennik.model.*

@Composable
fun LoginScreen(profiles: List<DiaryAccount>, existing: List<DiaryAccount>, busy: Boolean, error: String?, onLogin: (String) -> Unit, onCancel: (() -> Unit)? = null, onEduVulcanLogin: (() -> Unit)? = null) {
    val available = profiles.filterNot { candidate -> existing.any { it.id == candidate.id } }
    var chosen by rememberSaveable { mutableStateOf<String?>(null) }
    val selected = available.firstOrNull { it.id == chosen } ?: available.firstOrNull()
    LazyColumn(Modifier.fillMaxSize().safeDrawingPadding(), contentPadding = PaddingValues(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        item {
            Icon(painterResource(R.drawable.ic_luma), null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(56.dp))
            Spacer(Modifier.height(20.dp))
            Text("Luma", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
            Text(if (onCancel == null) "Twój dziennik pod ręką." else "Dodaj kolejny profil", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = MaterialTheme.shapes.medium) {
                Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(painterResource(R.drawable.ic_demo), null)
                    Text("Tryb demo z fikcyjnymi danymi. Nie wymaga adresu e-mail ani hasła.", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        item { Text("Wybierz profil demo", style = MaterialTheme.typography.titleLarge) }
        if (available.isEmpty()) item { Text("Wszystkie profile demo zostały już dodane. Możesz przełączać je w sekcji Konta.") }
        items(available, key = { it.id }) { account ->
            Card(onClick = { chosen = account.id }, enabled = !busy, colors = CardDefaults.cardColors(containerColor = if (selected?.id == account.id) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow)) {
                Row(Modifier.fillMaxWidth().padding(18.dp), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    ProfileAvatar(account)
                    Column(Modifier.weight(1f)) {
                        Text(account.displayName, style = MaterialTheme.typography.titleMedium)
                        Text(account.className, style = MaterialTheme.typography.bodyMedium)
                        Text("Konto uczniowskie · demo", style = MaterialTheme.typography.labelMedium)
                    }
                    if (selected?.id == account.id) Icon(painterResource(R.drawable.ic_check), contentDescription = "Wybrany profil")
                }
            }
        }
        error?.let { message -> item { Text(message, color = MaterialTheme.colorScheme.error) } }
        item {
            Button(onClick = { selected?.let { onLogin(it.id) } }, enabled = selected != null && !busy, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                if (busy) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                else Icon(painterResource(R.drawable.ic_login), null)
                Spacer(Modifier.width(10.dp))
                Text(if (busy) "Logowanie…" else "Kontynuuj w trybie demo")
            }
        }
        item {
            HorizontalDivider()
            Row(Modifier.padding(top = 18.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(painterResource(R.drawable.ic_lock), null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Column {
                    Text("eduVULCAN", style = MaterialTheme.typography.titleSmall)
                    Text(if (onEduVulcanLogin == null) "Logowanie do prawdziwego dziennika nie jest jeszcze dostępne." else "Połącz konto dziennika.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item { OutlinedButton(onClick = { onEduVulcanLogin?.invoke() }, enabled = onEduVulcanLogin != null && !busy, modifier = Modifier.fillMaxWidth()) { Icon(painterResource(R.drawable.ic_lock), null); Spacer(Modifier.width(8.dp)); Text("Zaloguj przez eduVULCAN") } }
        if (onCancel != null) item { TextButton(onClick = onCancel, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("Wróć do dziennika") } }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountSheet(session: AccountSession, busy: Boolean, onSelect: (String) -> Unit, onAdd: () -> Unit, onRemove: (String) -> Unit, onDismiss: () -> Unit) {
    var removing by rememberSaveable { mutableStateOf<String?>(null) }
    ModalBottomSheet(onDismissRequest = { if (!busy) onDismiss() }) {
        LazyColumn(contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Text("Twoje konta", style = MaterialTheme.typography.headlineSmall)
                Text("Wybierz profil, którego dziennik chcesz otworzyć.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            items(session.accounts, key = { it.id }) { account ->
                OutlinedCard(onClick = { onSelect(account.id) }, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ProfileAvatar(account)
                        Column(Modifier.weight(1f)) {
                            Text(account.displayName, fontWeight = FontWeight.SemiBold)
                            Text(account.className, style = MaterialTheme.typography.bodyMedium)
                            Text(if (session.activeId == account.id) "Aktywne konto" else if (account.providerId == "demo") "Konto demo" else "Połączone konto", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        }
                        if (session.activeId == account.id) Icon(painterResource(R.drawable.ic_check), "Aktywne konto")
                        IconButton(onClick = { removing = account.id }, enabled = !busy) { Icon(painterResource(R.drawable.ic_logout), "Usuń konto ${account.displayName} z urządzenia") }
                    }
                }
            }
            item {
                FilledTonalButton(onClick = onAdd, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                    Icon(painterResource(R.drawable.ic_add_account), null)
                    Spacer(Modifier.width(8.dp))
                    Text("Dodaj konto")
                }
            }
        }
    }
    val target = session.accounts.firstOrNull { it.id == removing }
    if (target != null) AlertDialog(
        onDismissRequest = { removing = null },
        icon = { Icon(painterResource(R.drawable.ic_logout), null) },
        title = { Text("Usunąć konto z urządzenia?") },
        text = { Text("Konto ${target.displayName} oraz zapisane dane tego profilu zostaną usunięte z urządzenia.") },
        confirmButton = { TextButton(onClick = { removing = null; onRemove(target.id) }, enabled = !busy) { Text("Usuń konto") } },
        dismissButton = { TextButton(onClick = { removing = null }) { Text("Anuluj") } },
    )
}

@Composable
fun ProfileAvatar(account: DiaryAccount) {
    Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.secondaryContainer) {
        Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) {
            Text(account.displayName.split(' ').filter { it.isNotBlank() }.take(2).joinToString("") { it.take(1) }, fontWeight = FontWeight.Bold)
        }
    }
}
