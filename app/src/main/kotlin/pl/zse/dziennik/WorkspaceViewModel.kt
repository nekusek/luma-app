package pl.zse.dziennik

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import pl.zse.dziennik.model.*
import pl.zse.dziennik.data.DiaryPreferences

data class WorkspaceUiState(val messages: List<DiaryMessage> = emptyList(), val entries: List<TeachingEntry> = emptyList(), val loading: Boolean = true, val busy: Boolean = false, val error: String? = null, val notice: String? = null, val sentReceipt: String? = null)

class WorkspaceViewModel(private val repository: WorkspaceRepository, val account: DiaryAccount, clock: Clock, private val preferences: DiaryPreferences? = null) : ViewModel() {
    val contacts get() = repository.contacts
    val today: LocalDate = LocalDate.now(clock.withZone(SchoolZone))
    private val busy = MutableStateFlow(false)
    private val error = MutableStateFlow<String?>(null)
    private val notice = MutableStateFlow<String?>(null)
    private val sentReceipt = MutableStateFlow<String?>(null)
    private val retry = MutableStateFlow(0)
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private val content = retry.flatMapLatest {
        combine(repository.messages(account.id), if (account.role == AccountRole.TEACHER) repository.teachingEntries(account.id) else flowOf(emptyList())) { messages, entries ->
            WorkspaceUiState(messages, entries, loading = false)
        }.catch { cause ->
            if (cause is CancellationException) throw cause
            error.value = "Nie udało się wczytać danych demo. Spróbuj ponownie."
            emit(WorkspaceUiState(loading = false))
        }
    }
    val state = combine(content, busy, error, notice, sentReceipt) { content, busy, error, notice, sentReceipt -> content.copy(busy = busy, error = error, notice = notice, sentReceipt = sentReceipt) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, WorkspaceUiState())
    fun retry() { error.value = null; retry.value++ }
    fun dismissNotice() { notice.value = null }
    fun clearError() { error.value = null }
    fun read(id: String) = operation(null) { if (preferences?.options?.first()?.readReceipts != false) repository.markRead(account.id, id) }
    fun send(recipientId: String, subject: String, body: String, onSuccess: () -> Unit) = operation("Wiadomość dostarczona lokalnie w demo.") {
        repository.send(account.id, recipientId, subject, body)
        sentReceipt.value = java.util.UUID.randomUUID().toString()
        onSuccess()
    }
    fun save(studentId: String?, className: String, kind: TeachingKind, value: String, description: String = "", weight: Int = 1, date: LocalDate = today, onSuccess: () -> Unit = {}) = operation("Zapisano w lokalnym dzienniku demo.") {
        repository.saveEntry(account.id, studentId, className, kind, value, description, weight, date)
        onSuccess()
    }
    private fun operation(message: String?, block: suspend () -> Unit) {
        if (!busy.compareAndSet(false, true)) return
        viewModelScope.launch {
            error.value = null
            try { block(); notice.value = message }
            catch (cause: CancellationException) { throw cause }
            catch (_: Exception) { error.value = "Nie udało się zapisać zmian. Sprawdź dane i spróbuj ponownie." }
            finally { busy.value = false }
        }
    }
}

class WorkspaceViewModelFactory @Inject constructor(private val repository: WorkspaceRepository, private val clock: Clock, private val preferences: DiaryPreferences) {
    fun create(account: DiaryAccount) = WorkspaceViewModel(repository, account, clock, preferences)
}
