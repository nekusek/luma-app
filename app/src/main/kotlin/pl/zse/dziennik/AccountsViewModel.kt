package pl.zse.dziennik

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import pl.zse.dziennik.data.DemoProfiles
import pl.zse.dziennik.model.*

data class CompletedLogin(val receiptId: String, val accountId: String)
data class AccountsUiState(val session: AccountSession? = null, val busy: Boolean = false, val error: String? = null, val completedLogin: CompletedLogin? = null)

@HiltViewModel
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class AccountsViewModel @Inject constructor(private val repository: AccountRepository) : ViewModel() {
    val demoProfiles = DemoProfiles.accounts
    private val retry = MutableStateFlow(0)
    private val busy = MutableStateFlow(false)
    private val error = MutableStateFlow<String?>(null)
    private val completedLogin = MutableStateFlow<CompletedLogin?>(null)
    private val session = retry.flatMapLatest {
        repository.session.catch { cause ->
            if (cause is CancellationException) throw cause
            error.value = "Nie udało się wczytać kont. Spróbuj ponownie."
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val state = combine(session, busy, error, completedLogin) { accounts, loading, message, login ->
        AccountsUiState(accounts, loading, message, login)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, AccountsUiState())

    fun retryLoad() { error.value = null; retry.value++ }
    fun dismissError() { error.value = null }
    fun signIn(request: AuthenticationRequest) = operation("Nie udało się zalogować. Spróbuj ponownie.") {
        val result = repository.signIn(request)
        completedLogin.value = CompletedLogin(UUID.randomUUID().toString(), requireNotNull(result.activeAccount).id)
    }
    fun activate(id: String) = operation("Nie udało się przełączyć konta.") { repository.activate(id) }
    fun remove(id: String) = operation("Nie udało się całkowicie usunąć konta. Spróbuj ponownie.") { repository.remove(id) }

    private fun operation(message: String, block: suspend () -> Unit) {
        if (!busy.compareAndSet(false, true)) return
        viewModelScope.launch {
            error.value = null
            try { block() }
            catch (cause: CancellationException) { throw cause }
            catch (_: Exception) { error.value = message }
            finally { busy.value = false }
        }
    }
}
