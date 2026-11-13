package pl.zse.dziennik

import androidx.activity.compose.BackHandler
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
import java.time.format.DateTimeFormatter
import pl.zse.dziennik.model.*

@Composable
fun MessagesScreen(account: DiaryAccount, contacts: List<DiaryAccount>, state: WorkspaceUiState, onRead: (String) -> Unit, onSend: (String, String, String, () -> Unit) -> Unit, onRetry: () -> Unit, onClearError: () -> Unit, modifier: Modifier = Modifier) {
    var sent by rememberSaveable { mutableStateOf(false) }
    var composing by rememberSaveable { mutableStateOf(false) }
    var handledReceipt by rememberSaveable { mutableStateOf(state.sentReceipt) }
    LaunchedEffect(state.sentReceipt) {
        if (state.sentReceipt != null && state.sentReceipt != handledReceipt) { composing = false; sent = true; handledReceipt = state.sentReceipt }
    }
    var selected by rememberSaveable { mutableStateOf<String?>(null) }
    var replyTo by rememberSaveable { mutableStateOf<String?>(null) }
    var replySubject by rememberSaveable { mutableStateOf("") }
    val recipients = contacts.filter { it.id != account.id && (account.role == AccountRole.TEACHER || it.role == AccountRole.TEACHER) }
    val messages = state.messages.filter { if (sent) it.senderId == account.id else it.recipientId == account.id }
    Column(modifier.fillMaxSize().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Wiadomości", Modifier.weight(1f), style = MaterialTheme.typography.headlineMedium)
            FilledTonalIconButton(onClick = { replyTo = null; replySubject = ""; onClearError(); composing = true }) { Icon(painterResource(R.drawable.ic_compose), "Napisz wiadomość") }
        }
        Text("DEMO · lokalna skrzynka na tym urządzeniu. Nic nie jest wysyłane do eduVULCAN.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        PrimaryTabRow(selectedTabIndex = if (sent) 1 else 0) {
            Tab(selected = !sent, onClick = { sent = false }, text = { Text("Odebrane (${state.messages.count { it.recipientId == account.id && !it.read }})") })
            Tab(selected = sent, onClick = { sent = true }, text = { Text("Wysłane") })
        }
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error); TextButton(onClick = onRetry) { Text("Spróbuj ponownie") } }
        if (state.loading) LinearProgressIndicator(Modifier.fillMaxWidth())
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 20.dp)) {
            if (!state.loading && messages.isEmpty()) item { Text(if (sent) "Nie wysłano jeszcze wiadomości." else "Brak odebranych wiadomości.", Modifier.padding(vertical = 24.dp)) }
            items(messages, key = { it.id }) { message ->
                val other = contacts.firstOrNull { it.id == if (sent) message.recipientId else message.senderId }
                OutlinedCard(onClick = { selected = message.id; if (!sent && !message.read) onRead(message.id) }, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(message.subject, style = MaterialTheme.typography.titleMedium, fontWeight = if (!sent && !message.read) FontWeight.Bold else FontWeight.Normal)
                        Text("${if (sent) "Do" else "Od"}: ${other?.displayName ?: "Profil demo"}", style = MaterialTheme.typography.bodyMedium)
                        Text(message.body, maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall)
                        Text(message.sentAt.atZone(SchoolZone).format(DateTimeFormatter.ofPattern("dd.MM.yyyy · HH:mm")), style = MaterialTheme.typography.labelSmall)
                        if (!sent && !message.read) Text("Nieprzeczytana", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
    state.messages.firstOrNull { it.id == selected }?.let { message ->
        MessageDetails(message, contacts, account.id, onDismiss = { selected = null }, onReply = {
            replyTo = message.senderId
            replySubject = if (message.subject.startsWith("Re: ")) message.subject else "Re: ${message.subject}".take(120)
            selected = null; onClearError(); composing = true
        })
    }
    if (composing) MessageComposer(recipients, replyTo, replySubject, state.busy, state.error, onDismiss = { composing = false }, onSend = { recipient, subject, body -> onSend(recipient, subject, body) { composing = false; sent = true } })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MessageDetails(message: DiaryMessage, contacts: List<DiaryAccount>, accountId: String, onDismiss: () -> Unit, onReply: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        LazyColumn(contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item { Row(verticalAlignment = Alignment.CenterVertically) { Text(message.subject, Modifier.weight(1f), style = MaterialTheme.typography.headlineSmall); IconButton(onClick = onDismiss) { Icon(painterResource(R.drawable.ic_close), "Zamknij wiadomość") } } }
            item { Text("Od: ${contacts.firstOrNull { it.id == message.senderId }?.displayName.orEmpty()}"); Text("Do: ${contacts.firstOrNull { it.id == message.recipientId }?.displayName.orEmpty()}"); Text(message.sentAt.atZone(SchoolZone).format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))) }
            item { HorizontalDivider(); Text(message.body, Modifier.padding(top = 16.dp), style = MaterialTheme.typography.bodyLarge) }
            if (message.recipientId == accountId) item { Button(onClick = onReply) { Text("Odpowiedz") } }
            item { Text("Wiadomość lokalna · demo", style = MaterialTheme.typography.labelMedium) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MessageComposer(recipients: List<DiaryAccount>, initialRecipient: String?, initialSubject: String, busy: Boolean, error: String?, onDismiss: () -> Unit, onSend: (String, String, String) -> Unit) {
    var recipientId by rememberSaveable { mutableStateOf(initialRecipient ?: recipients.firstOrNull()?.id) }
    var subject by rememberSaveable { mutableStateOf(initialSubject) }
    var body by rememberSaveable { mutableStateOf("") }
    var picking by remember { mutableStateOf(false) }
    var discard by rememberSaveable { mutableStateOf(false) }
    val dirty = subject != initialSubject || body.isNotBlank()
    val requestDismiss = { if (!busy) { if (dirty) discard = true else onDismiss() }; Unit }
    BackHandler { requestDismiss() }
    ModalBottomSheet(onDismissRequest = requestDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), sheetGesturesEnabled = !busy) {
        LazyColumn(Modifier.imePadding(), contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item { Row(verticalAlignment = Alignment.CenterVertically) { Text("Nowa wiadomość", Modifier.weight(1f), style = MaterialTheme.typography.headlineSmall); IconButton(onClick = requestDismiss, enabled = !busy) { Icon(painterResource(R.drawable.ic_close), "Zamknij formularz wiadomości") } } }
            item { Text("Tylko demo: dostarczenie do innego profilu na tym urządzeniu.", style = MaterialTheme.typography.bodySmall) }
            item {
                Box { OutlinedButton(onClick = { picking = true }, enabled = !busy) { Text("Do: ${recipients.firstOrNull { it.id == recipientId }?.displayName ?: "Wybierz odbiorcę"}") }
                    DropdownMenu(expanded = picking, onDismissRequest = { picking = false }) { recipients.forEach { recipient -> DropdownMenuItem(text = { Text(recipient.displayName) }, onClick = { recipientId = recipient.id; picking = false }) } }
                }
            }
            item { OutlinedTextField(subject, { subject = it.take(120) }, Modifier.fillMaxWidth(), label = { Text("Temat") }, enabled = !busy, singleLine = true, supportingText = { Text("${subject.length}/120") }) }
            item { OutlinedTextField(body, { body = it.take(5000) }, Modifier.fillMaxWidth(), label = { Text("Treść wiadomości") }, enabled = !busy, minLines = 5, supportingText = { Text("${body.length}/5000") }) }
            error?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
            item { Button(onClick = { recipientId?.let { onSend(it, subject, body) } }, enabled = !busy && recipients.any { it.id == recipientId } && subject.isNotBlank() && body.isNotBlank(), modifier = Modifier.fillMaxWidth()) { Text(if (busy) "Zapisywanie…" else "Wyślij lokalnie (demo)") } }
        }
    }
    if (discard) AlertDialog(onDismissRequest = { discard = false }, title = { Text("Odrzucić wiadomość?") }, text = { Text("Niewysłana treść zostanie usunięta.") }, confirmButton = { TextButton(onClick = onDismiss) { Text("Odrzuć") } }, dismissButton = { TextButton(onClick = { discard = false }) { Text("Wróć do pisania") } })
}
